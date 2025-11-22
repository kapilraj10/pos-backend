package kapil.raj.pos.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import kapil.raj.pos.service.FileUploadService;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private final String uploadDir = "uploads/"; // Make sure this folder exists

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Generate unique filename
            String uniqueFileName = UUID.randomUUID() + extension;

            Path filePath = Paths.get(uploadDir + uniqueFileName);
            Files.createDirectories(filePath.getParent());
            Files.copy(file.getInputStream(), filePath);

            return uniqueFileName; // Save this in DB as imgUrl
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        if (fileName == null) return true;

        Path path = Paths.get(uploadDir + fileName);
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
