package com.dotsolution.dot.leave;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.leave.entity.LeaveBalance;
import com.dotsolution.dot.leave.entity.LeaveRequest;
import com.dotsolution.dot.leave.entity.PublicHoliday;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private com.dotsolution.dot.leave.repository.PublicHolidayRepository publicHolidayRepository;

    @GetMapping("/balances")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveBalance>>> getBalances(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveBalances(employeeId)));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getRequests(@RequestParam(required = false) UUID employeeId) {
        if (employeeId != null) {
            return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveRequests(employeeId)));
        } else {
            return ResponseEntity.ok(ApiResponse.success(leaveService.getAllLeaveRequests()));
        }
    }

    @GetMapping("/requests/manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getManagerRequests(@RequestParam UUID managerId) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getManagerLeaveRequests(managerId)));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequest>> createRequest(@RequestBody LeaveRequest request) {
        LeaveRequest created = leaveService.createLeaveRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Leave request submitted successfully"));
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequest>> approveRequest(
            @PathVariable UUID id, 
            @RequestParam(required = false) String comment) {
        LeaveRequest approved = leaveService.approveLeaveRequest(id, comment);
        return ResponseEntity.ok(ApiResponse.success(approved, "Leave request approved successfully"));
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequest>> rejectRequest(
            @PathVariable UUID id, 
            @RequestParam(required = false) String comment) {
        LeaveRequest rejected = leaveService.rejectLeaveRequest(id, comment);
        return ResponseEntity.ok(ApiResponse.success(rejected, "Leave request rejected successfully"));
    }

    @PostMapping("/requests/{id}/cancel")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveRequest>> cancelRequest(@PathVariable UUID id) {
        LeaveRequest cancelled = leaveService.cancelLeaveRequest(id);
        return ResponseEntity.ok(ApiResponse.success(cancelled, "Leave request cancelled successfully"));
    }

    @GetMapping("/holidays")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<PublicHoliday>>> getHolidays() {
        return ResponseEntity.ok(ApiResponse.success(publicHolidayRepository.findAll(), "Holiday list retrieved"));
    }

    @PostMapping("/holidays")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<PublicHoliday>> addHoliday(@RequestBody PublicHoliday holiday) {
        if (publicHolidayRepository.findByCountryAndHolidayDate(holiday.getCountry(), holiday.getHolidayDate()).isPresent()) {
            throw new IllegalArgumentException("Public holiday already exists for this date and country");
        }
        PublicHoliday saved = publicHolidayRepository.save(holiday);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(saved, "Public holiday added successfully"));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        String originalFileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new RuntimeException("Filename contains invalid path sequence " + originalFileName);
        }
        
        try {
            Path storageLoc = Paths.get("uploads", "attachments").toAbsolutePath().normalize();
            Files.createDirectories(storageLoc);
            
            String cleanFileName = System.currentTimeMillis() + "_" + originalFileName;
            Path targetLocation = storageLoc.resolve(cleanFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                "fileName", originalFileName,
                "filePath", targetLocation.toString()
            ), "File uploaded successfully"));
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName, ex);
        }
    }

    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadAttachment(@RequestParam("path") String path) {
        try {
            Path filePath = Paths.get(path).normalize();
            if (!filePath.startsWith(Paths.get("uploads").toAbsolutePath().normalize())) {
                throw new SecurityException("Unauthorized file path access");
            }
            
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (resource.exists()) {
                String cleanName = resource.getFilename();
                if (cleanName != null && cleanName.contains("_")) {
                    cleanName = cleanName.substring(cleanName.indexOf("_") + 1);
                }
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cleanName + "\"")
                        .body(resource);
            } else {
                throw new com.dotsolution.dot.common.EntityNotFoundException("File not found");
            }
        } catch (java.net.MalformedURLException ex) {
            throw new com.dotsolution.dot.common.EntityNotFoundException("File not found: " + ex.getMessage());
        }
    }
}
