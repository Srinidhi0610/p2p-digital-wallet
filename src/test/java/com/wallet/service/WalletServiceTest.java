package com.wallet.service;

import com.wallet.dto.CreateWalletRequest;
import com.wallet.dto.DemoDepositRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.entity.*;
import com.wallet.exception.DuplicateWalletException;
import com.wallet.exception.InvalidTransferAmountException;
import com.wallet.exception.UnauthorizedAccessException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransferRepository;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private WalletService walletService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, userRepository, transferRepository, ledgerEntryRepository);

        alice = new User("Alice", "alice@example.com", "hash1");
        alice.setId(1L);

        bob = new User("Bob", "bob@example.com", "hash2");
        bob.setId(2L);
    }

    @Test
    @DisplayName("createWallet enforces zero starting balance, normalizes currency, and assigns owner")
    void createWalletShouldEnforceZeroBalanceAndNormalizedCurrency() {
        CreateWalletRequest request = new CreateWalletRequest("usd");

        when(walletRepository.findByOwnerIdAndCurrency(1L, "USD")).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(100L);
            wallet.setCreatedAt(Instant.now());
            return wallet;
        });

        WalletResponse response = walletService.createWallet(1L, request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(1L, response.userId());
        assertEquals("USD", response.currency());
        assertEquals(new BigDecimal("0.0000"), response.balance());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet saved = walletCaptor.getValue();

        assertEquals(alice, saved.getOwner());
        assertEquals("USD", saved.getCurrency());
        assertEquals(new BigDecimal("0.0000"), saved.getBalance());
    }

    @Test
    @DisplayName("createWallet rejects duplicate currency for the same user")
    void createWalletShouldRejectDuplicateCurrency() {
        CreateWalletRequest request = new CreateWalletRequest("EUR");
        when(walletRepository.findByOwnerIdAndCurrency(1L, "EUR"))
                .thenReturn(Optional.of(new Wallet(alice, "EUR", BigDecimal.ZERO)));

        assertThrows(DuplicateWalletException.class, () -> walletService.createWallet(1L, request));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("getWalletById allows owner to view wallet")
    void getWalletByIdShouldAllowOwner() {
        Wallet wallet = new Wallet(alice, "USD", new BigDecimal("50.0000"));
        wallet.setId(10L);
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWalletById(1L, 10L);
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(new BigDecimal("50.0000"), response.balance());
    }

    @Test
    @DisplayName("getWalletById throws UnauthorizedAccessException when accessed by non-owner")
    void getWalletByIdShouldRejectNonOwner() {
        Wallet wallet = new Wallet(alice, "USD", new BigDecimal("50.0000"));
        wallet.setId(10L);
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        assertThrows(UnauthorizedAccessException.class, () -> walletService.getWalletById(2L, 10L));
    }

    @Test
    @DisplayName("getWalletById throws WalletNotFoundException when wallet does not exist")
    void getWalletByIdShouldThrowWhenNotFound() {
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletById(1L, 999L));
    }

    @Test
    @DisplayName("getUserWallets returns only the authenticated user's wallets")
    void getUserWalletsShouldReturnOnlyUserWallets() {
        Wallet w1 = new Wallet(alice, "USD", BigDecimal.ZERO);
        w1.setId(11L);
        Wallet w2 = new Wallet(alice, "EUR", BigDecimal.ZERO);
        w2.setId(12L);

        when(walletRepository.findByOwnerId(1L)).thenReturn(List.of(w1, w2));

        List<WalletResponse> wallets = walletService.getUserWallets(1L);
        assertEquals(2, wallets.size());
        assertEquals("USD", wallets.get(0).currency());
        assertEquals("EUR", wallets.get(1).currency());
    }

    @Test
    @DisplayName("depositDemoFunds updates wallet balance, persists transfer, and persists double-entry ledger records")
    void depositDemoFundsShouldSucceed() {
        Wallet userWallet = new Wallet(alice, "USD", new BigDecimal("50.0000"));
        userWallet.setId(10L);

        User faucetUser = new User("Demo Treasury Faucet", "demo.faucet@system.local", "dummy");
        faucetUser.setId(999L);
        Wallet faucetWallet = new Wallet(faucetUser, "USD", new BigDecimal("1000000.0000"));
        faucetWallet.setId(99L);

        when(walletRepository.findById(10L)).thenReturn(Optional.of(userWallet));
        when(userRepository.findByEmail("demo.faucet@system.local")).thenReturn(Optional.of(faucetUser));
        when(walletRepository.findByOwnerIdAndCurrency(999L, "USD")).thenReturn(Optional.of(faucetWallet));
        when(transferRepository.findBySenderWalletIdAndIdempotencyKey(99L, "test-dep-1")).thenReturn(Optional.empty());

        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(userWallet));
        when(walletRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(faucetWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(i -> {
            Transfer t = i.getArgument(0);
            t.setId(1001L);
            return t;
        });

        DemoDepositRequest request = new DemoDepositRequest(new BigDecimal("100.0000"), "test-dep-1");
        WalletResponse response = walletService.depositDemoFunds(1L, 10L, request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(new BigDecimal("150.0000"), response.balance());

        verify(transferRepository).save(any(Transfer.class));
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("depositDemoFunds rejects unauthenticated or unauthorized users")
    void depositDemoFundsShouldRejectUnauthorized() {
        Wallet userWallet = new Wallet(alice, "USD", new BigDecimal("50.0000"));
        userWallet.setId(10L);

        when(walletRepository.findById(10L)).thenReturn(Optional.of(userWallet));

        DemoDepositRequest request = new DemoDepositRequest(new BigDecimal("100.0000"), "test-dep-2");

        // Bob attempting to deposit into Alice's wallet
        assertThrows(UnauthorizedAccessException.class,
                () -> walletService.depositDemoFunds(2L, 10L, request));
    }

    @Test
    @DisplayName("depositDemoFunds throws WalletNotFoundException when destination wallet does not exist")
    void depositDemoFundsShouldThrowWhenWalletNotFound() {
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());

        DemoDepositRequest request = new DemoDepositRequest(new BigDecimal("100.0000"), "test-dep-3");

        assertThrows(WalletNotFoundException.class,
                () -> walletService.depositDemoFunds(1L, 999L, request));
    }

    @Test
    @DisplayName("depositDemoFunds rejects negative or zero amounts")
    void depositDemoFundsShouldRejectInvalidAmount() {
        assertThrows(InvalidTransferAmountException.class,
                () -> walletService.depositDemoFunds(1L, 10L, new DemoDepositRequest(BigDecimal.ZERO, "k1")));

        assertThrows(InvalidTransferAmountException.class,
                () -> walletService.depositDemoFunds(1L, 10L, new DemoDepositRequest(new BigDecimal("-10.00"), "k2")));
    }
}
