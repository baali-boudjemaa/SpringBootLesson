package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.CourseScheduleSlot;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Model.TeacherAttendance;
import com.example.mef.demo.Repository.CourseRepository;
import com.example.mef.demo.Repository.EmployeeRepository;
import com.example.mef.demo.Repository.TeacherAttendanceRepository;
import com.example.mef.demo.enums.CompensationType;
import com.example.mef.demo.enums.TeacherAttendanceStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeacherPayrollService {
    private final TeacherAttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CourseRepository courseRepository;

    public TeacherPayrollService(TeacherAttendanceRepository attendanceRepository,
                                 EmployeeRepository employeeRepository,
                                 CourseRepository courseRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.courseRepository = courseRepository;
    }

    public TeacherAttendance saveAttendance(String teacherId, LocalDate date,
                                             TeacherAttendanceStatus status, double absentHours) {
        TeacherAttendance attendance = attendanceRepository.findByTeacherIdAndDate(teacherId, date)
                .orElseGet(() -> TeacherAttendance.builder()
                        .teacher(employeeRepository.getReferenceById(teacherId))
                        .date(date)
                        .build());
        attendance.setStatus(status == null ? TeacherAttendanceStatus.PRESENT : status);
        attendance.setAbsentHours(Math.max(0d, absentHours));
        return attendanceRepository.save(attendance);
    }

    @Transactional(readOnly = true)
    public TeacherAttendance attendanceFor(String teacherId, LocalDate date) {
        return attendanceRepository.findByTeacherIdAndDate(teacherId, date).orElse(null);
    }

    @Transactional(readOnly = true)
    public PayrollSummary calculate(String teacherId, YearMonth month) {
        Employee teacher = employeeRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<TeacherAttendance> attendance = attendanceRepository
                .findByTeacherIdAndDateBetween(teacherId, start, end);
        Map<LocalDate, TeacherAttendance> byDate = attendance.stream()
                .collect(Collectors.toMap(TeacherAttendance::getDate, item -> item, (first, ignored) -> first));

        long absentDays = attendance.stream()
                .filter(item -> item.getStatus() == TeacherAttendanceStatus.ABSENT).count();
        double absentHours = attendance.stream()
                .mapToDouble(item -> item.getAbsentHours() == null ? 0d : item.getAbsentHours()).sum();
        LessonCalculation lessonCalculation = plannedLessons(teacherId, start, end, byDate);

        CompensationType type = teacher.getCompensationType() == null
                ? CompensationType.MONTHLY : teacher.getCompensationType();
        double gross = type == CompensationType.PER_LESSON
                ? lessonCalculation.courseFeeTotal() * amount(teacher.getLessonRate()) / 100d
                : amount(teacher.getMonthlySalary());
        double deductions = absentDays * amount(teacher.getAbsenceDayDeduction())
                + absentHours * amount(teacher.getAbsenceHourDeduction());
        return new PayrollSummary(month, type, lessonCalculation.count(), absentDays, absentHours,
                gross, deductions, Math.max(0d, gross - deductions));
    }

    private LessonCalculation plannedLessons(String teacherId, LocalDate start, LocalDate end,
                                             Map<LocalDate, TeacherAttendance> attendance) {
        List<Course> courses = courseRepository.findByTeacherId(teacherId);
        int count = 0;
        double courseFeeTotal = 0d;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            TeacherAttendance record = attendance.get(date);
            if (record != null && record.getStatus() == TeacherAttendanceStatus.ABSENT) continue;
            for (Course course : courses) {
                for (CourseScheduleSlot slot : course.getScheduleSlots()) {
                    if (matches(slot.getDayOfWeek(), date.getDayOfWeek())) {
                        count++;
                        courseFeeTotal += amount(course.getMonthlyFee());
                    }
                }
            }
        }
        return new LessonCalculation(count, courseFeeTotal);
    }

    private boolean matches(String scheduleDay, DayOfWeek day) {
        if (scheduleDay == null) return false;
        String normalized = scheduleDay.trim().toLowerCase(Locale.ROOT);
        return switch (day) {
            case MONDAY -> normalized.equals("lundi") || normalized.equals("monday") || normalized.equals("الاثنين");
            case TUESDAY -> normalized.equals("mardi") || normalized.equals("tuesday") || normalized.equals("الثلاثاء");
            case WEDNESDAY -> normalized.equals("mercredi") || normalized.equals("wednesday") || normalized.equals("الأربعاء");
            case THURSDAY -> normalized.equals("jeudi") || normalized.equals("thursday") || normalized.equals("الخميس");
            case FRIDAY -> normalized.equals("vendredi") || normalized.equals("friday") || normalized.equals("الجمعة");
            case SATURDAY -> normalized.equals("samedi") || normalized.equals("saturday") || normalized.equals("السبت");
            case SUNDAY -> normalized.equals("dimanche") || normalized.equals("sunday") || normalized.equals("الأحد");
        };
    }

    private double amount(Double value) { return value == null ? 0d : Math.max(0d, value); }

    public record PayrollSummary(YearMonth month, CompensationType compensationType,
                                 int payableLessons, long absentDays, double absentHours,
                                 double gross, double deductions, double net) { }

    private record LessonCalculation(int count, double courseFeeTotal) { }
}
