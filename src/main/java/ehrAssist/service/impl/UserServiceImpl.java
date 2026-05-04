package ehrAssist.service.impl;

import ehrAssist.dto.request.CreateUserRequest;
import ehrAssist.dto.request.LoginRequest;
import ehrAssist.dto.request.UpdateUserRequest;
import ehrAssist.dto.response.LoginResponse;
import ehrAssist.dto.response.UserResponse;
import ehrAssist.entity.UserAccountEntity;
import ehrAssist.repository.UserAccountRepository;
import ehrAssist.security.jwt.JwtUtil;
import ehrAssist.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    private final UserAccountRepository userAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserAccountEntity user = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts >= MAX_LOGIN_ATTEMPTS ? 0 : attempts);
            user.setUpdatedAt(LocalDateTime.now());
            userAccountRepository.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Successful login — reset failed attempts counter
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userAccountRepository.save(user);

        UUID refId = resolveRefId(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole(), refId);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtUtil.getExpiryMs())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .refId(refId)
                .build();
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user with email '" + request.getEmail() + "' already exists");
        }

        if ("PATIENT".equals(request.getRole())) {
            if (request.getPatientRefId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "patientRefId is required when role is PATIENT");
            }
            if (userAccountRepository.existsByPatientRefId(request.getPatientRefId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A user account for patient '" + request.getPatientRefId() + "' already exists");
            }
        }

        if ("PROVIDER".equals(request.getRole())) {
            if (request.getPractitionerRefId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "practitionerRefId is required when role is PROVIDER");
            }
            if (userAccountRepository.existsByPractitionerRefId(request.getPractitionerRefId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A user account for practitioner '" + request.getPractitionerRefId() + "' already exists");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        UserAccountEntity entity = UserAccountEntity.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isActive(true)
                .failedLoginAttempts(0)
                .patientRefId(request.getPatientRefId())
                .practitionerRefId(request.getPractitionerRefId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            return UserResponse.from(userAccountRepository.save(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User account already exists — duplicate email, patient, or practitioner reference");
        }
    }

    @Override
    public List<UserResponse> listUsers() {
        return userAccountRepository.findByRoleNot("ADMIN")
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    public UserResponse getUserById(UUID id) {
        return userAccountRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        UserAccountEntity entity = userAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));

        if ("ADMIN".equals(entity.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify ADMIN account");
        }

        if (request.getRole() != null) {
            entity.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
        entity.setUpdatedAt(LocalDateTime.now());

        return UserResponse.from(userAccountRepository.save(entity));
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        UserAccountEntity entity = userAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));

        if ("ADMIN".equals(entity.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot deactivate ADMIN account");
        }

        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        userAccountRepository.save(entity);
    }

    private UUID resolveRefId(UserAccountEntity user) {
        return switch (user.getRole()) {
            case "PATIENT"  -> user.getPatientRefId();
            case "PROVIDER" -> user.getPractitionerRefId();
            default         -> null;
        };
    }
}
