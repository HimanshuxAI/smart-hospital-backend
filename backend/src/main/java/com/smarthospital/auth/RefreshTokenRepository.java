package com.smarthospital.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Refresh-token writes must use this global lock sequence:
 * <ol>
 *     <li>Resolve the incoming token with the non-locking {@link #findByTokenHash(String)}
 *     lookup and read its immutable family id.</li>
 *     <li>Lock the complete family with {@link #findByFamilyIdForUpdate(String)} before
 *     acquiring any refresh-token write lock.</li>
 *     <li>Locate and revalidate the incoming token in the returned locked family set.</li>
 * </ol>
 * A direct locked-token lookup is intentionally not provided: two transactions using
 * distinct incoming tokens could otherwise own different family rows before contending
 * for the family and deadlock.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Non-locking identity lookup used only to obtain the immutable family id.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyIdAndRevokedAtIsNull(String familyId);

    List<RefreshToken> findByFamilyId(String familyId);

    /**
     * Selects a bounded set of families whose latest row is already beyond the
     * cleanup cutoff. The grouped maximum prevents a family with any live row
     * from being selected.
     */
    @Query("""
            select token.familyId
            from RefreshToken token
            group by token.familyId
            having max(token.familyDeadline) < :cutoff
            order by min(token.familyDeadline) asc
            """)
    List<String> findExpiredFamilyIds(Instant cutoff, Pageable pageable);

    /**
     * Locks all refresh credentials belonging to a user in ascending token-id order.
     * This is used only after the target user row has been locked by the activation
     * boundary, preserving the documented user-then-refresh-row lock order.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from RefreshToken t
            where t.user.id = :userId
            order by t.id asc
            """)
    List<RefreshToken> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Locks every token in the family in ascending token-id order. Callers must first
     * resolve the incoming token without a lock, then invoke this query as the first
     * write-lock operation in the transaction that rotates or revokes the family.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from RefreshToken t
            where t.familyId = :familyId
            order by t.id asc
            """)
    List<RefreshToken> findByFamilyIdForUpdate(@Param("familyId") String familyId);
}
