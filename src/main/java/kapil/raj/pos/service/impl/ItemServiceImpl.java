package kapil.raj.pos.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import kapil.raj.pos.entity.CategoryEntity;
import kapil.raj.pos.entity.ItemEntity;
import kapil.raj.pos.io.ItemRequest;
import kapil.raj.pos.io.ItemResponse;
import kapil.raj.pos.repository.CategoryRepository;
import kapil.raj.pos.repository.ItemRepository;
import kapil.raj.pos.service.FileUploadService;
import kapil.raj.pos.service.ItemService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final FileUploadService fileUploadService;
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    @Override
    public ItemResponse add(ItemRequest request, MultipartFile file) {

        // Lookup category by ID first, fallback to name if ID is null
        CategoryEntity category;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByCategoryId(request.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Category not found: " + request.getCategoryId()
                    ));
        } else if (request.getCategoryName() != null && !request.getCategoryName().isEmpty()) {
            category = categoryRepository.findByName(request.getCategoryName())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Category not found: " + request.getCategoryName()
                    ));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category information is required");
        }

        // Build item entity
        ItemEntity newItem = ItemEntity.builder()
                .itemId(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .build();

        // Upload file if present
        if (file != null && !file.isEmpty()) {
            try {
                String imgUrl = fileUploadService.uploadFile(file);
                newItem.setImgUrl(imgUrl);
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to upload image: " + e.getMessage()
                );
            }
        }

        // Save item
        ItemEntity savedItem;
        try {
            savedItem = itemRepository.save(newItem);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save item: " + e.getMessage()
            );
        }

        return convertToResponse(savedItem);
    }

    private ItemResponse convertToResponse(ItemEntity item) {
        return ItemResponse.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .imgUrl(item.getImgUrl())
                .categoryId(item.getCategory().getCategoryId())
                .categoryName(item.getCategory().getName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    @Override
    public List<ItemResponse> fetchItems() {
        return itemRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteItem(String itemId) {
        ItemEntity item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Item not found: " + itemId
                ));

        // Delete file if exists
        if (item.getImgUrl() != null) {
            boolean deleted = fileUploadService.deleteFile(item.getImgUrl());
            if (!deleted) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to delete associated image"
                );
            }
        }

        itemRepository.delete(item);
    }
}
