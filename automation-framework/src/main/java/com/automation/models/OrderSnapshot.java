package com.automation.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class OrderSnapshot {
    private String referenceOrder;
    private OrderTotals totals;
    private List<OrderItem> items;
    private CustomerInfo customer;
    private String shippingMethod;

    public OrderSnapshot() {
    }

    public OrderSnapshot(String referenceOrder, OrderTotals totals, List<OrderItem> items,
                         CustomerInfo customer, String shippingMethod) {
        this.referenceOrder = referenceOrder;
        this.totals = totals;
        this.items = items;
        this.customer = customer;
        this.shippingMethod = shippingMethod;
    }
}
