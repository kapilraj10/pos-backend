package kapil.raj.pos.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import kapil.raj.pos.io.ItemRequest;
import kapil.raj.pos.io.ItemResponse;

public interface ItemService {
    ItemResponse add(ItemRequest itemRequest, MultipartFile file) throws Exception;
    List<ItemResponse> fetchItems();
    void deleteItem(String itemId);
    ItemResponse update(String itemId, ItemRequest itemRequest, MultipartFile file) throws Exception;
    ItemResponse purchase(String itemId, int quantity);
}
