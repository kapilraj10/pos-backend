package kapil.raj.pos.service;

import kapil.raj.pos.io.UserRequest;
import kapil.raj.pos.io.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserRequest request);

    String getUserRole(String email);
    List<UserResponse > readUser();

    void deleteUser(String id);

}
