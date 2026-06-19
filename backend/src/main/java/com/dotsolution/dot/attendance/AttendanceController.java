package com.dotsolution.dot.attendance;

import com.dotsolution.dot.attendance.entity.Attendance;
import com.dotsolution.dot.attendance.repository.AttendanceRepository;
import com.dotsolution.dot.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @PostMapping("/punch-in")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN', 'IT_SUPPORT')")
    public ResponseEntity<ApiResponse<Attendance>> punchIn(
            @RequestParam UUID employeeId,
            @RequestParam(defaultValue = "PRESENT") String status) {
        
        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today);
        
        Attendance record;
        if (existing.isPresent()) {
            record = existing.get();
        } else {
            String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
            record = Attendance.builder()
                    .employeeId(employeeId)
                    .workDate(today)
                    .punchIn(timeStr)
                    .status(status)
                    .build();
            record = attendanceRepository.save(record);
        }
        return ResponseEntity.ok(ApiResponse.success(record, "Punched in successfully"));
    }

    @PostMapping("/punch-out")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN', 'IT_SUPPORT')")
    public ResponseEntity<ApiResponse<Attendance>> punchOut(@RequestParam UUID employeeId) {
        LocalDate today = LocalDate.now();
        Attendance record = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseGet(() -> Attendance.builder()
                        .employeeId(employeeId)
                        .workDate(today)
                        .status("PRESENT")
                        .build());
        
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
        record.setPunchOut(timeStr);
        Attendance saved = attendanceRepository.save(record);
        return ResponseEntity.ok(ApiResponse.success(saved, "Punched out successfully"));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN', 'IT_SUPPORT')")
    public ResponseEntity<ApiResponse<List<Attendance>>> getLogs(@RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceRepository.findByEmployeeId(employeeId)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('HR_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<Attendance>>> getAllLogs() {
        return ResponseEntity.ok(ApiResponse.success(attendanceRepository.findAll()));
    }
}
