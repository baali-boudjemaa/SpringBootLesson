package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Repository.EmployeeRepository;
import com.example.mef.demo.enums.EmployeeRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Employee> findById(String id) {
        return employeeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Employee> findByRole(EmployeeRole role) {
        return employeeRepository.findByRole(role);
    }

    /** Teachers only, for course/classroom "assign teacher" pickers. */
    @Transactional(readOnly = true)
    public List<Employee> findTeachers() {
        return employeeRepository.findByRole(EmployeeRole.TEACHER);
    }

    public Employee save(Employee employee) {
        if (employee.getEmployeeNumber() == null || employee.getEmployeeNumber().isBlank()) {
            employee.setEmployeeNumber("EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (employee.getRole() == null) {
            employee.setRole(EmployeeRole.TEACHER);
        }
        return employeeRepository.save(employee);
    }

    public void delete(String id) {
        employeeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Employee> search(String needle) {
        if (needle == null || needle.isBlank()) {
            return findAll();
        }
        String lower = needle.toLowerCase();
        return findAll().stream()
                .filter(e -> containsIgnoreCase(e.getFirstName(), lower)
                        || containsIgnoreCase(e.getLastName(), lower)
                        || containsIgnoreCase(e.getEmail(), lower))
                .toList();
    }

    private static boolean containsIgnoreCase(String value, String lowerNeedle) {
        return value != null && value.toLowerCase().contains(lowerNeedle);
    }
}