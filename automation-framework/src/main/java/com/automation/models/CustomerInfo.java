package com.automation.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CustomerInfo {
    private String email;
    private String name;
    private String address1;
    private String address2;
    private String phone;

    public CustomerInfo() {
    }

    public CustomerInfo(String email, String name, String address1, String address2, String phone) {
        this.email = email;
        this.name = name;
        this.address1 = address1;
        this.address2 = address2;
        this.phone = phone;
    }
}
