package kapil.raj.pos.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetails {
    private PaymentStatus status;

    public enum PaymentStatus {
        PENDING,
        COMPLETED
    }
}
