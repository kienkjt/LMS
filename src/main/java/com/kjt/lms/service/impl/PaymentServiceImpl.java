package com.kjt.lms.service.impl;

import com.kjt.lms.common.constants.OrderStatusEnum;
import com.kjt.lms.common.constants.PaymentMethodEnum;
import com.kjt.lms.common.i18n.MessageProvider;
import com.kjt.lms.common.security.SecurityUtils;
import com.kjt.lms.config.VnPayConfig;
import com.kjt.lms.exception.BusinessException;
import com.kjt.lms.exception.ResourceNotFoundException;
import com.kjt.lms.mapper.OrderMapper;
import com.kjt.lms.model.entity.EnrollmentEntity;
import com.kjt.lms.model.entity.OrderEntity;
import com.kjt.lms.model.entity.OrderItemEntity;
import com.kjt.lms.model.request.order.InitPaymentRequestDto;
import com.kjt.lms.model.request.order.PayOrderRequestDto;
import com.kjt.lms.model.response.order.OrderResponseDto;
import com.kjt.lms.repository.EnrollmentRepository;
import com.kjt.lms.repository.OrderItemRepository;
import com.kjt.lms.repository.OrderRepository;
import com.kjt.lms.service.PaymentService;
import com.kjt.lms.service.WithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String VNPAY_TX_PREFIX = "VNPAY-";
    private static final String MOMO_TX_PREFIX = "MOMO-";
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SecurityUtils securityUtils;
    private final MessageProvider messageProvider;
    private final OrderMapper orderMapper;
    private final VnPayConfig vnPayConfig;
    private final WithdrawalService withdrawalService;

    @Override
    @Transactional
    public OrderResponseDto initPayment(UUID orderId, InitPaymentRequestDto request, HttpServletRequest httpRequest) {
        OrderEntity order = getAccessibleOrder(orderId);

        if (order.getStatus() != OrderStatusEnum.PENDING) {
            throw new BusinessException(messageProvider.getMessage("exception.order.notPayable"));
        }

        if (request.getPaymentMethod() == PaymentMethodEnum.VNPAY) {
            String txnRef = buildVnPayTxnRef(order.getId());
            String paymentUrl = buildVnPayPaymentUrl(order, request, httpRequest, txnRef);

            order.setPaymentMethod(PaymentMethodEnum.VNPAY);
            order.setTransactionId(txnRef);
            orderRepository.save(order);

            OrderResponseDto response = mapToDto(order);
            response.setPaymentUrl(paymentUrl);
            return response;
        }

        if (request.getPaymentMethod() == PaymentMethodEnum.MOMO) {
            OrderResponseDto response = mapToDto(order);
            response.setPaymentUrl("momo://pay?orderId=" + order.getId());
            return response;
        }

        throw new BusinessException(messageProvider.getMessage("exception.payment.method.unsupported"));
    }

    @Override
    @Transactional
    public OrderResponseDto payOrder(UUID orderId, PayOrderRequestDto request) {
        OrderEntity order = getAccessibleOrder(orderId);
        PaymentMethodEnum requestedMethod = request.getPaymentMethod();

        if (order.getPaymentMethod() == PaymentMethodEnum.VNPAY) {
            throw new BusinessException(messageProvider.getMessage("exception.payment.vnpay.useInit"));
        }

        if (order.getPaymentMethod() != null && order.getPaymentMethod() != requestedMethod) {
            throw new BusinessException(messageProvider.getMessage("exception.payment.method.mismatch"));
        }

        if (order.getStatus() == OrderStatusEnum.COMPLETED) {
            String gatewayTransactionId = normalizeTransactionId(request.getTransactionId(), requestedMethod);
            if (order.getTransactionId() != null && !order.getTransactionId().equals(gatewayTransactionId)) {
                throw new BusinessException(messageProvider.getMessage("exception.payment.transaction.mismatch"));
            }
            return mapToDto(order);
        }

        if (order.getStatus() != OrderStatusEnum.PENDING) {
            throw new BusinessException(messageProvider.getMessage("exception.order.notPayable"));
        }

        return switch (requestedMethod) {
            case VNPAY -> throw new BusinessException(messageProvider.getMessage("exception.payment.vnpay.useInit"));
            case MOMO -> processMomo(order, request.getTransactionId());
        };
    }

    @Override
    @Transactional
    public OrderResponseDto handleVnPayReturn(Map<String, String> queryParams) {
        validateVnPaySignature(queryParams);

        String txnRef = queryParams.get("vnp_TxnRef");
        String responseCode = queryParams.get("vnp_ResponseCode");
        OrderEntity order = findOrderByTransactionId(txnRef);

        if ("00".equals(responseCode)) {
            if (order.getStatus() == OrderStatusEnum.PENDING) {
                completePayment(order, PaymentMethodEnum.VNPAY, txnRef);
            }
            return mapToDto(order);
        }

        log.warn("VNPay return failed for transaction {} with code {}", txnRef, responseCode);
        return mapToDto(order);
    }

    @Override
    @Transactional
    public Map<String, String> handleVnPayIpn(Map<String, String> queryParams) {
        try {
            validateVnPaySignature(queryParams);
            String txnRef = queryParams.get("vnp_TxnRef");
            String responseCode = queryParams.get("vnp_ResponseCode");
            OrderEntity order = findOrderByTransactionId(txnRef);

            if ("00".equals(responseCode) && order.getStatus() == OrderStatusEnum.PENDING) {
                completePayment(order, PaymentMethodEnum.VNPAY, txnRef);
            }

            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } catch (ResourceNotFoundException ex) {
            return Map.of("RspCode", "01", "Message", "Order not found");
        } catch (BusinessException ex) {
            return Map.of("RspCode", "02", "Message", "Invalid status");
        } catch (Exception ex) {
            log.error("VNPay IPN processing error", ex);
            return Map.of("RspCode", "99", "Message", "Unknown error");
        }
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(UUID orderId) {
        OrderEntity order = getAccessibleOrder(orderId);

        if (order.getStatus() != OrderStatusEnum.PENDING) {
            throw new BusinessException(messageProvider.getMessage("exception.order.notCancellable"));
        }

        order.setStatus(OrderStatusEnum.CANCELLED);
        OrderEntity savedOrder = orderRepository.save(order);

        log.info("Order {} cancelled", orderId);
        return mapToDto(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponseDto refundOrder(UUID orderId) {
        OrderEntity order = getAccessibleOrder(orderId);

        if (order.getStatus() != OrderStatusEnum.COMPLETED) {
            throw new BusinessException(messageProvider.getMessage("exception.order.notRefundable"));
        }

        order.setStatus(OrderStatusEnum.REFUNDED);
        revokeEnrollmentsByOrder(order);

        // Reverse instructor earnings immediately and persist an audit record.
        withdrawalService.processRefundAdjustment(orderId);

        OrderEntity savedOrder = orderRepository.save(order);

        log.info("Order {} refunded and instructor earnings reversed", orderId);
        return mapToDto(savedOrder);
    }

    private OrderResponseDto processMomo(OrderEntity order, String transactionId) {
        return completePayment(order, PaymentMethodEnum.MOMO, normalizeTransactionId(transactionId, PaymentMethodEnum.MOMO));
    }

    private OrderResponseDto completePayment(OrderEntity order, PaymentMethodEnum paymentMethod, String transactionId) {
        order.setStatus(OrderStatusEnum.COMPLETED);
        order.setPaymentMethod(paymentMethod);
        order.setTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());

        OrderEntity savedOrder = orderRepository.save(order);
        createEnrollmentsForOrder(savedOrder);

        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(order.getId());
        for (OrderItemEntity item : orderItems) {
            if (item.getInstructorId() != null && item.getInstructorRevenue() != null) {
                withdrawalService.addEarnings(item.getInstructorId(), item.getInstructorRevenue());
            }
        }

        log.info("Order {} paid successfully via {}", order.getId(), paymentMethod);
        return mapToDto(savedOrder);
    }

    private String buildVnPayPaymentUrl(OrderEntity order,
                                        InitPaymentRequestDto request,
                                        HttpServletRequest httpRequest,
                                        String txnRef) {
        LocalDateTime nowGmt7 = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(toVnPayAmount(order.getFinalAmount())));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", normalizeLanguage(request.getLanguage()));
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", resolveClientIp(httpRequest));
        vnpParams.put("vnp_CreateDate", nowGmt7.format(VNPAY_DATE_FORMAT));
        vnpParams.put("vnp_ExpireDate", nowGmt7.plusMinutes(vnPayConfig.getExpireMinutes()).format(VNPAY_DATE_FORMAT));

        if (request.getBankCode() != null && !request.getBankCode().isBlank()) {
            vnpParams.put("vnp_BankCode", request.getBankCode().trim());
        }

        Map<String, String> sortedParams = new java.util.TreeMap<>(vnpParams);
        String hashData = buildEncodedParamString(sortedParams);
        String secureHash = hmacSha512(vnPayConfig.getHashSecret(), hashData);

        return vnPayConfig.getPaymentUrl()
                + "?"
                + hashData
                + "&vnp_SecureHash="
                + secureHash;
    }

    private long toVnPayAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100L)).longValue();
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return vnPayConfig.getLocale();
        }
        return language.trim();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private void validateVnPaySignature(Map<String, String> queryParams) {
        String secureHash = queryParams.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            throw new BusinessException(messageProvider.getMessage("exception.payment.vnpay.invalidChecksum"));
        }

        Map<String, String> filtered = queryParams.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .filter(e -> !"vnp_SecureHash".equals(e.getKey()))
                .filter(e -> !"vnp_SecureHashType".equals(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String signedData = buildEncodedParamString(new java.util.TreeMap<>(filtered));
        String localHash = hmacSha512(vnPayConfig.getHashSecret(), signedData);

        if (!localHash.equalsIgnoreCase(secureHash)) {
            throw new BusinessException(messageProvider.getMessage("exception.payment.vnpay.invalidChecksum"));
        }
    }

    private String buildEncodedParamString(Map<String, String> params) {
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            pairs.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return String.join("&", pairs);
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (Exception ex) {
            log.error("Failed to generate HMAC SHA512", ex);
            throw new BusinessException(messageProvider.getMessage("exception.payment.hmacGenerationFailed"));
        }
    }

    private String buildVnPayTxnRef(UUID orderId) {
        return VNPAY_TX_PREFIX + orderId.toString().replace("-", "").substring(0, 20);
    }

    private OrderEntity findOrderByTransactionId(String txnRef) {
        return orderRepository.findByTransactionIdAndDeletedFalse(txnRef)
                .orElseThrow(() -> new ResourceNotFoundException(messageProvider.getMessage("exception.payment.transaction.notFound")));
    }

    private String normalizeTransactionId(String transactionId, PaymentMethodEnum paymentMethod) {
        String trimmed = transactionId == null ? "" : transactionId.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(messageProvider.getMessage("validation.payment.transactionId.notBlank"));
        }

        String prefix = switch (paymentMethod) {
            case VNPAY -> VNPAY_TX_PREFIX;
            case MOMO -> MOMO_TX_PREFIX;
        };

        if (trimmed.toUpperCase().startsWith(prefix)) {
            return trimmed;
        }

        return prefix + trimmed;
    }

    private OrderEntity getAccessibleOrder(UUID orderId) {
        OrderEntity order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageProvider.getMessage("exception.order.notFound")));

        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!securityUtils.isAdmin() && !order.getStudentId().equals(currentUserId)) {
            throw new BusinessException(messageProvider.getMessage("exception.order.accessDenied"));
        }

        return order;
    }

    private void createEnrollmentsForOrder(OrderEntity order) {
        List<OrderItemEntity> orderItems = orderItemRepository.findByOrderId(order.getId());

        for (OrderItemEntity item : orderItems) {
            boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseIdAndDeletedFalse(
                    order.getStudentId(), item.getCourseId());
            if (enrolled) {
                continue;
            }

            EnrollmentEntity enrollment = EnrollmentEntity.builder()
                    .studentId(order.getStudentId())
                    .courseId(item.getCourseId())
                    .orderId(order.getId())
                    .progressPercent(BigDecimal.ZERO)
                    .certificateIssued(false)
                    .build();
            enrollmentRepository.save(enrollment);
        }
    }

    private void revokeEnrollmentsByOrder(OrderEntity order) {
        List<EnrollmentEntity> enrollments = enrollmentRepository
                .findByStudentIdAndOrderIdAndDeletedFalse(order.getStudentId(), order.getId());

        if (enrollments.isEmpty()) {
            return;
        }

        enrollments.forEach(enrollment -> enrollment.setDeleted(true));
        enrollmentRepository.saveAll(enrollments);
    }

    private OrderResponseDto mapToDto(OrderEntity order) {
        List<OrderItemEntity> items = orderItemRepository.findByOrderId(order.getId());
        return orderMapper.toResponse(order, items);
    }
}
