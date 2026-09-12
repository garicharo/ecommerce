package com.garicharo.shop.commerce;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class FakePaymentClient implements PaymentClient {

    @Override
    public PaymentOutcome charge(BigDecimal total) {
        return PaymentOutcome.APPROVED;
    }
}
