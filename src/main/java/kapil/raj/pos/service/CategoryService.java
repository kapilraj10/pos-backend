package kapil.raj.pos.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import kapil.raj.pos.io.CategoryRequest;
import kapil.raj.pos.io.CategoryResponse;

public interface CategoryService {
    CategoryResponse add(CategoryRequest request, MultipartFile file);


    List<CategoryResponse> read();


    void delete(String categoryId);
    
}
