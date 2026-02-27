package com.automation.pages.buyer.checkout;

import com.automation.enums.WaitStrategy;
import com.automation.models.*;
import com.automation.pages.BasePage;
import com.automation.utils.CurrencyUtils;
import com.automation.utils.PriceUtils;
import com.automation.utils.WaitUtils;
import com.google.gson.*;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.util.*;

public class ThankYouSection extends BasePage {

    private static final By ORDER_SUCCESS_MSG = By.xpath("//p[normalize-space(.)=\"Thank you for your order\"]");
    private static final By ORDER_LABEL = By.xpath("//div[contains(concat(' ',normalize-space(@class),' '),' order-label ')]");
    private static final By SUBTOTAL = By.xpath("//div[@id=\"order-summary\"]//div[@class=\"d-flex justify-content-between mb-3\"][1]//span[@class=\"price fw-bold\"]");
    private static final By SHIPPING_FEE = By.xpath("//div[@id=\"order-summary\"]//div[@class=\"d-flex justify-content-between mb-3\"][2]//span[@class=\"price fw-bold\"]");
    private static final By TOTAL_PRICE = By.xpath("//span[@class=\"price-value fw-bold\"]");

    @Step("Wait for order success message")
    public ThankYouSection waitForOrderSuccessMessage() {
        WaitUtils.isElementDisplayed(ORDER_SUCCESS_MSG, 25);
        return this;
    }

    @Step("Get order success total calculation")
    public Map<String, BigDecimal> getOrderSuccessTotalCalculation() {
        waitForUrlContains("thankyou", 20);
        String subTotal = getText(SUBTOTAL);
        String shipping = getText(SHIPPING_FEE);
        String totalPrice = getText(TOTAL_PRICE);
        String orderLabel = getText(ORDER_LABEL);

        BigDecimal expected = PriceUtils.roundPrice(PriceUtils.parsePrice(subTotal).add(PriceUtils.parsePrice(shipping)));
        BigDecimal actual = PriceUtils.parsePrice(totalPrice);

        setData("DATA_BUYER_PAGE_ORDER_SUCCESS_TEXT_ORDER_LABEL", orderLabel);

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("expected", expected);
        result.put("actual", actual);
        return result;
    }

    @Step("Get order success info")
    public Map<String, BigDecimal> getOrderSuccessInfo() {
        return getOrderSuccessTotalCalculation();
    }

    @Step("Validate currency symbol on thank you page")
    public Map<String, Object> getThankYouCurrencySymbolValidation(String expectedSymbol) {
        String total = getText(TOTAL_PRICE);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("expectedSymbol", expectedSymbol);
        if (total == null || total.trim().isEmpty()) {
            result.put("hasTotal", false);
            return result;
        }
        result.put("hasTotal", true);
        String token = CurrencyUtils.extractToken(total);
        result.put("token", token);
        result.put("matches", token.contains(expectedSymbol));
        return result;
    }

