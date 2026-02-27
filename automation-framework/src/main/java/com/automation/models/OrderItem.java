package com.automation.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class OrderItem {
    private String title;
    private int qty;
    private BigDecimal price;
    private String priceText;

    public OrderItem() {
    }

    public OrderItem(String title, int qty, BigDecimal price, String priceText) {
        this.title = title;
        this.qty = qty;
        this.price = price;
        this.priceText = priceText;
    }
}
