package com.garicharo.shop.commerce;

import java.math.BigDecimal;

public interface PaymentClient {

    PaymentOutcome charge(BigDecimal total);
}
