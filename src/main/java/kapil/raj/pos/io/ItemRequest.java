package kapil.raj.pos.io;

import lombok.Data;

@Data
public class ItemRequest {
    private String name;
    private String description;
    private String categoryId;
    private double price;
    // Stock quantity for inventory (optional when updating)
    private Integer stock;
}
