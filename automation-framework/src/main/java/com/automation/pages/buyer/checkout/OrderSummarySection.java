package com.automation.pages.buyer.checkout;

import com.automation.enums.WaitStrategy;
import com.automation.pages.BasePage;
import com.automation.utils.CurrencyUtils;
import com.automation.utils.PriceUtils;
import com.automation.utils.WaitUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.math.BigDecimal;
import java.util.*;

public class OrderSummarySection extends BasePage {

    private static final By SUBTOTAL = By.xpath("//div[@data-testid=\"div_subtotal\"]/span");
    private static final By SHIPPING = By.xpath("//div[@data-testid=\"div_shipping\"]/span");
    private static final By TAX = By.xpath("//div[@data-testid=\"div_tax\"]//span");
    private static final By TIP = By.xpath("//div[@data-testid=\"div_tip\"]/span");
    private static final By TOTAL = By.xpath("//div[@data-testid=\"div_total\"]/span[2]/span");
    private static final By TOTAL_SAVINGS = By.xpath("//div[@data-testid=\"div_total_saving\"]/span");
    private static final By CHECKOUT_BUTTON = By.xpath("//a[@id=\"btn-process-checkout\"]");
    private static final By SHOW_ORDER_SUMMARY = By.xpath("//span[text()=\"Show order summary\"]");
    private static final By PRODUCT_TITLE = By.xpath("//a[@class=\"product-title\"]");

    @Step("Click checkout button")
    public void clickCheckout() {
        click(CHECKOUT_BUTTON);
        waitForUrlContains("/checkout", 15);
    }

    @Step("Wait for checkout page")
    public void waitForCheckoutPage() {
        waitForUrlContains("checkout", 15);
    }

    @Step("Ensure order summary is visible")
    public void ensureOrderSummaryVisible() {
        if (isElementDisplayed(SHOW_ORDER_SUMMARY, 3)) {
            click(SHOW_ORDER_SUMMARY);
        }
    }

    @Step("Get checkout item count")
    public int getCheckoutItemCount() {
        return getElements(PRODUCT_TITLE).size();
    }

    @Step("Get checkout item info at index {index}")
    public Map<String, String> getCheckoutItemInfo(int index) {
        Map<String, String> info = new HashMap<>();
        String base = "//div//div[@class=\"col-10\"][" + index + "]";
        String titleXpath = base + "//a[@class=\"product-title\"]";
        String priceXpath = base + "//div[@class=\"col-4 text-end\"]";
        String qtyXpath = "//div//div[@class=\"col-2 img-checkout-item\"][" + index + "]//span[@class=\"qty-checkout-item\"]";

        String title = getText(By.xpath(titleXpath));
        String price = getText(By.xpath(priceXpath));
        String qty = getText(By.xpath(qtyXpath));
        double qtyValue = Double.parseDouble(qty);
        String priceTotal = PriceUtils.calculateLineTotalFromUnit(price, qtyValue);

        info.put("Title", title);
        info.put("Price Product", price);
        info.put("Qty", qty);
        info.put("Price Total", priceTotal);
        return info;
    }

    @Step("Get price information from order summary")
    public Map<String, String> getPriceInformation() {
        Map<String, String> info = new HashMap<>();
        String subtotalText = getText(SUBTOTAL, WaitStrategy.PRESENCE);
        String shippingText = waitForPriceReady(SHIPPING);
        String taxText = waitForPriceReady(TAX);
        String tipText = waitForPriceReady(TIP);
        String totalText = getText(TOTAL, WaitStrategy.PRESENCE);
        String savingsText = getTextSafe(TOTAL_SAVINGS);

        String referencePrice = (subtotalText == null || subtotalText.trim().isEmpty()) ? totalText : subtotalText;
        shippingText = PriceUtils.normalizeOptionalPrice(shippingText, referencePrice);
        taxText = PriceUtils.normalizeOptionalPrice(taxText, referencePrice);
        tipText = PriceUtils.normalizeOptionalPrice(tipText, referencePrice);
        savingsText = PriceUtils.normalizeOptionalPrice(savingsText, referencePrice);

        info.put("Subtotal", subtotalText);
        info.put("Shipping", shippingText);
        info.put("Tax", taxText);
        info.put("Tip", tipText);
        info.put("Total", totalText);
        info.put("TotalSavings", savingsText);
        return info;
    }

