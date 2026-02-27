package com.automation.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class OrderItemDetail {
    private String title;
    private int qty;
    private BigDecimal unitPrice;
    private BigDecimal linePrice;
    private String unitPriceText;
    private String linePriceText;

    public OrderItemDetail() {
    }

    public OrderItemDetail(String title, int qty, BigDecimal unitPrice, BigDecimal linePrice,
                           String unitPriceText, String linePriceText) {
        this.title = title;
        this.qty = qty;
        this.unitPrice = unitPrice;
        this.linePrice = linePrice;
        this.unitPriceText = unitPriceText;
        this.linePriceText = linePriceText;
    }
}
