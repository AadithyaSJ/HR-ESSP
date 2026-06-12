package com.dotsolution.dot.employee;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.employee.entity.Employee;
import com.dotsolution.dot.employee.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));
    }

    public Employee getEmployeeByCode(String code) {
        return employeeRepository.findByEmployeeCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with code: " + code));
    }

    public Employee createEmployee(Employee employee) {
        if (employeeRepository.findByEmployeeCode(employee.getEmployeeCode()).isPresent()) {
            throw new IllegalArgumentException("Employee code already exists: " + employee.getEmployeeCode());
        }
        if (employeeRepository.findByEmail(employee.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Employee email already exists: " + employee.getEmail());
        }
        return employeeRepository.save(employee);
    }

    public Employee updateEmployee(UUID id, Employee details) {
        Employee employee = getEmployeeById(id);
        
        employee.setName(details.getName());
        employee.setPhone(details.getPhone());
        employee.setAddress(details.getAddress());
        employee.setBankAccountNo(details.getBankAccountNo());
        employee.setBankIfsc(details.getBankIfsc());
        employee.setBankName(details.getBankName());
        employee.setEmergencyName(details.getEmergencyName());
        employee.setEmergencyRelation(details.getEmergencyRelation());
        employee.setEmergencyPhone(details.getEmergencyPhone());
        employee.setDepartment(details.getDepartment());
        employee.setDesignation(details.getDesignation());
        employee.setManagerId(details.getManagerId());
        employee.setOnboardingPercent(details.getOnboardingPercent());
        employee.setSalary(details.getSalary());
        employee.setSalaryBand(details.getSalaryBand());
        employee.setStatus(details.getStatus());

        return employeeRepository.save(employee);
    }

    public void deactivateEmployee(UUID id) {
        Employee employee = getEmployeeById(id);
        employee.setStatus("INACTIVE");
        employeeRepository.save(employee);
    }
}