    @Step("Get full Thank You page info")
    public Map<String, Object> getThankYouPageInfo() {
        String referenceOrder = getText(ORDER_LABEL);
        Map<String, Object> result = new HashMap<>();
        List<String> missingFields = new ArrayList<>();

        if (referenceOrder == null || referenceOrder.trim().isEmpty()) {
            missingFields.add("referenceOrder");
        }
        result.put("referenceOrder", referenceOrder);

        // Customer info
        String email = getTextByXpath("//div[label[normalize-space()=\"Email:\"]]");
        String name = getTextByXpath("//div[label[normalize-space()=\"Name:\"]]");
        String address1 = getTextByXpath("//div[label[normalize-space()=\"Address:\"]]");
        String address2 = getTextByXpath("//div[label[normalize-space()=\"Address:\"]]/following::div[1]");
        String phone = getTextByXpath("//div[label[normalize-space()=\"Phone:\"]]");

        email = normalizeLabelValue(email);
        name = normalizeLabelValue(name);
        address1 = normalizeLabelValue(address1);
        phone = normalizeLabelValue(phone);

        // Items
        String itemsXpath = "//div[@id=\"order-summary\"]/div[contains(concat(' ',normalize-space(@class),' '),' row ')]//div[contains(concat(' ',normalize-space(@class),' '),' row ')]";
        List<WebElement> productRows = getElements(By.xpath(itemsXpath));

        // Totals
        String subtotalText = getTextByXpath("//div[contains(text(), 'Subtotal')]//span[contains(concat(' ',normalize-space(@class),' '),' price ')]");
        String shippingText = getTextByXpath("(//div[contains(text(), 'Shipping')]/following::span[contains(concat(' ',normalize-space(@class),' '),' price ')])[1]");
        String totalText = getTextByXpath("//span[contains(text(), 'Total')]/following::span[contains(concat(' ',normalize-space(@class),' '),' price-value ')]");
        String taxText = getTextByXpath("//div[contains(text(), 'Tax')]//span[contains(concat(' ',normalize-space(@class),' '),' price ')]");
        String tipText = getTextByXpath("//div[contains(text(), 'Tip')]//span[contains(concat(' ',normalize-space(@class),' '),' price ')]");
        String discountText = getTextByXpath("//div[contains(text(), 'Discount')]//span[contains(concat(' ',normalize-space(@class),' '),' price ')]");
        String shippingMethod = getTextByXpath("//div[contains(text(), 'Shipping')]//p");

        // Build JSON structure
        JsonObject orderInfo = new JsonObject();
        orderInfo.addProperty("referenceOrder", referenceOrder);

        JsonObject customer = new JsonObject();
        customer.addProperty("email", email);
        customer.addProperty("name", name);
        customer.addProperty("address1", address1);
        customer.addProperty("address2", address2 != null ? address2.trim() : "");
        customer.addProperty("phone", phone);
        orderInfo.add("customer", customer);

        JsonArray items = new JsonArray();
        for (WebElement row : productRows) {
            String title = getChildText(row, ".//a[contains(@class,'product-title')]");
            String qtyText = getChildText(row, ".//span");
            String priceText = getChildText(row, ".//b");
            priceText = priceText.replaceFirst("^\\d+\\s*x\\s*", "");
            JsonObject item = new JsonObject();
            item.addProperty("title", title);
            item.addProperty("qty", normalizeQuantity(qtyText));
            item.addProperty("price", priceText);
            items.add(item);
        }
        orderInfo.add("items", items);

        String refPrice = (subtotalText == null || subtotalText.trim().isEmpty()) ? totalText : subtotalText;
        JsonObject totals = new JsonObject();
        totals.addProperty("subtotal", subtotalText);
        totals.addProperty("shipping", PriceUtils.normalizeOptionalPrice(shippingText, refPrice));
        totals.addProperty("total", totalText);
        if (taxText != null && !taxText.isEmpty()) totals.addProperty("tax", PriceUtils.normalizeOptionalPrice(taxText, refPrice));
        if (tipText != null && !tipText.isEmpty()) totals.addProperty("tip", PriceUtils.normalizeOptionalPrice(tipText, refPrice));
        if (discountText != null && !discountText.isEmpty()) totals.addProperty("discount", PriceUtils.normalizeOptionalPrice(discountText, refPrice));
        orderInfo.add("totals", totals);
        orderInfo.addProperty("shippingMethod", shippingMethod);

        setData("BUYER_ORDER_SUCCESS_INFO", orderInfo.toString());
        setData("THANK_YOU_TOTAL", totalText);
        setData("REFERENCE_ORDER", referenceOrder);
        log.info("Order success info: {}", orderInfo);

        result.put("orderInfo", orderInfo.toString());
        result.put("missingFields", missingFields);
        result.put("allFieldsPresent", missingFields.isEmpty());
        return result;
    }

    @Step("Capture abandoned order ID from cookies")
    public void captureAbandonedOrderIdFromCookies() {
        String existing = getData("BUYER_ABANDONED_ORDER_ID");
        if (existing != null && !existing.trim().isEmpty()) return;

        String orderId = readOrderIdFromCookies();
        if (orderId != null && !orderId.trim().isEmpty()) {
            setData("BUYER_ABANDONED_ORDER_ID", orderId.trim());
            log.info("Stored abandoned order id: {}", orderId);
        }
    }

    @Step("Compare checkout vs thank you data")
    public Map<String, Object> getCheckoutAndThankYouVerification(Map<String, String> totalsMap, List<Map<String, String>> items) {
        Map<String, Object> result = new HashMap<>();
        List<String> missingData = new ArrayList<>();

        if (totalsMap == null || totalsMap.isEmpty()) missingData.add("checkoutTotals");
        if (items == null || items.isEmpty()) missingData.add("checkoutItems");

        String snapshotText = getData("BUYER_ORDER_SUCCESS_INFO");
        if (snapshotText == null || snapshotText.trim().isEmpty()) missingData.add("thankYouSnapshot");

        if (!missingData.isEmpty()) {
            result.put("hasMissingData", true);
            result.put("missingData", missingData);
            return result;
        }

        result.put("hasMissingData", false);
        OrderSnapshot thankYou = parseOrderSnapshot(snapshotText);
        OrderTotals checkoutTotals = buildCheckoutTotals(totalsMap);

        result.put("thankYou", thankYou);
        result.put("checkoutTotals", checkoutTotals);
        result.put("totalsMatch", validateTotalsMatch(checkoutTotals, thankYou.getTotals()));
        return result;
    }

    // === Private helpers ===

    private String getTextByXpath(String xpath) {
        List<WebElement> elements = getElements(By.xpath(xpath));
        if (elements.isEmpty()) return "";
        String text = elements.get(0).getText();
        return text != null ? text.trim() : "";
    }

    private String getChildText(WebElement root, String childXpath) {
        List<WebElement> elements = root.findElements(By.xpath(childXpath));
        if (elements.isEmpty()) return "";
        String text = elements.get(0).getText();
        return text != null ? text.trim() : "";
    }

