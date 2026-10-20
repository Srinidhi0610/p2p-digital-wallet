package com.wallet.service;

import com.wallet.dto.CreateWalletRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.exception.DuplicateWalletException;
import com.wallet.exception.UnauthorizedAccessException;
import com.wallet.exception.WalletNotFoundException;
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

    private WalletService walletService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, userRepository);

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
}
