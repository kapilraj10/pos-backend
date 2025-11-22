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

    // Helper method
    private ItemResponse mapToResponse(ItemEntity item) {
        ItemResponse resp = new ItemResponse();
        resp.setId(item.getItemId());
        resp.setName(item.getName());
        resp.setPrice(item.getPrice().doubleValue());
        resp.setDescription(item.getDescription());
        resp.setCategoryId(item.getCategory().getCategoryId());
        resp.setImgUrl(item.getImgUrl());
        return resp;
    }
}
