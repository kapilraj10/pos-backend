package kapil.raj.pos.io;

import lombok.Data;

@Data
public class ItemResponse {
    private String id;
    private String name;
    private String description;
    private String categoryId;
    private double price;
    private String imgUrl;
    private Integer stock;
}
