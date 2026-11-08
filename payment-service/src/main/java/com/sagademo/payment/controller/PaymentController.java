package com.sagademo.payment.controller;

import com.sagademo.payment.dto.PaymentResponse;
import com.sagademo.payment.dto.ProcessPaymentRequest;
import com.sagademo.payment.dto.RefundPaymentRequest;
import com.sagademo.payment.service.PaymentApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Thin controller: HTTP in, DTO out, all logic in {@link PaymentApplicationService}. */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentApplicationService paymentService;

    /** Saga step 3 (PROCESS_PAYMENT) - local transaction T3. */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> process(@Valid @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.processPayment(request)));
    }

    /** COMPENSATION for step 3 (REFUND_PAYMENT) - compensating transaction C3. */
    @PostMapping("/refund")
    public ResponseEntity<PaymentResponse> refund(@Valid @RequestBody RefundPaymentRequest request) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.refundPayment(request)));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> listPayments() {
        return ResponseEntity.ok(
                paymentService.findAllPayments().stream().map(PaymentResponse::from).toList());
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.findPayment(paymentId)));
    }

    /** Demo helper: clears the Payment ledger between presentation runs. */
    @DeleteMapping("/reset")
    public ResponseEntity<Void> reset() {
        paymentService.resetAllData();
        return ResponseEntity.noContent().build();
    }
}
