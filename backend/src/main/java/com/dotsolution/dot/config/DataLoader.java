package com.dotsolution.dot.config;

import com.dotsolution.dot.auth.entity.Role;
import com.dotsolution.dot.auth.entity.User;
import com.dotsolution.dot.auth.repository.RoleRepository;
import com.dotsolution.dot.auth.repository.UserRepository;
import com.dotsolution.dot.employee.entity.Employee;
import com.dotsolution.dot.employee.repository.EmployeeRepository;
import com.dotsolution.dot.leave.entity.LeaveBalance;
import com.dotsolution.dot.leave.repository.LeaveBalanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Roles
        seedRoles();

        // 2. Seed Employees
        seedEmployeesAndUsers();
    }

    private void seedRoles() {
        List<String> roleNames = Arrays.asList("EMPLOYEE", "MANAGER", "HR_ADMIN", "FINANCE_ADMIN", "SYSTEM_ADMIN");
        for (String roleName : roleNames) {
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().roleName(roleName).build());
            }
        }
    }

    private void seedEmployeesAndUsers() {
        if (employeeRepository.count() > 0) {
            return; // Already seeded
        }

        Role employeeRole = roleRepository.findByRoleName("EMPLOYEE").orElseThrow();
        Role managerRole = roleRepository.findByRoleName("MANAGER").orElseThrow();
        Role hrRole = roleRepository.findByRoleName("HR_ADMIN").orElseThrow();
        Role financeRole = roleRepository.findByRoleName("FINANCE_ADMIN").orElseThrow();
        Role systemRole = roleRepository.findByRoleName("SYSTEM_ADMIN").orElseThrow();

        String defaultPassword = passwordEncoder.encode("password123");

        // Seed Manager Sarah Jenkins first
        Employee sarah = Employee.builder()
                .employeeCode("EMP002")
                .name("Sarah Jenkins")
                .email("sarah.j@company.com")
                .phone("9876543210")
                .address("123 Silicon Boulevard, Tech City")
                .bankAccountNo("1122334455")
                .bankIfsc("UTIB0001234")
                .bankName("Axis Bank")
                .emergencyName("David Jenkins")
                .emergencyRelation("Spouse")
                .emergencyPhone("9876543211")
                .onboardingPercent(100)
                .joiningDate(LocalDate.of(2020, 1, 1))
                .status("ACTIVE")
                .salary(120000.0)
                .salaryBand("L2")
                .department("Engineering")
                .designation("Engineering Manager")
                .build();
        sarah = employeeRepository.save(sarah);

        User sarahUser = User.builder()
                .employeeId(sarah.getEmployeeCode())
                .email(sarah.getEmail())
                .password(defaultPassword)
                .role(managerRole)
                .status("ACTIVE")
                .build();
        userRepository.save(sarahUser);

        // Seed HR Admin Jane Doe under Sarah
        Employee jane = Employee.builder()
                .employeeCode("EMP001")
                .name("Jane Doe")
                .email("jane.doe@company.com")
                .phone("9876543220")
                .address("456 Orchid Residency, Garden City")
                .bankAccountNo("2233445566")
                .bankIfsc("ICIC0005678")
                .bankName("ICICI Bank")
                .emergencyName("Robert Doe")
                .emergencyRelation("Father")
                .emergencyPhone("9876543221")
                .onboardingPercent(100)
                .managerId(sarah.getId())
                .joiningDate(LocalDate.of(2022, 3, 15))
                .status("ACTIVE")
                .salary(85000.0)
                .salaryBand("L1")
                .department("HR")
                .designation("HR Specialist")
                .build();
        jane = employeeRepository.save(jane);

        User janeUser = User.builder()
                .employeeId(jane.getEmployeeCode())
                .email(jane.getEmail())
                .password(defaultPassword)
                .role(hrRole)
                .status("ACTIVE")
                .build();
        userRepository.save(janeUser);

        // Seed Finance Admin David Vance under Sarah
        Employee david = Employee.builder()
                .employeeCode("EMP003")
                .name("David Vance")
                .email("david.v@company.com")
                .phone("9876543230")
                .address("789 Pine Meadows, River Town")
                .bankAccountNo("3344556677")
                .bankIfsc("HDFC0009876")
                .bankName("HDFC Bank")
                .emergencyName("Alice Vance")
                .emergencyRelation("Mother")
                .emergencyPhone("9876543231")
                .onboardingPercent(100)
                .managerId(sarah.getId())
                .joiningDate(LocalDate.of(2021, 6, 10))
                .status("ACTIVE")
                .salary(90000.0)
                .salaryBand("L1")
                .department("Finance")
                .designation("Finance Analyst")
                .build();
        david = employeeRepository.save(david);

        User davidUser = User.builder()
                .employeeId(david.getEmployeeCode())
                .email(david.getEmail())
                .password(defaultPassword)
                .role(financeRole)
                .status("ACTIVE")
                .build();
        userRepository.save(davidUser);

        // Seed Regular Employee John Doe under Sarah
        Employee john = Employee.builder()
                .employeeCode("EMP004")
                .name("John Doe")
                .email("john.doe@company.com")
                .phone("9876543240")
                .address("321 Rosewood Apartment, Tech City")
                .bankAccountNo("4455667788")
                .bankIfsc("SBIN0004321")
                .bankName("State Bank of India")
                .emergencyName("Mary Doe")
                .emergencyRelation("Mother")
                .emergencyPhone("9876543241")
                .onboardingPercent(80)
                .managerId(sarah.getId())
                .joiningDate(LocalDate.of(2023, 1, 15))
                .status("ACTIVE")
                .salary(70000.0)
                .salaryBand("L1")
                .department("Engineering")
                .designation("Software Engineer")
                .build();
        john = employeeRepository.save(john);

        User johnUser = User.builder()
                .employeeId(john.getEmployeeCode())
                .email(john.getEmail())
                .password(defaultPassword)
                .role(employeeRole)
                .status("ACTIVE")
                .build();
        userRepository.save(johnUser);

        // Seed System Admin admin@company.com
        Employee adminEmp = Employee.builder()
                .employeeCode("EMP005")
                .name("System Admin")
                .email("admin@company.com")
                .phone("9876543250")
                .address("Server Room 1, HQ")
                .joiningDate(LocalDate.of(2018, 1, 1))
                .status("ACTIVE")
                .salary(150000.0)
                .salaryBand("L3")
                .department("IT")
                .designation("System Administrator")
                .build();
        adminEmp = employeeRepository.save(adminEmp);

        User adminUser = User.builder()
                .employeeId(adminEmp.getEmployeeCode())
                .email(adminEmp.getEmail())
                .password(defaultPassword)
                .role(systemRole)
                .status("ACTIVE")
                .build();
        userRepository.save(adminUser);

        // Seed Leave Balances for 2026
        seedBalancesForEmployee(sarah.getId());
        seedBalancesForEmployee(jane.getId());
        seedBalancesForEmployee(david.getId());
        seedBalancesForEmployee(john.getId());
    }

    private void seedBalancesForEmployee(java.util.UUID employeeId) {
        leaveBalanceRepository.save(LeaveBalance.builder()
                .employeeId(employeeId)
                .leaveType("ANNUAL")
                .balance(15)
                .year(2026)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .employeeId(employeeId)
                .leaveType("SICK")
                .balance(10)
                .year(2026)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .employeeId(employeeId)
                .leaveType("CASUAL")
                .balance(7)
                .year(2026)
                .build());
    }
}