    @Step("Capture checkout items")
    public List<Map<String, String>> captureCheckoutItems() {
        int count = getCheckoutItemCount();
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add(getCheckoutItemInfo(i));
        }
        log.info("Checkout items: {}", items);
        return items;
    }

    @Step("Capture checkout totals")
    public Map<String, String> captureCheckoutTotals() {
        sleepSilently(5000);
        Map<String, String> totals = getPriceInformation();
        log.info("Checkout totals: {}", totals);
        return totals;
    }

    @Step("Compare cart items vs checkout items")
    public Map<String, Object> getCheckoutItemsCartComparison(List<Map<String, String>> addToCart, List<Map<String, String>> checkoutItems) {
        Map<String, Object> result = new HashMap<>();
        if (addToCart == null || addToCart.isEmpty()) {
            result.put("hasAddToCart", false);
            result.put("error", "Missing add-to-cart items for comparison.");
            return result;
        }
        if (checkoutItems == null || checkoutItems.isEmpty()) {
            result.put("hasCheckoutItems", false);
            result.put("error", "Missing checkout items for comparison.");
            return result;
        }
        result.put("hasAddToCart", true);
        result.put("hasCheckoutItems", true);
        boolean allMatch = true;
        for (int i = 0; i < addToCart.size(); i++) {
            Map<String, String> cart = addToCart.get(i);
            Map<String, String> checkout = checkoutItems.get(i);
            boolean titleMatches = checkout.get("Title").equals(cart.get("Title"));
            boolean qtyMatches = checkout.get("Qty").equals(cart.get("Qty"));
            if (!titleMatches || !qtyMatches) allMatch = false;
        }
        result.put("allMatch", allMatch);
        return result;
    }

    @Step("Validate total breakdown")
    public Map<String, Object> getTotalBreakdownComparison(Map<String, String> totalsMap) {
        Map<String, Object> result = new HashMap<>();
        if (totalsMap == null || totalsMap.isEmpty()) {
            result.put("hasTotals", false);
            return result;
        }
        result.put("hasTotals", true);
        BigDecimal subtotal = PriceUtils.parsePrice(totalsMap.get("Subtotal"));
        BigDecimal shipping = PriceUtils.parsePrice(totalsMap.get("Shipping"));
        BigDecimal tax = PriceUtils.parsePrice(totalsMap.get("Tax"));
        BigDecimal tip = PriceUtils.parsePrice(totalsMap.get("Tip"));
        BigDecimal savings = PriceUtils.parsePrice(totalsMap.get("TotalSavings"));
        BigDecimal expected = subtotal.add(shipping).add(tax).add(tip).subtract(savings);
        BigDecimal actual = PriceUtils.parsePrice(totalsMap.get("Total"));
        result.put("expected", expected);
        result.put("actual", actual);
        result.put("matches", PriceUtils.priceEquals(actual, expected));
        return result;
    }

    @Step("Validate currency symbol in checkout")
    public Map<String, Object> getCheckoutCurrencySymbolValidation(String expectedSymbol) {
        Map<String, String> prices = getPriceInformation();
        String subtotal = prices.get("Subtotal");
        if (subtotal == null || subtotal.trim().isEmpty()) {
            subtotal = prices.get("Total");
        }
        String token = CurrencyUtils.extractToken(subtotal);
        Map<String, Object> result = new HashMap<>();
        result.put("subtotal", subtotal);
        result.put("token", token);
        result.put("expectedSymbol", expectedSymbol);
        result.put("matches", token.contains(expectedSymbol));
        return result;
    }

    // === Private helpers ===

    private String waitForPriceReady(By locator) {
        String value = getTextSafe(locator);
        int attempts = 0;
        while ("Calculating...".equalsIgnoreCase(value) && attempts < 10) {
            sleepSilently(1000);
            value = getTextSafe(locator);
            attempts++;
        }
        return value;
    }

    private String getTextSafe(By locator) {
        try {
            return getText(locator, WaitStrategy.PRESENCE);
        } catch (Exception e) {
            return "";
        }
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
