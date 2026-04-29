package ehrAssist.service;

import ehrAssist.dto.request.CreateUserRequest;
import ehrAssist.dto.request.LoginRequest;
import ehrAssist.dto.request.UpdateUserRequest;
import ehrAssist.dto.response.LoginResponse;
import ehrAssist.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    LoginResponse login(LoginRequest request);

    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> listUsers();

    UserResponse getUserById(UUID id);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void deactivateUser(UUID id);
}
