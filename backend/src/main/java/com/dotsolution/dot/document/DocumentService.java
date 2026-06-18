package com.dotsolution.dot.document;

import com.dotsolution.dot.auth.entity.User;
import com.dotsolution.dot.auth.repository.UserRepository;
import com.dotsolution.dot.common.EntityNotFoundException;
import com.dotsolution.dot.document.entity.EmployeeDocument;
import com.dotsolution.dot.document.entity.MandatoryDocument;
import com.dotsolution.dot.document.repository.EmployeeDocumentRepository;
import com.dotsolution.dot.document.repository.MandatoryDocumentRepository;
import com.dotsolution.dot.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {

    private final Path fileStorageLocation;

    @Autowired
    private MandatoryDocumentRepository mandatoryDocumentRepository;

    @Autowired
    private EmployeeDocumentRepository employeeDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public DocumentService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public List<MandatoryDocument> getAllMandatoryDocuments() {
        return mandatoryDocumentRepository.findAll();
    }

    public MandatoryDocument addMandatoryDocument(String name) {
        if (mandatoryDocumentRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Mandatory document already exists: " + name);
        }

        MandatoryDocument doc = MandatoryDocument.builder()
                .name(name)
                .build();
        MandatoryDocument saved = mandatoryDocumentRepository.save(doc);

        // Notify all users
        List<User> users = userRepository.findAll();
        for (User user : users) {
            notificationService.sendNotification(
                    user.getId(),
                    "New Required Document",
                    "A new required document '" + name + "' has been added. Please upload it in your My Documents tab."
            );
        }

        return saved;
    }

    public List<EmployeeDocument> getEmployeeDocuments(UUID employeeId) {
        return employeeDocumentRepository.findByEmployeeId(employeeId);
    }

    public EmployeeDocument storeFile(UUID employeeId, MultipartFile file, String documentType) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new RuntimeException("Filename contains invalid path sequence " + originalFileName);
        }

        try {
            Path targetDir = this.fileStorageLocation.resolve(employeeId.toString());
            Files.createDirectories(targetDir);

            // Clean old file of same documentType if exists
            Optional<EmployeeDocument> existing = employeeDocumentRepository
                    .findByEmployeeIdAndDocumentType(employeeId, documentType);
            if (existing.isPresent()) {
                EmployeeDocument oldDoc = existing.get();
                try {
                    Files.deleteIfExists(Paths.get(oldDoc.getFilePath()));
                } catch (Exception e) {
                    // Ignore delete errors for old local files
                }
                employeeDocumentRepository.delete(oldDoc);
            }

            String cleanFileName = System.currentTimeMillis() + "_" + originalFileName;
            Path targetLocation = targetDir.resolve(cleanFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileSize = String.format("%.1f KB", (double) file.getSize() / 1024);

            EmployeeDocument employeeDocument = EmployeeDocument.builder()
                    .employeeId(employeeId)
                    .documentType(documentType)
                    .fileName(originalFileName)
                    .filePath(targetLocation.toString())
                    .fileSize(fileSize)
                    .build();

            return employeeDocumentRepository.save(employeeDocument);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(UUID documentId) {
        EmployeeDocument doc = employeeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + documentId));
        try {
            Path filePath = Paths.get(doc.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new EntityNotFoundException("File not found: " + doc.getFileName());
            }
        } catch (MalformedURLException ex) {
            throw new EntityNotFoundException("File not found: " + doc.getFileName());
        }
    }

    public void deleteDocument(UUID employeeId, UUID documentId) {
        EmployeeDocument doc = employeeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));

        if (!doc.getEmployeeId().equals(employeeId)) {
            throw new SecurityException("Unauthorized to delete this document");
        }

        try {
            Files.deleteIfExists(Paths.get(doc.getFilePath()));
        } catch (Exception e) {
            // Ignore file deletion errors
        }
        employeeDocumentRepository.delete(doc);
    }
}
