package kapil.raj.pos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kapil.raj.pos.io.ItemRequest;
import kapil.raj.pos.io.ItemResponse;
import kapil.raj.pos.service.ItemService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse addItem(
            @RequestPart("item") String itemString,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        ObjectMapper objectMapper = new ObjectMapper();
        ItemRequest itemRequest;

        try {
            // Parse JSON string to ItemRequest
            itemRequest = objectMapper.readValue(itemString, ItemRequest.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON format in 'item'", e);
        }

        // Validation
        if (itemRequest.getName() == null || itemRequest.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item name is required");
        }
        if (itemRequest.getPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item price must be greater than 0");
        }
        if (itemRequest.getCategoryId() == null || itemRequest.getCategoryId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }
        if (itemRequest.getStock() != null && itemRequest.getStock() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }

        try {
            // Call service to add the item
            return itemService.add(itemRequest, file);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add item: " + e.getMessage(), e);
        }
    }

    // Public endpoint for reducing stock when a purchase occurs (guest checkout)
    @PostMapping("/items/{itemId}/purchase")
    @ResponseStatus(HttpStatus.OK)
    public ItemResponse purchaseItem(@PathVariable String itemId, @RequestBody java.util.Map<String, Object> body) {
        int qty = 1;
        if (body != null && body.get("quantity") instanceof Number) {
            qty = ((Number) body.get("quantity")).intValue();
        }
        try {
            return itemService.purchase(itemId, qty);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/items")
    public List<ItemResponse> getItems() {
        return itemService.fetchItems();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/admin/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable String itemId) {
        try {
            itemService.deleteItem(itemId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + itemId);
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/admin/items/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public ItemResponse updateItem(
            @PathVariable String itemId,
            @RequestPart("item") String itemString,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        // Log authentication info for debugging 403 issues
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                logger.info("UpdateItem called by principal={}, authorities={}", auth.getName(), auth.getAuthorities());
            } else {
                logger.warn("UpdateItem called with no authentication in SecurityContext");
            }
        } catch (Exception e) {
            logger.error("Failed to read authentication info: {}", e.getMessage());
        }
        ObjectMapper objectMapper = new ObjectMapper();
        ItemRequest itemRequest;

        try {
            itemRequest = objectMapper.readValue(itemString, ItemRequest.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON format in 'item'", e);
        }

        try {
            return itemService.update(itemId, itemRequest, file);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update item: " + e.getMessage(), e);
        }
    }
}
