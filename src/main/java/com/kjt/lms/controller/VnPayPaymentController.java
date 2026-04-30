package com.kjt.lms.controller;

import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.response.APIResponse;
import com.kjt.lms.model.response.order.OrderResponseDto;
import com.kjt.lms.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
@Tag(name = "VNPay Payment", description = "Public VNPay gateway callbacks")
public class VnPayPaymentController {

    private final PaymentService paymentService;
    private final MessageProvider messageProvider;

    @GetMapping("/return")
    @Operation(summary = "Handle VNPay browser return callback")
    public ResponseEntity<APIResponse<OrderResponseDto>> handleReturn(@RequestParam Map<String, String> params) {
        OrderResponseDto response = paymentService.handleVnPayReturn(params);
        return ResponseEntity.ok(APIResponse.success(response, messageProvider.getMessage("payment.vnpay.return.success")));
    }

    @GetMapping("/ipn")
    @Operation(summary = "Handle VNPay IPN callback")
    public ResponseEntity<Map<String, String>> handleIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentService.handleVnPayIpn(params));
    }
}
