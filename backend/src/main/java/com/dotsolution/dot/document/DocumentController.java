package com.dotsolution.dot.document;

import com.dotsolution.dot.common.ApiResponse;
import com.dotsolution.dot.document.entity.EmployeeDocument;
import com.dotsolution.dot.document.entity.MandatoryDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping("/documents/mandatory")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<MandatoryDocument>>> getMandatoryDocuments() {
        return ResponseEntity.ok(ApiResponse.success(documentService.getAllMandatoryDocuments()));
    }

    @PostMapping("/documents/mandatory")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<MandatoryDocument>> addMandatoryDocument(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Document name cannot be empty");
        }
        MandatoryDocument doc = documentService.addMandatoryDocument(name.trim());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(doc, "Mandatory document requirements updated and employees notified"));
    }

    @GetMapping("/employees/{employeeId}/documents")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<List<EmployeeDocument>>> getEmployeeDocuments(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(documentService.getEmployeeDocuments(employeeId)));
    }

    @PostMapping("/employees/{employeeId}/documents")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeDocument>> uploadDocument(
            @PathVariable UUID employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) {
        
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        EmployeeDocument doc = documentService.storeFile(employeeId, file, documentType);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(doc, "Document uploaded successfully to local storage"));
    }

    @GetMapping("/employees/{employeeId}/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID employeeId, @PathVariable UUID documentId) {
        Resource resource = documentService.loadFileAsResource(documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/employees/{employeeId}/documents/{documentId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN', 'FINANCE_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID employeeId, @PathVariable UUID documentId) {
        documentService.deleteDocument(employeeId, documentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }
}
