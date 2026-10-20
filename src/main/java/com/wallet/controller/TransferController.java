package com.wallet.controller;

import com.wallet.dto.TransferRequest;
import com.wallet.dto.TransferResponse;
import com.wallet.exception.InvalidCredentialsException;
import com.wallet.security.UserPrincipal;
import com.wallet.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransferRequest request) {
        if (principal == null) {
            throw new InvalidCredentialsException("Unauthenticated request");
        }
        TransferResponse response = transferService.transfer(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransfer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        if (principal == null) {
            throw new InvalidCredentialsException("Unauthenticated request");
        }
        TransferResponse response = transferService.getTransferById(principal.getId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<TransferResponse>> getTransfers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (principal == null) {
            throw new InvalidCredentialsException("Unauthenticated request");
        }
        int effectivePage = Math.max(page, 0);
        int effectiveSize = (size <= 0 || size > 100) ? 20 : size;

        PageRequest pageRequest = PageRequest.of(
                effectivePage,
                effectiveSize,
                Sort.by(Sort.Direction.DESC, "createdAt", "id")
        );

        Page<TransferResponse> response = transferService.getUserTransfers(principal.getId(), pageRequest);
        return ResponseEntity.ok(response);
    }
}
