package com.automation.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class OrderTotals {
    private BigDecimal subtotal;
    private BigDecimal shipping;
    private BigDecimal tax;
    private BigDecimal tip;
    private BigDecimal discount;
    private BigDecimal total;

    private String subtotalText;
    private String shippingText;
    private String taxText;
    private String tipText;
    private String discountText;
    private String totalText;

    public OrderTotals() {
    }

    public OrderTotals(BigDecimal subtotal, BigDecimal shipping, BigDecimal tax, BigDecimal tip,
                       BigDecimal discount, BigDecimal total) {
        this.subtotal = subtotal;
        this.shipping = shipping;
        this.tax = tax;
        this.tip = tip;
        this.discount = discount;
        this.total = total;
    }
}
