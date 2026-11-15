package com.smarthospital.auth;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Refresh-token write-lock order is: lock the incoming token first with
 * {@link #findByTokenHashForUpdate(String)}, then lock the complete token family with
 * {@link #findByFamilyIdForUpdate(String)} in ascending token-id order.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyIdAndRevokedAtIsNull(String familyId);

    List<RefreshToken> findByFamilyId(String familyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Locks every token in the family. Callers must use the repository-level lock
     * order documented on this interface and must invoke this query inside the
     * transaction that performs rotation or family revocation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t from RefreshToken t
            join fetch t.user
            where t.familyId = :familyId
            order by t.id asc
            """)
    List<RefreshToken> findByFamilyIdForUpdate(@Param("familyId") String familyId);
}
