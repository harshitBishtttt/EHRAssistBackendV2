package ehrAssist.repository;

import ehrAssist.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, UUID> {

    Optional<UserAccountEntity> findByEmail(String email);

    List<UserAccountEntity> findByRoleNot(String role);

    boolean existsByEmail(String email);
}
