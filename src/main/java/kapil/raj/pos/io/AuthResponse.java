package kapil.raj.pos.io;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String email;
    private String token;
    private String role;


    public AuthResponse(String email, String role, String jwtToken) {
        this.email = email;
        this.role = role;
        this.token = jwtToken;
    }
}
