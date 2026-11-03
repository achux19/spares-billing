package com.sparesbilling.backend.user;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Users are NOT filtered automatically (the Super Admin has no shop), so every method here
 * names the shop explicitly. There is deliberately no plain findByUsername.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByShopIdAndUsername(Long shopId, String username);

    /** The platform Super Admin (no shop). */
    Optional<User> findByShopIdIsNullAndUsername(String username);

    Optional<User> findByIdAndShopId(Long id, Long shopId);

    boolean existsByShopIdAndUsername(Long shopId, String username);

    long countByShopIdAndRoleAndActive(Long shopId, Role role, boolean active);

    List<User> findAllByShopId(Long shopId, Sort sort);

    long countByRole(Role role);
}
