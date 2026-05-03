package com.kjt.lms.controller;

import com.kjt.lms.config.VnPayConfig;
import com.kjt.lms.model.response.order.OrderResponseDto;
import com.kjt.lms.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "VNPay Payment", description = "Public VNPay gateway callbacks")
public class VnPayPaymentController {

    private final PaymentService paymentService;
    private final VnPayConfig vnPayConfig;

    @GetMapping("/return")
    @Operation(summary = "Handle VNPay browser return callback")
    public ResponseEntity<Void> handleReturn(@RequestParam Map<String, String> params) {
        try {
            OrderResponseDto response = paymentService.handleVnPayReturn(params);
            return redirectToFrontend(buildFrontendReturnUri(params, response, null));
        } catch (Exception ex) {
            log.error("VNPay browser return processing error", ex);
            return redirectToFrontend(buildFrontendReturnUri(params, null, ex.getMessage()));
        }
    }

    @GetMapping("/ipn")
    @Operation(summary = "Handle VNPay IPN callback")
    public ResponseEntity<Map<String, String>> handleIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleVnPayIpn(params));
    }

    private ResponseEntity<Void> redirectToFrontend(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(uri);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private URI buildFrontendReturnUri(Map<String, String> params, OrderResponseDto order, String errorMessage) {
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean gatewaySuccess = "00".equals(responseCode) && "00".equals(transactionStatus);
        boolean success = gatewaySuccess && order != null && order.getStatus() != null && "COMPLETED".equals(order.getStatus().name());
        String result = resolvePaymentResult(responseCode, success);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(vnPayConfig.getFrontendReturnUrl())
                .queryParam("success", success)
                .queryParam("result", result)
                .queryParam("paymentMethod", "VNPAY");

        addQueryParamIfPresent(builder, "orderId", order != null && order.getId() != null ? order.getId().toString() : null);
        addQueryParamIfPresent(builder, "orderCode", order != null ? order.getOrderCode() : null);
        addQueryParamIfPresent(builder, "status", order != null && order.getStatus() != null ? order.getStatus().name() : null);
        addQueryParamIfPresent(builder, "transactionId", order != null ? order.getTransactionId() : params.get("vnp_TxnRef"));
        addQueryParamIfPresent(builder, "responseCode", responseCode);
        addQueryParamIfPresent(builder, "transactionStatus", transactionStatus);
        addQueryParamIfPresent(builder, "message", errorMessage);

        return builder.build().encode().toUri();
    }

    private String resolvePaymentResult(String responseCode, boolean success) {
        if (success) {
            return "success";
        }
        if ("24".equals(responseCode)) {
            return "cancelled";
        }
        return "failed";
    }

    private void addQueryParamIfPresent(UriComponentsBuilder builder, String name, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(name, value);
        }
    }
}
