package com.example.mef.demo.Repository;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.enums.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    Optional<Employee> findByEmail(String email);

    List<Employee> findByRole(EmployeeRole role);

}
