package demo.ms.user.repository;

import demo.ms.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRepository extends JpaRepository<User,Long> {

    @Query(value = "SELECT u.* FROM users u LEFT JOIN user_project_ref r ON u.id = r" +
            ".user_id " +
            "WHERE r" +
            ".project_id = ?1",
            nativeQuery = true)
    List<User> getUsersByProjectId(Long projectId);

    @Query(value = "SELECT u.* FROM users u LEFT JOIN authorities a ON u.username = a.username " +
            "WHERE a.authority = ?1",
            nativeQuery = true)
    List<User> getUsersByRole(String role);
}
