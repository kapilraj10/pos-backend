package kapil.raj.pos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
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
            itemRequest = objectMapper.readValue(itemString, ItemRequest.class);

            // Validation
            if (itemRequest.getName() == null || itemRequest.getName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item name is required");
            }
            if (itemRequest.getPrice() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item price must be > 0");
            }
            if (itemRequest.getCategoryId() == null || itemRequest.getCategoryId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
            }

            return itemService.add(itemRequest, file);

        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON format in 'item'");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
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
}
