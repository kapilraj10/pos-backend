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

    // -----------------------
    // DEBUG - Check Authentication Status
    // -----------------------
    @GetMapping("/admin/auth-check")
    public String checkAuth() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "No authentication found!";
        return "User: " + auth.getName() + " | Authorities: " + auth.getAuthorities();
    }

    // -----------------------
    // POST - Admin only
    // -----------------------
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse addItem(
            @RequestPart("item") String itemString,
            @RequestPart("file") MultipartFile file
    ) {
        // DEBUG: Log authentication info
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("🔍 POST /admin/items - Auth: " + (auth != null ? auth.getName() : "NULL") + 
                          " | Authorities: " + (auth != null ? auth.getAuthorities() : "NONE"));
        
        ObjectMapper objectMapper = new ObjectMapper();
        ItemRequest itemRequest;

        try {
            // Convert JSON string into ItemRequest object
            itemRequest = objectMapper.readValue(itemString, ItemRequest.class);

            // Call service to add item
            return itemService.add(itemRequest, file);

        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid JSON format in 'item' body"
            );
        }
    }

    // -----------------------
    // GET - Public
    // -----------------------
    @GetMapping("/items")
    public List<ItemResponse> getItems() {
        return itemService.fetchItems();
    }

    // -----------------------
    // DELETE - Admin only
    // -----------------------
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/admin/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable String itemId) {
        // DEBUG: Log authentication info
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        System.out.println("🔍 DELETE /admin/items/" + itemId + " - Auth: " + (auth != null ? auth.getName() : "NULL") + 
                          " | Authorities: " + (auth != null ? auth.getAuthorities() : "NONE"));
        
        try {
            itemService.deleteItem(itemId);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Item not found: " + itemId
            );
        }
    }
}
