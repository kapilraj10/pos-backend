package kapil.raj.pos.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KhaltiOrderResponse {

    private String id;
    private String entity;
    private Integer amount;
    private String status;
    private String currency;
    private Date created_at;
    private String receipt;


}
