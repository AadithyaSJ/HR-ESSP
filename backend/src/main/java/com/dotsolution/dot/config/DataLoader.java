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

    @Autowired
    private com.dotsolution.dot.document.repository.MandatoryDocumentRepository mandatoryDocumentRepository;

    @Autowired
    private com.dotsolution.dot.leave.repository.PublicHolidayRepository publicHolidayRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Roles
        seedRoles();

        // 2. Seed Employees
        seedEmployeesAndUsers();

        // 3. Seed Mandatory Documents
        seedMandatoryDocuments();

        // 4. Seed Public Holidays
        seedPublicHolidays();
    }

    private void seedPublicHolidays() {
        int year = LocalDate.now().getYear();
        seedHoliday("India", "New Year", LocalDate.of(year, 1, 1));
        seedHoliday("India", "Republic Day", LocalDate.of(year, 1, 26));
        seedHoliday("India", "Independence Day", LocalDate.of(year, 8, 15));
        seedHoliday("India", "Gandhi Jayanti", LocalDate.of(year, 10, 2));
        seedHoliday("India", "Diwali", LocalDate.of(year, 11, 9));

        seedHoliday("USA", "Independence Day", LocalDate.of(year, 7, 4));
        seedHoliday("USA", "Christmas Day", LocalDate.of(year, 12, 25));

        seedHoliday("UK", "Christmas Day", LocalDate.of(year, 12, 25));
        seedHoliday("UK", "Boxing Day", LocalDate.of(year, 12, 26));
    }

    private void seedHoliday(String country, String name, LocalDate date) {
        if (publicHolidayRepository.findByCountryAndHolidayDate(country, date).isEmpty()) {
            publicHolidayRepository.save(com.dotsolution.dot.leave.entity.PublicHoliday.builder()
                    .country(country)
                    .name(name)
                    .holidayDate(date)
                    .build());
        }
    }

    private void seedMandatoryDocuments() {
        List<String> docs = Arrays.asList("Aadhaar Card", "PAN Card", "10th Certificate", "12th Certificate", "Degree Certificate");
        for (String docName : docs) {
            if (mandatoryDocumentRepository.findByName(docName).isEmpty()) {
                mandatoryDocumentRepository.save(com.dotsolution.dot.document.entity.MandatoryDocument.builder()
                        .name(docName)
                        .build());
            }
        }
    }

    private void seedRoles() {
        List<String> roleNames = Arrays.asList("EMPLOYEE", "MANAGER", "HR_ADMIN", "FINANCE_ADMIN", "SYSTEM_ADMIN", "IT_SUPPORT");
        for (String roleName : roleNames) {
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().roleName(roleName).build());
            }
        }
    }

    private void seedEmployeesAndUsers() {
        Role employeeRole = roleRepository.findByRoleName("EMPLOYEE").orElseThrow();
        Role managerRole = roleRepository.findByRoleName("MANAGER").orElseThrow();
        Role hrRole = roleRepository.findByRoleName("HR_ADMIN").orElseThrow();
        Role financeRole = roleRepository.findByRoleName("FINANCE_ADMIN").orElseThrow();
        Role systemRole = roleRepository.findByRoleName("SYSTEM_ADMIN").orElseThrow();

        String defaultPassword = passwordEncoder.encode("password123");

        // 1. Sarah Jenkins (EMP002) - Manager
        Optional<Employee> existingSarah = employeeRepository.findByEmployeeCode("EMP002");
        Employee sarah;
        if (existingSarah.isPresent()) {
            sarah = existingSarah.get();
            sarah.setSalary(2800000.0);
            sarah.setSalaryBand("Band 4 (L5-L6)");
            sarah = employeeRepository.save(sarah);
        } else {
            sarah = Employee.builder()
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
                    .salary(2800000.0)
                    .salaryBand("Band 4 (L5-L6)")
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
            seedBalancesForEmployee(sarah.getId());
        }

        // 2. Jane Doe (EMP001) - HR Admin
        Optional<Employee> existingJane = employeeRepository.findByEmployeeCode("EMP001");
        Employee jane;
        if (existingJane.isPresent()) {
            jane = existingJane.get();
            jane.setSalary(1400000.0);
            jane.setSalaryBand("Band 3 (L3-L4)");
            jane = employeeRepository.save(jane);
        } else {
            jane = Employee.builder()
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
                    .salary(1400000.0)
                    .salaryBand("Band 3 (L3-L4)")
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
            seedBalancesForEmployee(jane.getId());
        }

        // 3. David Vance (EMP003) - Finance Admin
        Optional<Employee> existingDavid = employeeRepository.findByEmployeeCode("EMP003");
        Employee david;
        if (existingDavid.isPresent()) {
            david = existingDavid.get();
            david.setSalary(4500000.0);
            david.setSalaryBand("Band 5 (L7+)");
            david = employeeRepository.save(david);
        } else {
            david = Employee.builder()
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
                    .salary(4500000.0)
                    .salaryBand("Band 5 (L7+)")
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
            seedBalancesForEmployee(david.getId());
        }

        // 4. John Doe (EMP004) - Regular Employee
        Optional<Employee> existingJohn = employeeRepository.findByEmployeeCode("EMP004");
        Employee john;
        if (existingJohn.isPresent()) {
            john = existingJohn.get();
            john.setSalary(1000000.0);
            john.setSalaryBand("Band 2 (L2-L3)");
            john = employeeRepository.save(john);
        } else {
            john = Employee.builder()
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
                    .salary(1000000.0)
                    .salaryBand("Band 2 (L2-L3)")
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
            seedBalancesForEmployee(john.getId());
        }

        // 5. System Admin (EMP005) - Admin User
        Optional<Employee> existingAdmin = employeeRepository.findByEmployeeCode("EMP005");
        Employee adminEmp;
        if (existingAdmin.isPresent()) {
            adminEmp = existingAdmin.get();
            adminEmp.setSalary(2000000.0);
            adminEmp.setSalaryBand("Band 4 (L5-L6)");
            adminEmp = employeeRepository.save(adminEmp);
        } else {
            adminEmp = Employee.builder()
                    .employeeCode("EMP005")
                    .name("System Admin")
                    .email("admin@company.com")
                    .phone("9876543250")
                    .address("Server Room 1, HQ")
                    .joiningDate(LocalDate.of(2018, 1, 1))
                    .status("ACTIVE")
                    .salary(2000000.0)
                    .salaryBand("Band 4 (L5-L6)")
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
        }
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
