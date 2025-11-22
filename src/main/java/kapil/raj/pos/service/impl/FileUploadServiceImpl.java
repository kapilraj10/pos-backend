package kapil.raj.pos.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import kapil.raj.pos.service.FileUploadService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final Cloudinary cloudinary;
    
    @Value("${cloudinary.folder:pos}")
    private String folderName;

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // Generate unique public ID
            String publicId = UUID.randomUUID().toString();
            
            // Upload to Cloudinary
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folderName,
                    "resource_type", "auto"
                )
            );
            
            // Return the secure URL from Cloudinary
            String imageUrl = (String) uploadResult.get("secure_url");
            System.out.println("✅ Image uploaded to Cloudinary: " + imageUrl);
            
            return imageUrl;
            
        } catch (IOException e) {
            System.err.println("❌ Failed to upload to Cloudinary: " + e.getMessage());
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return true;
        }

        try {
            // Extract public_id from Cloudinary URL
            // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/{folder}/{public_id}.ext
            String publicId = extractPublicIdFromUrl(imageUrl);
            
            if (publicId == null) {
                System.err.println("⚠️ Could not extract public_id from URL: " + imageUrl);
                return false;
            }
            
            // Delete from Cloudinary
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.emptyMap()
            );
            
            String resultStatus = (String) result.get("result");
            boolean deleted = "ok".equals(resultStatus);
            
            if (deleted) {
                System.out.println("✅ Image deleted from Cloudinary: " + publicId);
            } else {
                System.err.println("⚠️ Failed to delete from Cloudinary: " + resultStatus);
            }
            
            return deleted;
            
        } catch (IOException e) {
            System.err.println("❌ Error deleting from Cloudinary: " + e.getMessage());
            return false;
        }
    }
    
    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            // Extract public_id from URL like:
            // https://res.cloudinary.com/dxudm3c5s/image/upload/v1234567890/pos/uuid.jpg
            // Should return: pos/uuid
            
            if (!imageUrl.contains("/upload/")) {
                return null;
            }
            
            String afterUpload = imageUrl.substring(imageUrl.indexOf("/upload/") + 8);
            
            // Remove version if present (v1234567890/)
            if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }
            
            // Remove file extension
            if (afterUpload.contains(".")) {
                afterUpload = afterUpload.substring(0, afterUpload.lastIndexOf("."));
            }
            
            return afterUpload;
            
        } catch (Exception e) {
            System.err.println("Error extracting public_id: " + e.getMessage());
            return null;
        }
    }
}
