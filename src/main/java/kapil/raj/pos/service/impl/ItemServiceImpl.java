package kapil.raj.pos.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final FileUploadService fileUploadService;

    @Override
    public ItemResponse add(ItemRequest itemRequest, MultipartFile file) {
        try {
            // Find category
            CategoryEntity category = categoryRepository.findByCategoryId(itemRequest.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found: " + itemRequest.getCategoryId()));

            // Upload file to Cloudinary
            String imageUrl = null;
            if (file != null && !file.isEmpty()) {
                imageUrl = fileUploadService.uploadFile(file);
            }

            // Create Item entity
            ItemEntity item = ItemEntity.builder()
                    .itemId(java.util.UUID.randomUUID().toString())
                    .name(itemRequest.getName())
                    .price(java.math.BigDecimal.valueOf(itemRequest.getPrice()))
                    .description(itemRequest.getDescription())
                    .category(category)
                    .imgUrl(imageUrl)
            .stock(itemRequest.getStock() == null ? 0 : itemRequest.getStock())
                    .build();

            // Save item in DB
            ItemEntity savedItem = itemRepository.save(item);

            // Return response
            return mapToResponse(savedItem);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to add item: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<ItemResponse> fetchItems() {
        List<ItemEntity> items = itemRepository.findAll();
        List<ItemResponse> response = new ArrayList<>();
        for (ItemEntity item : items) {
            response.add(mapToResponse(item));
        }
        return response;
    }

    @Override
    public void deleteItem(String itemId) {
        ItemEntity item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        
        // Delete file from Cloudinary if exists
        if (item.getImgUrl() != null && !item.getImgUrl().isEmpty()) {
            try {
                fileUploadService.deleteFile(item.getImgUrl());
            } catch (Exception e) {
                System.err.println("Failed to delete file from Cloudinary: " + e.getMessage());
            }
        }
        
        // Delete DB record
        itemRepository.delete(item);
    }

    @Override
    public ItemResponse update(String itemId, ItemRequest itemRequest, MultipartFile file) {
        try {
            ItemEntity item = itemRepository.findByItemId(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

            // If category changed, fetch new category
            if (itemRequest.getCategoryId() != null && !itemRequest.getCategoryId().isBlank()
                    && !item.getCategory().getCategoryId().equals(itemRequest.getCategoryId())) {
                CategoryEntity newCategory = categoryRepository.findByCategoryId(itemRequest.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Category not found: " + itemRequest.getCategoryId()));
                item.setCategory(newCategory);
            }

            // Update fields
            if (itemRequest.getName() != null && !itemRequest.getName().isBlank()) {
                item.setName(itemRequest.getName());
            }
            if (itemRequest.getPrice() > 0) {
                item.setPrice(java.math.BigDecimal.valueOf(itemRequest.getPrice()));
            }
            if (itemRequest.getDescription() != null) {
                item.setDescription(itemRequest.getDescription());
            }

            // Update stock if provided
            if (itemRequest.getStock() != null) {
                if (itemRequest.getStock() < 0) {
                    throw new RuntimeException("Stock cannot be negative");
                }
                item.setStock(itemRequest.getStock());
            }

            // If new file provided, upload and delete old
            if (file != null && !file.isEmpty()) {
                try {
                    String newUrl = fileUploadService.uploadFile(file);
                    if (item.getImgUrl() != null && !item.getImgUrl().isEmpty()) {
                        fileUploadService.deleteFile(item.getImgUrl());
                    }
                    item.setImgUrl(newUrl);
                } catch (Exception e) {
                    // Log but don't fail the whole update unless needed
                    System.err.println("Failed to replace file: " + e.getMessage());
                }
            }

            ItemEntity saved = itemRepository.save(item);
            return mapToResponse(saved);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to update item: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ItemResponse purchase(String itemId, int quantity) {
        if (quantity <= 0) throw new RuntimeException("Quantity must be > 0");
        ItemEntity item = itemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));

        synchronized (this) {
            int current = item.getStock() == null ? 0 : item.getStock();
            // Business rule: if item is already low stock (<=5), only allow single-unit purchases
            if (current <= 5 && quantity > 1) {
                throw new RuntimeException("Item is low in stock (" + current + ") - max 1 unit can be purchased");
            }

            if (current < quantity) {
                throw new RuntimeException("Insufficient stock for item: " + itemId);
            }
            item.setStock(current - quantity);
            ItemEntity saved = itemRepository.save(item);
            return mapToResponse(saved);
        }
    }

    // Helper method
    private ItemResponse mapToResponse(ItemEntity item) {
        ItemResponse resp = new ItemResponse();
        resp.setId(item.getItemId());
        resp.setName(item.getName());
        resp.setPrice(item.getPrice().doubleValue());
        resp.setDescription(item.getDescription());
        resp.setCategoryId(item.getCategory().getCategoryId());
        resp.setImgUrl(item.getImgUrl());
        resp.setStock(item.getStock());
        return resp;
    }
}
