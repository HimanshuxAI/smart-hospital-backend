package com.smarthospital.user;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Administrator activation writes use this global lock sequence:
 * <ol>
 *     <li>Resolve the target without a lock.</li>
 *     <li>When the target is an administrator, lock all administrator rows in
 *     ascending id order and evaluate their current active state in memory.</li>
 *     <li>Lock the target user row.</li>
 *     <li>Lock and revoke the target user's refresh-token rows in ascending id order.</li>
 * </ol>
 * Refresh rotation only locks a complete token family and never acquires a user
 * write lock, so the two write paths do not form a lock-order cycle.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from User u
            where u.role = com.smarthospital.user.UserRole.ADMIN
            order by u.id asc
            """)
    List<User> findAdministratorsForUpdate();
}
