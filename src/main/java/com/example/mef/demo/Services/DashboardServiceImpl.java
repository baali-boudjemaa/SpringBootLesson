package com.example.mef.demo.Services;

import com.example.mef.demo.Repository.AttendanceRepository;
import com.example.mef.demo.Repository.ClassroomRepository;
import com.example.mef.demo.Repository.PaymentRepository;
import com.example.mef.demo.Repository.StudentRepository;
import com.example.mef.demo.dashboard.DashboardStats;
import com.example.mef.demo.Model.Attendance;
import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.enums.AttendanceStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DashboardServiceImpl implements DashboardService {

    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final PaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;

    public DashboardServiceImpl(
            StudentRepository studentRepository,
            ClassroomRepository classroomRepository,
            PaymentRepository paymentRepository,
            AttendanceRepository attendanceRepository) {

        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.paymentRepository = paymentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Override
    public DashboardStats getDashboardStats() {

        DashboardStats stats = new DashboardStats();

        stats.setTotalStudents(studentRepository.count());
        stats.setTotalClassrooms(classroomRepository.count());
        stats.setTotalPayments(paymentRepository.count());
        stats.setAttendanceRate(calculateAttendanceRate());

        return stats;
    }

    @Override
    public List<Student> getRecentStudents() {
        return studentRepository.findAll().stream()
                .limit(5)
                .toList();
    }

    @Override
    public List<Payment> getRecentPayments() {
        return paymentRepository.findAll().stream()
                .limit(5)
                .toList();
    }

    private double calculateAttendanceRate() {
        List<Attendance> attendance = attendanceRepository.findAll();
        if (attendance.isEmpty()) {
            return 0;
        }

        long present = attendance.stream()
                .filter(record -> record.getStatus() == AttendanceStatus.PRESENT)
                .count();

        return (present * 100.0) / attendance.size();
    }
}
