package com.wallet.repository;

import com.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByOwnerId(Long ownerId);

    Optional<Wallet> findByOwnerIdAndCurrency(Long ownerId, String currency);

    Optional<Wallet> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * Retrieves a wallet by its ID and acquires a pessimistic write lock (SELECT ... FOR UPDATE).
     *
     * <p><strong>Transaction Requirement:</strong> This method MUST be executed within an
     * active transaction context (e.g. an enclosing {@code @Transactional} service method).
     * The row-level lock is acquired on the PostgreSQL {@code wallets} row and held until
     * the surrounding transaction commits or rolls back.</p>
     *
     * <p><strong>Deadlock Prevention:</strong> When transferring between two wallets,
     * the caller must acquire locks in a consistent, deterministic order
     * (e.g. locking {@code min(id1, id2)} first, then {@code max(id1, id2)}).</p>
     *
     * @param id primary key of the wallet to retrieve and lock
     * @return an {@link Optional} containing the locked wallet if found, or empty otherwise
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);
}
