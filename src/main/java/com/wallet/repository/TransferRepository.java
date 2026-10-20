package com.wallet.repository;

import com.wallet.entity.Transfer;
import com.wallet.entity.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findBySenderWalletIdOrRecipientWalletId(Long senderWalletId, Long recipientWalletId);

    List<Transfer> findBySenderWalletOwnerIdOrRecipientWalletOwnerId(Long senderOwnerId, Long recipientOwnerId);

    List<Transfer> findByStatus(TransferStatus status);

    /**
     * Finds a transfer by the sender wallet ID and client idempotency key.
     * Matches the composite unique constraint uk_transfers_sender_idempotency (sender_wallet_id, idempotency_key).
     *
     * @param senderWalletId ID of the sender's wallet
     * @param idempotencyKey unique request token scoped to the sender wallet
     * @return an {@link Optional} containing the transfer if one already exists, or empty otherwise
     */
    Optional<Transfer> findBySenderWalletIdAndIdempotencyKey(Long senderWalletId, String idempotencyKey);

    /**
     * Retrieves a paginated list of transfers where the specified user is the owner
     * of either the sender wallet or the recipient wallet.
     *
     * @param userId ID of the user
     * @param pageable pagination and sorting parameters
     * @return page of transfers involving the user
     */
    @Query("SELECT t FROM Transfer t WHERE t.senderWallet.owner.id = :userId OR t.recipientWallet.owner.id = :userId")
    Page<Transfer> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
