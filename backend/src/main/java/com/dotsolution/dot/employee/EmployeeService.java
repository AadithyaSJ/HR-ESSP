package com.dotsolution.dot.employee;

import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.employee.entity.Employee;
import com.dotsolution.dot.employee.repository.EmployeeRepository;
import com.dotsolution.dot.auth.entity.User;
import com.dotsolution.dot.auth.entity.Role;
import com.dotsolution.dot.auth.repository.UserRepository;
import com.dotsolution.dot.auth.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        
        employee.setOnboardingStatus("PENDING_DETAILS");
        employee.setOnboardingPercent(0);
        
        Employee saved = employeeRepository.save(employee);
        
        // Automatically create user account
        Role roleEntity = roleRepository.findByRoleName(employee.getRole() != null ? employee.getRole() : "EMPLOYEE")
                .orElseGet(() -> roleRepository.findByRoleName("EMPLOYEE").orElseThrow());
                
        User user = User.builder()
                .employeeId(saved.getEmployeeCode())
                .email(saved.getEmail())
                .password(passwordEncoder.encode("PENDING_INITIAL_PASSWORD_SET"))
                .role(roleEntity)
                .status("ACTIVE")
                .build();
        userRepository.save(user);
        
        return saved;
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
        
        employee.setSchool(details.getSchool());
        employee.setCollege(details.getCollege());
        employee.setExperience(details.getExperience());
        employee.setCertificates(details.getCertificates());
        employee.setOnboardingStatus(details.getOnboardingStatus());
 
        return employeeRepository.save(employee);
    }

    public Employee approveOnboarding(UUID id) {
        Employee employee = getEmployeeById(id);
        employee.setOnboardingStatus("APPROVED");
        employee.setOnboardingPercent(100);
        return employeeRepository.save(employee);
    }

    public void deactivateEmployee(UUID id) {
        Employee employee = getEmployeeById(id);
        employee.setStatus("INACTIVE");
        employeeRepository.save(employee);
    }

    public boolean deleteEmployeeByEmail(String email) {
        java.util.Optional<Employee> empOpt = employeeRepository.findByEmail(email);
        if (empOpt.isPresent()) {
            Employee employee = empOpt.get();
            // Delete associated user if exists
            userRepository.findByEmail(email).ifPresent(user -> userRepository.delete(user));
            // Delete employee
            employeeRepository.delete(employee);
            return true;
        }
        return false;
    }
}

