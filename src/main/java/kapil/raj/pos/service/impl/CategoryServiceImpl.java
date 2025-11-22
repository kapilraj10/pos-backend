package kapil.raj.pos.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import kapil.raj.pos.entity.CategoryEntity;
import kapil.raj.pos.io.CategoryRequest;
import kapil.raj.pos.io.CategoryResponse;
import kapil.raj.pos.repository.CategoryRepository;
import kapil.raj.pos.repository.ItemRepository;
import kapil.raj.pos.service.CategoryService;
import kapil.raj.pos.service.FileUploadService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final FileUploadService fileUploadService;
    private final ItemRepository itemRepository;

    // -------------------------------
    //   CREATE CATEGORY
    // -------------------------------
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

    // -------------------------------
    //   READ ALL CATEGORIES
    // -------------------------------
    @Override
    public List<CategoryResponse> read() {
        return categoryRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------
    //   DELETE CATEGORY
    // -------------------------------
    @Override
    public void delete(String categoryId) {
        CategoryEntity existingCategory = categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")
                );

        // delete Cloudinary image
        if (existingCategory.getImgUrl() != null && !existingCategory.getImgUrl().isEmpty()) {
            fileUploadService.deleteFile(existingCategory.getImgUrl());
        }

        // delete DB record
        categoryRepository.delete(existingCategory);
    }

    // -------------------------------
    //   ENTITY ↔ DTO CONVERTERS
    // -------------------------------
    private CategoryResponse convertToResponse(CategoryEntity entity) {
        if (entity == null) return null;

        Integer itemsCount = itemRepository.countByCategory_CategoryId(entity.getCategoryId());

        return CategoryResponse.builder()
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .description(entity.getDescription())
                .bgColor(entity.getBgColor())
                .imgUrl(entity.getImgUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(itemsCount)
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
