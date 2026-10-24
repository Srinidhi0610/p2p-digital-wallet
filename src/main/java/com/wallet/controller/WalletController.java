package com.wallet.controller;

import com.wallet.dto.CreateWalletRequest;
import com.wallet.dto.DemoDepositRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.exception.InvalidCredentialsException;
import com.wallet.security.UserPrincipal;
import com.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateWalletRequest request) {
        if (principal == null) {
            throw new InvalidCredentialsException("Unauthenticated request");
        }
        WalletResponse response = walletService.createWallet(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getWallets(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("Unauthenticated request");
        }
        List<WalletResponse> wallets = walletService.getUserWallets(principal.getId());
        return ResponseEntity.ok(wallets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponse> getWallet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        if (principal == null) {
            throw new InvalidCredentialsException("Unauthenticated request");
        }
        WalletResponse response = walletService.getWalletById(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<WalletResponse> depositDemoFunds(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody DemoDepositRequest request) {
        if (principal == null) {
            throw new InvalidCredentialsException("Unauthenticated request");
        }
        WalletResponse response = walletService.depositDemoFunds(principal.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/demo-deposit")
    public ResponseEntity<WalletResponse> demoDeposit(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody DemoDepositRequest request) {
        return depositDemoFunds(principal, id, request);
    }
}
