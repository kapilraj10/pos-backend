package kapil.raj.pos.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import kapil.raj.pos.entity.CategoryEntity;
import kapil.raj.pos.io.CategoryRequest;
import kapil.raj.pos.io.CategoryResponse;
import kapil.raj.pos.repository.CategoryRepository;
import kapil.raj.pos.service.CategoryService;
import kapil.raj.pos.service.FileUploadService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final FileUploadService fileUploadService;

    @Override
    public void delete(String categoryId) {
        CategoryEntity existingCategory = categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (existingCategory.getImgUrl() != null && !existingCategory.getImgUrl().isEmpty()) {
            fileUploadService.deleteFile(existingCategory.getImgUrl());
        }

        categoryRepository.delete(existingCategory);
    }

    @Override
    public CategoryResponse add(CategoryRequest request, MultipartFile file) {
        String imgUrl = null;
        if (file != null && !file.isEmpty()) {
            imgUrl = fileUploadService.uploadFile(file);
        }

        CategoryEntity newCategory = convertToEntity(request);
        newCategory.setImgUrl(imgUrl);

        newCategory = categoryRepository.save(newCategory);

        return convertToResponse(newCategory);
    }

    @Override
    public List<CategoryResponse> read() {
        return categoryRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private CategoryResponse convertToResponse(CategoryEntity categoryEntity) {
        if (categoryEntity == null) return null;

        return CategoryResponse.builder()
                .categoryId(categoryEntity.getCategoryId())
                .name(categoryEntity.getName())
                .description(categoryEntity.getDescription())
                .bgColor(categoryEntity.getBgColor())
                .imgUrl(categoryEntity.getImgUrl())
                .createdAt(categoryEntity.getCreatedAt())
                .updatedAt(categoryEntity.getUpdatedAt())
                .build();
    }

    private CategoryEntity convertToEntity(CategoryRequest request) {
        return CategoryEntity.builder()
                .categoryId(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .bgColor(request.getBgColor())
                .build();
    }
}