    private String normalizeLabelValue(String valueWithLabel) {
        if (valueWithLabel == null || valueWithLabel.trim().isEmpty()) return "";
        String trimmed = valueWithLabel.trim();
        int colonIdx = trimmed.indexOf(':');
        if (colonIdx >= 0 && colonIdx < trimmed.length() - 1) {
            return trimmed.substring(colonIdx + 1).trim();
        }
        return trimmed;
    }

    private String normalizeQuantity(String qtyText) {
        if (qtyText == null || qtyText.trim().isEmpty()) return "1";
        String digits = qtyText.trim().replaceAll("[^0-9]", "");
        return digits.isEmpty() ? "1" : digits;
    }

    private String readOrderIdFromCookies() {
        for (Cookie cookie : getDriver().manage().getCookies()) {
            if (cookie.getName() != null && cookie.getName().equalsIgnoreCase("orderid")) {
                String value = cookie.getValue();
                if (value != null && !value.trim().isEmpty()
                        && !"null".equalsIgnoreCase(value.trim())
                        && !"undefined".equalsIgnoreCase(value.trim())) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private OrderSnapshot parseOrderSnapshot(String snapshotText) {
        JsonObject root = JsonParser.parseString(snapshotText).getAsJsonObject();
        OrderSnapshot snapshot = new OrderSnapshot();

        if (root.has("customer")) {
            JsonObject c = root.getAsJsonObject("customer");
            CustomerInfo ci = new CustomerInfo();
            ci.setEmail(jsonString(c, "email"));
            ci.setName(jsonString(c, "name"));
            ci.setAddress1(jsonString(c, "address1"));
            ci.setAddress2(jsonString(c, "address2"));
            ci.setPhone(jsonString(c, "phone"));
            snapshot.setCustomer(ci);
        }

        if (root.has("totals")) {
            JsonObject t = root.getAsJsonObject("totals");
            OrderTotals totals = new OrderTotals();
            totals.setSubtotal(parseJsonPrice(t, "subtotal"));
            totals.setShipping(parseJsonPrice(t, "shipping"));
            totals.setTotal(parseJsonPrice(t, "total"));
            totals.setTax(parseJsonPrice(t, "tax"));
            totals.setTip(parseJsonPrice(t, "tip"));
            totals.setDiscount(parseJsonPrice(t, "discount"));
            snapshot.setTotals(totals);
        }

        if (root.has("items") && root.get("items").isJsonArray()) {
            JsonArray itemsArray = root.getAsJsonArray("items");
            List<OrderItem> orderItems = new ArrayList<>();
            for (JsonElement el : itemsArray) {
                JsonObject io = el.getAsJsonObject();
                OrderItem oi = new OrderItem();
                oi.setTitle(jsonString(io, "title"));
                String qtyStr = jsonString(io, "qty");
                oi.setQty(qtyStr.isEmpty() ? 1 : Integer.parseInt(qtyStr.replaceAll("[^0-9]", "")));
                String priceStr = jsonString(io, "price");
                oi.setPriceText(priceStr);
                oi.setPrice(PriceUtils.parsePrice(priceStr));
                orderItems.add(oi);
            }
            snapshot.setItems(orderItems);
        }

        snapshot.setShippingMethod(jsonString(root, "shippingMethod"));
        snapshot.setReferenceOrder(jsonString(root, "referenceOrder"));
        return snapshot;
    }

    private OrderTotals buildCheckoutTotals(Map<String, String> totalsMap) {
        OrderTotals totals = new OrderTotals();
        totals.setSubtotal(PriceUtils.parsePrice(totalsMap.get("Subtotal")));
        totals.setShipping(PriceUtils.parsePrice(totalsMap.get("Shipping")));
        totals.setTotal(PriceUtils.parsePrice(totalsMap.get("Total")));
        totals.setTax(PriceUtils.parsePrice(totalsMap.get("Tax")));
        totals.setTip(PriceUtils.parsePrice(totalsMap.get("Tip")));
        totals.setDiscount(PriceUtils.parsePrice(totalsMap.get("TotalSavings")));
        return totals;
    }

    private Map<String, Object> validateTotalsMatch(OrderTotals checkout, OrderTotals thankYou) {
        Map<String, Object> result = new HashMap<>();
        if (thankYou == null) {
            result.put("allMatch", false);
            return result;
        }
        result.put("subtotalMatch", PriceUtils.priceEquals(checkout.getSubtotal(), thankYou.getSubtotal()));
        result.put("shippingMatch", PriceUtils.priceEquals(checkout.getShipping(), thankYou.getShipping()));
        result.put("totalMatch", PriceUtils.priceEquals(checkout.getTotal(), thankYou.getTotal()));
        return result;
    }

    private String jsonString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private BigDecimal parseJsonPrice(JsonObject obj, String key) {
        String value = jsonString(obj, key);
        return value.isEmpty() ? BigDecimal.ZERO : PriceUtils.parsePrice(value);
    }
}
