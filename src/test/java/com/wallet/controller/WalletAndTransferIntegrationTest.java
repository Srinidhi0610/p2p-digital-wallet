package com.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.CreateWalletRequest;
import com.wallet.dto.TransferRequest;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransferRepository;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import com.wallet.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WalletAndTransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User alice;
    private User bob;
    private User charlie;

    private String aliceToken;
    private String bobToken;
    private String charlieToken;

    private Wallet aliceUsdWallet;
    private Wallet bobUsdWallet;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        transferRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(new User("Alice", "alice.api@example.com", passwordEncoder.encode("Password123!")));
        bob = userRepository.save(new User("Bob", "bob.api@example.com", passwordEncoder.encode("Password123!")));
        charlie = userRepository.save(new User("Charlie", "charlie.api@example.com", passwordEncoder.encode("Password123!")));

        aliceToken = "Bearer " + jwtService.generateToken(alice);
        bobToken = "Bearer " + jwtService.generateToken(bob);
        charlieToken = "Bearer " + jwtService.generateToken(charlie);

        aliceUsdWallet = walletRepository.save(new Wallet(alice, "USD", new BigDecimal("500.0000")));
        bobUsdWallet = walletRepository.save(new Wallet(bob, "USD", new BigDecimal("100.0000")));
    }

    // =========================================================================
    // Authentication & Unauthenticated Access Checks
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated access to wallets or transfers endpoints returns 401 Unauthorized")
    void unauthenticatedEndpointsReturn401() throws Exception {
        mockMvc.perform(get("/api/wallets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"EUR\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/transfers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderWalletId\":1,\"recipientWalletId\":2,\"amount\":10,\"currency\":\"USD\",\"idempotencyKey\":\"k1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid or malformed JWT returns 401 Unauthorized")
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/wallets")
                        .header("Authorization", "Bearer invalid-or-malformed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // Wallet API Tests
    // =========================================================================

    @Test
    @DisplayName("Authenticated wallet creation creates wallet with 0 starting balance for authenticated user")
    void createWalletEnforcesZeroBalanceAndAuthenticatedUser() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("EUR");

        mockMvc.perform(post("/api/wallets")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(alice.getId()))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance").value(0.0000))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Client cannot set initial balance or owner ID when creating wallet")
    void clientCannotInjectBalanceOrOwnerId() throws Exception {
        // Attempting to send custom fields in JSON: balance and userId
        Map<String, Object> maliciousPayload = Map.of(
                "currency", "GBP",
                "balance", 10000.00,
                "userId", bob.getId()
        );

        mockMvc.perform(post("/api/wallets")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(alice.getId())) // Enforced from token
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.balance").value(0.0000)); // Enforced to strictly 0
    }

    @Test
    @DisplayName("Duplicate wallet currency for the same user is rejected with 409 Conflict")
    void duplicateWalletCurrencyRejected() throws Exception {
        // Alice already has a USD wallet from setUp()
        CreateWalletRequest duplicateRequest = new CreateWalletRequest("USD");

        mockMvc.perform(post("/api/wallets")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @DisplayName("Invalid currency codes are rejected with 400 Bad Request")
    void invalidCurrencyRejected() throws Exception {
        mockMvc.perform(post("/api/wallets")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"US\"}")) // 2 chars
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/wallets")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"123\"}")) // digits
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/wallets returns only the authenticated user's wallets")
    void getWalletsReturnsOnlyUserWallets() throws Exception {
        // Alice has 1 USD wallet
        mockMvc.perform(get("/api/wallets")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(aliceUsdWallet.getId()))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].userId").value(alice.getId()));

        // Charlie has 0 wallets
        mockMvc.perform(get("/api/wallets")
                        .header("Authorization", charlieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/wallets/{id} allows owner to view wallet details")
    void getWalletByIdAllowsOwner() throws Exception {
        mockMvc.perform(get("/api/wallets/" + aliceUsdWallet.getId())
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceUsdWallet.getId()))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(500.0000));
    }

    @Test
    @DisplayName("GET /api/wallets/{id} rejects non-owner with 403 Forbidden (preventing IDOR)")
    void getWalletByIdRejectsNonOwnerWith403() throws Exception {
        mockMvc.perform(get("/api/wallets/" + aliceUsdWallet.getId())
                        .header("Authorization", bobToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("permission")));
    }

    @Test
    @DisplayName("GET /api/wallets/{id} returns 404 Not Found for non-existent wallet")
    void getWalletByIdReturns404ForMissingWallet() throws Exception {
        mockMvc.perform(get("/api/wallets/999999")
                        .header("Authorization", aliceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // Transfer API Tests
    // =========================================================================

    @Test
    @DisplayName("Authenticated wallet owner can transfer funds successfully")
    void transferFundsSuccessfully() throws Exception {
        TransferRequest request = new TransferRequest(
                aliceUsdWallet.getId(),
                bobUsdWallet.getId(),
                new BigDecimal("100.0000"),
                "USD",
                "tx-http-success-1"
        );

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.senderWalletId").value(aliceUsdWallet.getId()))
                .andExpect(jsonPath("$.recipientWalletId").value(bobUsdWallet.getId()))
                .andExpect(jsonPath("$.amount").value(100.0000))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Verify balances in database
        Wallet updatedAlice = walletRepository.findById(aliceUsdWallet.getId()).orElseThrow();
        Wallet updatedBob = walletRepository.findById(bobUsdWallet.getId()).orElseThrow();
        assertEquals(new BigDecimal("400.0000"), updatedAlice.getBalance());
        assertEquals(new BigDecimal("200.0000"), updatedBob.getBalance());
    }

    @Test
    @DisplayName("User cannot initiate transfer from another user's wallet (403 Forbidden)")
    void transferFromUnownedWalletRejectedWith403() throws Exception {
        // Bob attempts to send money from Alice's wallet
        TransferRequest fraudRequest = new TransferRequest(
                aliceUsdWallet.getId(),
                bobUsdWallet.getId(),
                new BigDecimal("50.0000"),
                "USD",
                "tx-fraud-1"
        );

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", bobToken) // Bob is authenticated, but does NOT own aliceUsdWallet
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fraudRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("permission")));

        // Alice's balance must be unchanged
        Wallet unchangedAlice = walletRepository.findById(aliceUsdWallet.getId()).orElseThrow();
        assertEquals(new BigDecimal("500.0000"), unchangedAlice.getBalance());
    }

    @Test
    @DisplayName("Transfer with insufficient balance returns 400 Bad Request")
    void transferWithInsufficientFundsReturns400() throws Exception {
        TransferRequest request = new TransferRequest(
                aliceUsdWallet.getId(),
                bobUsdWallet.getId(),
                new BigDecimal("9000.0000"), // Exceeds 500
                "USD",
                "tx-insufficient-1"
        );

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Insufficient balance")));
    }

    @Test
    @DisplayName("Self-transfer to same wallet returns 400 Bad Request")
    void selfTransferReturns400() throws Exception {
        TransferRequest request = new TransferRequest(
                aliceUsdWallet.getId(),
                aliceUsdWallet.getId(),
                new BigDecimal("10.0000"),
                "USD",
                "tx-self-1"
        );

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Repeated transfer with same idempotency key returns original transfer without duplicate debit")
    void idempotencyReplayReturnsOriginalTransfer() throws Exception {
        TransferRequest request = new TransferRequest(
                aliceUsdWallet.getId(),
                bobUsdWallet.getId(),
                new BigDecimal("50.0000"),
                "USD",
                "tx-idem-repeat-1"
        );

        // First attempt
        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Repeated attempt
        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value("tx-idem-repeat-1"));

        // Deducted only once
        Wallet aliceFinal = walletRepository.findById(aliceUsdWallet.getId()).orElseThrow();
        assertEquals(new BigDecimal("450.0000"), aliceFinal.getBalance());
        assertEquals(1, transferRepository.count());
    }

    @Test
    @DisplayName("Reusing idempotency key with different payload returns 409 Conflict")
    void reusingIdempotencyKeyDifferentPayloadReturns409() throws Exception {
        TransferRequest original = new TransferRequest(
                aliceUsdWallet.getId(),
                bobUsdWallet.getId(),
                new BigDecimal("50.0000"),
                "USD",
                "tx-idem-conflict-1"
        );

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(original)))
                .andExpect(status().isOk());

        // Different amount with same key
        TransferRequest tampered = new TransferRequest(
                aliceUsdWallet.getId(),
                bobUsdWallet.getId(),
                new BigDecimal("75.0000"),
                "USD",
                "tx-idem-conflict-1"
        );

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tampered)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/transfers/{id} allows sender and recipient to view, rejects third party with 403")
    void getTransferByIdAllowsParticipantsRejectsThirdParty() throws Exception {
        TransferRequest request = new TransferRequest(
                aliceUsdWallet.getId(),
                bobUsdWallet.getId(),
                new BigDecimal("25.0000"),
                "USD",
                "tx-view-1"
        );

        String transferJson = mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long transferId = objectMapper.readTree(transferJson).get("id").asLong();

        // 1. Sender (Alice) can view
        mockMvc.perform(get("/api/transfers/" + transferId)
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId));

        // 2. Recipient (Bob) can view
        mockMvc.perform(get("/api/transfers/" + transferId)
                        .header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId));

        // 3. Third party (Charlie) cannot view (403 Forbidden)
        mockMvc.perform(get("/api/transfers/" + transferId)
                        .header("Authorization", charlieToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("permission")));
    }

    @Test
    @DisplayName("GET /api/transfers returns paginated transfers involving only the user's wallets")
    void getTransfersReturnsPaginatedUserTransfers() throws Exception {
        // Alice transfers to Bob
        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest(
                                aliceUsdWallet.getId(), bobUsdWallet.getId(), new BigDecimal("10.0000"), "USD", "tx-p-1"))))
                .andExpect(status().isOk());

        // Alice transfers to Bob again
        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest(
                                aliceUsdWallet.getId(), bobUsdWallet.getId(), new BigDecimal("20.0000"), "USD", "tx-p-2"))))
                .andExpect(status().isOk());

        // Alice views transfers
        mockMvc.perform(get("/api/transfers?page=0&size=10")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        // Bob also views transfers (recipient in both)
        mockMvc.perform(get("/api/transfers?page=0&size=10")
                        .header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        // Charlie has no involvement in any transfer
        mockMvc.perform(get("/api/transfers")
                        .header("Authorization", charlieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
