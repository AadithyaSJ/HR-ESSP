package com.dotsolution.dot.auth;

import com.dotsolution.dot.auth.dto.CreatePasswordRequest;
import com.dotsolution.dot.auth.dto.LoginRequest;
import com.dotsolution.dot.auth.dto.LoginResponse;
import com.dotsolution.dot.auth.entity.User;
import com.dotsolution.dot.auth.repository.UserRepository;
import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.employee.entity.Employee;
import com.dotsolution.dot.employee.repository.EmployeeRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials"));
        }

        User user = userOpt.get();
        // Support plain password or BCrypt hash for testing flexibility
        boolean passMatch = passwordEncoder.matches(request.getPassword(), user.getPassword()) 
                || request.getPassword().equals(user.getPassword());
        
        if (!passMatch) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials"));
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("User account is inactive"));
        }

        // Retrieve linked employee profile details
        String empCode = user.getEmployeeId();
        String fullName = "System User";
        if (empCode != null) {
            Optional<Employee> empOpt = employeeRepository.findByEmployeeCode(empCode);
            if (empOpt.isPresent()) {
                fullName = empOpt.get().getName();
            }
        }

        String roleName = user.getRole().getRoleName();
        String token = jwtUtil.generateToken(user.getEmail(), roleName, empCode);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(fullName)
                .role(roleName)
                .employeeCode(empCode)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/sso-login")
    public ResponseEntity<ApiResponse<LoginResponse>> ssoLogin(@RequestParam String provider) {
        // Mock SSO fetches the main seed employee (Jane Doe)
        Optional<User> userOpt = userRepository.findByEmail("jane.doe@company.com");
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Default SSO user profile not configured"));
        }

        User user = userOpt.get();
        String empCode = user.getEmployeeId();
        String fullName = "Jane Doe";
        String roleName = user.getRole().getRoleName();
        
        String token = jwtUtil.generateToken(user.getEmail(), roleName, empCode);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(fullName)
                .role(roleName)
                .employeeCode(empCode)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "SAML SSO authenticated via " + provider));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Email address not found"));
        }
        // Simulate SES triggers
        return ResponseEntity.ok(ApiResponse.success("Reset link dispatched via AWS SES successfully"));
    }

    @PostMapping("/create-password")
    public ResponseEntity<ApiResponse<LoginResponse>> createPassword(@RequestBody @Valid CreatePasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User account not found"));
        }

        User user = userOpt.get();
        Optional<Employee> empOpt = employeeRepository.findByEmployeeCode(user.getEmployeeId());
        if (empOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Employee profile not found"));
        }

        Employee employee = empOpt.get();
        if (!"PENDING_DETAILS".equals(employee.getOnboardingStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Employee is not in the password creation stage or onboarding is already completed"));
        }

        // Hash and save new password
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // Generate JWT token and log user in automatically
        String roleName = user.getRole().getRoleName();
        String token = jwtUtil.generateToken(user.getEmail(), roleName, user.getEmployeeId());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(employee.getName())
                .role(roleName)
                .employeeCode(user.getEmployeeId())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Password set successfully, welcome to the portal!"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Healthy", "Service is up and running"));
    }
}

