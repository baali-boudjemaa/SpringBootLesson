package com.example.mef.demo.Services;


import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Attendance;
import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Room;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Repository.AttendanceRepository;
import com.example.mef.demo.Repository.ClassroomRepository;
import com.example.mef.demo.Repository.InscriptionRepository;
import com.example.mef.demo.Repository.StudentRepository;
import com.example.mef.demo.enums.AttendanceStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final InscriptionRepository inscriptionRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    public ClassroomService(ClassroomRepository classroomRepository,
                            InscriptionRepository inscriptionRepository,
                            AttendanceRepository attendanceRepository,
                            StudentRepository studentRepository) {
        this.classroomRepository = classroomRepository;
        this.inscriptionRepository = inscriptionRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<Classroom> findAll() {
        return classroomRepository.findAll();
    }

    public Classroom save(Classroom classroom) {
        return classroomRepository.save(classroom);
    }

    public void delete(String classroomId) {
        classroomRepository.deleteById(classroomId);
    }

    /** Students currently enrolled in this classroom, via their active Inscription records. */
    @Transactional(readOnly = true)
    public List<Student> getStudentsInClassroom(String classroomId) {
        List<Inscription> inscriptions = inscriptionRepository.findByClassroomId(classroomId);
        return inscriptions.stream()
                .map(Inscription::getStudent)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClassAttendanceReport getClassAttendanceReport(String classroomId, LocalDate date) {
        List<Student> students = getStudentsInClassroom(classroomId);
        return buildAttendanceReport(date, students);
    }

    @Transactional(readOnly = true)
    public ClassAttendanceReport getAllStudentsAttendanceReport(LocalDate date) {
        return buildAttendanceReport(date, studentRepository.findAll());
    }

    private ClassAttendanceReport buildAttendanceReport(LocalDate date, List<Student> students) {
        List<String> studentIds = students.stream()
                .map(Student::getId)
                .toList();

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
        Map<String, Attendance> attendanceByStudentId = studentIds.isEmpty()
                ? Map.of()
                : attendanceRepository.findByStudentIdInAndDateBetween(studentIds, start, end).stream()
                .collect(Collectors.toMap(
                        attendance -> attendance.getStudent().getId(),
                        Function.identity(),
                        (first, second) -> second));

        List<ClassStudentAttendance> rows = students.stream()
                .map(student -> {
                    Attendance attendance = attendanceByStudentId.get(student.getId());
                    AttendanceStatus status = attendance == null ? null : attendance.getStatus();
                    return new ClassStudentAttendance(
                            student.getId(),
                            student.getStudentNumber(),
                            student.getFirstName(),
                            student.getLastName(),
                            status);
                })
                .toList();

        long present = rows.stream()
                .filter(row -> row.status() == AttendanceStatus.PRESENT)
                .count();
        long absent = rows.stream()
                .filter(row -> row.status() == AttendanceStatus.ABSENT)
                .count();
        long excused = rows.stream()
                .filter(row -> row.status() == AttendanceStatus.EXCUSED)
                .count();
        long unmarked = rows.stream()
                .filter(row -> row.status() == null)
                .count();

        return new ClassAttendanceReport(date, rows, present, absent, excused, unmarked);
    }

    @Transactional
    public void saveAttendance(LocalDate date, Map<String, AttendanceStatus> statuses) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);

        statuses.forEach((studentId, status) -> {
            if (status == null) {
                return;
            }

            Attendance attendance = attendanceRepository
                    .findFirstByStudentIdAndDateBetween(studentId, start, end)
                    .orElseGet(() -> Attendance.builder()
                            .student(studentRepository.getReferenceById(studentId))
                            .date(start)
                            .build());

            attendance.setStatus(status);
            if (status == AttendanceStatus.PRESENT && attendance.getCheckInTime() == null) {
                attendance.setCheckInTime(LocalDateTime.now());
            }
            attendanceRepository.save(attendance);
        });
    }

    /** Enrollment count for a classroom, for the card's "X/capacity" display. */
    @Transactional(readOnly = true)
    public int countStudentsInClassroom(String classroomId) {
        return inscriptionRepository.findByClassroomId(classroomId).size();
    }

    /**
     * Remaining seats in a classroom (capacity minus current enrollments),
     * never negative. Used to block enrollment when a class is full and to
     * show "X places restantes" while picking a classroom.
     */
    @Transactional(readOnly = true)
    public int remainingSeats(Classroom classroom) {
        if (classroom == null || classroom.getCapacity() == null) {
            return Integer.MAX_VALUE;
        }
        int taken = countStudentsInClassroom(classroom.getId());
        return Math.max(0, classroom.getCapacity() - taken);
    }

    /**
     * Checks whether {@code candidate}'s weekly occupancy schedule overlaps,
     * on any room it is linked to, with another section's schedule on that
     * same room. Used before saving a section so two sections can never be
     * booked into the same room at overlapping times.
     *
     * @param candidate the section being saved (its rooms + occupancySchedule
     *                  reflect the pending form values, not yet persisted)
     * @return the list of conflicts found; empty if the schedule is free
     */
    @Transactional(readOnly = true)
    public List<RoomConflict> findRoomConflicts(Classroom candidate) {
        if (candidate.getRooms() == null || candidate.getRooms().isEmpty()) {
            return List.of();
        }
        List<ScheduleSlot> candidateSlots = parseSchedule(candidate.getOccupancySchedule());
        if (candidateSlots.isEmpty()) {
            return List.of();
        }

        Set<String> roomIds = candidate.getRooms().stream()
                .map(Room::getId)
                .collect(Collectors.toSet());

        List<RoomConflict> conflicts = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Classroom other : classroomRepository.findAll()) {
            if (other.getId() != null && other.getId().equals(candidate.getId())) {
                continue;
            }
            if (other.getRooms() == null || other.getRooms().isEmpty()) {
                continue;
            }

            List<ScheduleSlot> otherSlots = parseSchedule(other.getOccupancySchedule());
            if (otherSlots.isEmpty()) {
                continue;
            }

            for (Room room : other.getRooms()) {
                if (!roomIds.contains(room.getId())) {
                    continue;
                }
                for (ScheduleSlot mine : candidateSlots) {
                    for (ScheduleSlot theirs : otherSlots) {
                        if (!mine.day().equals(theirs.day())) continue;
                        if (mine.start() < theirs.end() && theirs.start() < mine.end()) {
                            String key = room.getId() + "|" + other.getId() + "|" + mine.day()
                                    + "|" + Math.max(mine.start(), theirs.start());
                            if (seen.add(key)) {
                                conflicts.add(new RoomConflict(
                                        room.getName(),
                                        other.getName(),
                                        mine.day(),
                                        overlapLabel(mine, theirs)));
                            }
                        }
                    }
                }
            }
        }

        return conflicts;
    }

    /** Parses "Lundi 07:00-09:00; Mardi 09:00-11:00" into day/start/end slots (minutes since midnight). */
    private List<ScheduleSlot> parseSchedule(String schedule) {
        List<ScheduleSlot> slots = new ArrayList<>();
        if (schedule == null || schedule.isBlank()) {
            return slots;
        }
        for (String raw : schedule.split(";")) {
            String entry = raw.trim();
            if (entry.isEmpty()) continue;
            int spaceIndex = entry.indexOf(' ');
            if (spaceIndex < 0) continue;
            String day = entry.substring(0, spaceIndex);
            String[] range = entry.substring(spaceIndex + 1).split("-");
            if (range.length != 2) continue;
            Integer start = toMinutes(range[0]);
            Integer end = toMinutes(range[1]);
            if (start == null || end == null || end <= start) continue;
            slots.add(new ScheduleSlot(day, start, end));
        }
        return slots;
    }

    private Integer toMinutes(String hhmm) {
        try {
            String[] parts = hhmm.trim().split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return null;
        }
    }

    private String overlapLabel(ScheduleSlot a, ScheduleSlot b) {
        int start = Math.max(a.start(), b.start());
        int end = Math.min(a.end(), b.end());
        return format(start) + "-" + format(end);
    }

    private String format(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private record ScheduleSlot(String day, int start, int end) {
    }

    /** One room-booking conflict between the section being saved and an existing one. */
    public record RoomConflict(String roomName, String otherClassroomName, String day, String timeRange) {
    }

    public record ClassAttendanceReport(
            LocalDate date,
            List<ClassStudentAttendance> students,
            long present,
            long absent,
            long excused,
            long unmarked) {
    }

    public record ClassStudentAttendance(
            String id,
            String studentNumber,
            String firstName,
            String lastName,
            AttendanceStatus status) {

        public String fullName() {
            return firstName + " " + lastName;
        }

        public String statusLabel() {
            if (status == null) {
                return "NON MARQUE";
            }
            return status.name();
        }
    }
}