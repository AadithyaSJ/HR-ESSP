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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * DataSeeder - Seeds all 5 user roles, employees, user accounts, and leave balances.
 * Runs idempotently on every startup (skips if data already exists).
 *
 * Credentials:
 *   jane.doe@company.com  / password123  -> HR_ADMIN    (EMP001)
 *   sarah.j@company.com   / password123  -> MANAGER     (EMP002)
 *   david.v@company.com   / password123  -> FINANCE_ADMIN (EMP003)
 *   john.doe@company.com  / password123  -> EMPLOYEE    (EMP004)
 *   admin@company.com     / password123  -> SYSTEM_ADMIN (EMP005)
 */
@Component
public class DataSeeder implements ApplicationRunner {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeaveBalanceRepository leaveBalanceRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private com.dotsolution.dot.document.repository.MandatoryDocumentRepository mandatoryDocumentRepository;
    @Autowired private com.dotsolution.dot.leave.repository.PublicHolidayRepository publicHolidayRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        deleteEmployeeIfExists("aadithyasuresh2725@gmail.com");
        deleteEmployeeIfExists("sec22cj018@sairatamp.edu.in");
        seedRoles();
        seedEmployeesAndUsers();
        seedMandatoryDocuments();
        seedPublicHolidays();
        System.out.println("[DataSeeder] ✅ Database seeding complete.");
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
        List<String> docs = List.of("Aadhaar Card", "PAN Card", "10th Certificate", "12th Certificate", "Degree Certificate");
        for (String docName : docs) {
            if (mandatoryDocumentRepository.findByName(docName).isEmpty()) {
                mandatoryDocumentRepository.save(com.dotsolution.dot.document.entity.MandatoryDocument.builder()
                        .name(docName)
                        .build());
            }
        }
    }

    private void deleteEmployeeIfExists(String email) {
        employeeRepository.findByEmail(email).ifPresent(employee -> {
            userRepository.findByEmail(email).ifPresent(user -> userRepository.delete(user));
            userRepository.findAll().stream()
                .filter(u -> employee.getEmployeeCode().equals(u.getEmployeeId()))
                .forEach(user -> userRepository.delete(user));
            employeeRepository.delete(employee);
            System.out.println("[DataSeeder] 🗑️ Deleted employee and user: " + email);
        });
    }


    private void seedRoles() {
        List<String> roleNames = List.of("HR_ADMIN", "MANAGER", "FINANCE_ADMIN", "EMPLOYEE", "SYSTEM_ADMIN");
        for (String roleName : roleNames) {
            roleRepository.findByRoleName(roleName).orElseGet(() -> {
                Role r = Role.builder().roleName(roleName).build();
                return roleRepository.save(r);
            });
        }
        System.out.println("[DataSeeder] Roles seeded.");
    }

    private void seedEmployeesAndUsers() {
        // First pass – seed employees that have no manager
        // EMP003 - David Vance (VP, FINANCE_ADMIN, no manager)
        Employee david = seedEmployee("EMP003", "David Vance", "david.v@company.com",
                "+91 98765 43212", "789 Gachibowli, Hyderabad, India",
                "Executive", "VP of Engineering", null,
                LocalDate.of(2020, 8, 1), 4500000.0, "Band 5 (L7+)", 100);

        // EMP005 - Admin User (SYSTEM_ADMIN, no manager)
        Employee admin = seedEmployee("EMP005", "System Admin", "admin@company.com",
                "+91 98765 99999", "Corporate HQ, Hyderabad, India",
                "IT", "System Administrator", null,
                LocalDate.of(2019, 1, 1), 2000000.0, "Band 4 (L5-L6)", 100);

        // Second pass – employees that report to david
        // EMP001 - Jane Doe (HR_ADMIN) -> manager = david
        Employee jane = seedEmployee("EMP001", "Jane Doe", "jane.doe@company.com",
                "+91 98765 43210", "123 Cyber Towers, Hitec City, Hyderabad, India",
                "HR", "HR Lead", david.getId(),
                LocalDate.of(2024, 3, 15), 1400000.0, "Band 3 (L3-L4)", 100);

        // EMP002 - Sarah Jenkins (MANAGER) -> manager = david
        Employee sarah = seedEmployee("EMP002", "Sarah Jenkins", "sarah.j@company.com",
                "+91 98765 43211", "456 Jubilee Hills, Hyderabad, India",
                "Engineering", "Engineering Manager", david.getId(),
                LocalDate.of(2023, 1, 10), 2800000.0, "Band 4 (L5-L6)", 100);

        // EMP004 - John Doe (EMPLOYEE) -> manager = sarah
        Employee john = seedEmployee("EMP004", "John Doe", "john.doe@company.com",
                "+91 98765 43213", "Secunderabad, India",
                "Engineering", "Software Engineer", sarah.getId(),
                LocalDate.of(2025, 5, 1), 1000000.0, "Band 2 (L2-L3)", 80);

        // Seed users
        String hashedPw = passwordEncoder.encode("password123");
        seedUser("jane.doe@company.com", "EMP001", "HR_ADMIN", hashedPw);
        seedUser("sarah.j@company.com", "EMP002", "MANAGER", hashedPw);
        seedUser("david.v@company.com", "EMP003", "FINANCE_ADMIN", hashedPw);
        seedUser("john.doe@company.com", "EMP004", "EMPLOYEE", hashedPw);
        seedUser("admin@company.com", "EMP005", "SYSTEM_ADMIN", hashedPw);

        // Seed leave balances
        seedLeaveBalances(jane.getId());
        seedLeaveBalances(sarah.getId());
        seedLeaveBalances(david.getId());
        seedLeaveBalances(john.getId());
        seedLeaveBalances(admin.getId());

        System.out.println("[DataSeeder] Employees, users, and leave balances seeded.");
    }

    private Employee seedEmployee(String code, String name, String email,
                                   String phone, String address,
                                   String department, String designation,
                                   UUID managerId, LocalDate joiningDate,
                                   Double salary, String salaryBand, int onboardingPct) {
        Optional<Employee> existing = employeeRepository.findByEmployeeCode(code);
        if (existing.isPresent()) {
            Employee emp = existing.get();
            emp.setSalary(salary);
            emp.setSalaryBand(salaryBand);
            return employeeRepository.save(emp);
        }
        Employee emp = Employee.builder()
                .employeeCode(code)
                .name(name)
                .email(email)
                .phone(phone)
                .address(address)
                .department(department)
                .designation(designation)
                .managerId(managerId)
                .joiningDate(joiningDate)
                .salary(salary)
                .salaryBand(salaryBand)
                .onboardingPercent(onboardingPct)
                .status("ACTIVE")
                .bankAccountNo("")
                .bankIfsc("")
                .bankName("")
                .emergencyName("")
                .emergencyRelation("")
                .emergencyPhone("")
                .build();
        return employeeRepository.save(emp);
    }

    private void seedUser(String email, String empCode, String roleName, String hashedPw) {
        userRepository.findByEmail(email).orElseGet(() -> {
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            User user = User.builder()
                    .email(email)
                    .employeeId(empCode)
                    .password(hashedPw)
                    .role(role)
                    .status("ACTIVE")
                    .build();
            return userRepository.save(user);
        });
    }

    private void seedLeaveBalances(UUID employeeId) {
        int year = LocalDate.now().getYear();
        String[][] types = {
                {"ANNUAL", "20"},
                {"CASUAL", "12"},
                {"SICK", "10"},
                {"MATERNITY", "180"},
                {"PATERNITY", "15"}
        };
        for (String[] t : types) {
            String leaveType = t[0];
            int balance = Integer.parseInt(t[1]);
            Optional<LeaveBalance> existing = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeAndYear(employeeId, leaveType, year);
            if (existing.isEmpty()) {
                LeaveBalance lb = LeaveBalance.builder()
                        .employeeId(employeeId)
                        .leaveType(leaveType)
                        .balance(balance)
                        .year(year)
                        .build();
                leaveBalanceRepository.save(lb);
            }
        }
    }
}
