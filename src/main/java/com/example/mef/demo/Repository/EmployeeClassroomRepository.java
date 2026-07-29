package com.schooladmin.repository;



import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeClassroomRepository extends JpaRepository<EmployeeClassroom, String> {

    List<EmployeeClassroom> findByEmployeeId(String employeeId);

    List<EmployeeClassroom> findByClassId(String classId);

}