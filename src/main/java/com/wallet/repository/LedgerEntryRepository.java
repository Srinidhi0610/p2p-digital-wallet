package com.wallet.repository;

import com.wallet.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /**
     * Finds all ledger entries generated for a specific transfer.
     * In a completed double-entry transfer, this will return exactly two entries (one DEBIT, one CREDIT).
     *
     * @param transferId ID of the parent transfer
     * @return list of ledger entries associated with the transfer
     */
    List<LedgerEntry> findByTransferId(Long transferId);

    /**
     * Finds all ledger entries affecting a specific wallet.
     *
     * @param walletId ID of the wallet
     * @return list of ledger entries for the wallet
     */
    List<LedgerEntry> findByWalletId(Long walletId);

    /**
     * Finds all ledger entries for a wallet sorted in descending chronological order.
     * Useful for statement generation and audit trail inspection.
     *
     * @param walletId ID of the wallet
     * @return list of ledger entries sorted newest first
     */
    List<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
