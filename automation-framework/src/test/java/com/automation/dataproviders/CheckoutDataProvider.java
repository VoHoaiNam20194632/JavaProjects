package com.automation.dataproviders;

import org.testng.annotations.DataProvider;

import java.util.HashMap;
import java.util.Map;

public class CheckoutDataProvider {

    @DataProvider(name = "defaultCheckoutData")
    public static Object[][] defaultCheckoutData() {
        Map<String, String> shippingData = createDefaultShippingData();
        return new Object[][]{{shippingData}};
    }

    @DataProvider(name = "creditCardCheckoutData")
    public static Object[][] creditCardCheckoutData() {
        Map<String, String> shippingData = createDefaultShippingData();
        shippingData.put("cardNumber", "4242424242424242");
        shippingData.put("cardExpiry", "12/28");
        shippingData.put("cardCvc", "123");
        return new Object[][]{{shippingData}};
    }

    @DataProvider(name = "checkoutWithTipData")
    public static Object[][] checkoutWithTipData() {
        Map<String, String> shippingData = createDefaultShippingData();
        shippingData.put("tipAmount", "5.00");
        return new Object[][]{{shippingData}};
    }

    @DataProvider(name = "checkoutWithCouponData")
    public static Object[][] checkoutWithCouponData() {
        Map<String, String> shippingData = createDefaultShippingData();
        shippingData.put("couponCode", "TESTCOUPON");
        return new Object[][]{{shippingData}};
    }

    private static Map<String, String> createDefaultShippingData() {
        Map<String, String> data = new HashMap<>();
        data.put("fullName", "Test Automation User");
        data.put("email", "testbuyer_" + System.currentTimeMillis() + "@mailinator.com");
        data.put("phone", "0866843576");
        data.put("country", "United States");
        data.put("state", "CA - California");
        data.put("city", "Los Angeles");
        data.put("address", "123 Test Street, Suite 100");
        data.put("zipCode", "90001");
        return data;
    }
}
