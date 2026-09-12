package com.edu.order.dto;

import lombok.*;

@Data @AllArgsConstructor
public class PaymentFormResponse {
    private String orderNo;
    private String mode;
    private String paymentForm;
    private String paymentUrl;
}
