package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Room;
import com.example.mef.demo.Repository.ClassroomRepository;
import com.example.mef.demo.Repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** CRUD + usage lookups for physical rooms (salles). */
@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final ClassroomRepository classroomRepository;

    public RoomService(RoomRepository roomRepository, ClassroomRepository classroomRepository) {
        this.roomRepository = roomRepository;
        this.classroomRepository = classroomRepository;
    }

    @Transactional(readOnly = true)
    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    public Room save(Room room) {
        return roomRepository.save(room);
    }

    /** Sections currently linked to a given room, used to warn before deleting it. */
    @Transactional(readOnly = true)
    public List<Classroom> findSectionsUsingRoom(String roomId) {
        return classroomRepository.findAll().stream()
                .filter(c -> c.getRooms() != null && c.getRooms().stream().anyMatch(r -> r.getId().equals(roomId)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(String roomId) {
        // Detach the room from every section that references it before deleting,
        // so the join table doesn't keep a dangling reference.
        List<Classroom> linked = findSectionsUsingRoom(roomId);
        for (Classroom classroom : linked) {
            classroom.getRooms().removeIf(r -> r.getId().equals(roomId));
            classroomRepository.save(classroom);
        }
        roomRepository.deleteById(roomId);
    }
}