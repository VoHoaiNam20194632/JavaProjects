package com.automation.pages.buyer.checkout;

import com.automation.enums.WaitStrategy;
import com.automation.pages.BasePage;
import com.automation.utils.PriceUtils;
import com.automation.utils.WaitUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ShippingSection extends BasePage {

    private static final By EMAIL_INPUT = By.xpath("//input[@id=\"order-email\"]");
    private static final By FULL_NAME_INPUT = By.xpath("//input[@id=\"order-name\"]");
    private static final By PHONE_INPUT = By.xpath("//input[@id=\"order-phone\"]");
    private static final By COUNTRY_SELECT = By.xpath("//select[@id=\"order-country\"]");
    private static final By STATE_CONTAINER = By.xpath("//div[select[@id=\"order-state\"]]");
    private static final By CITY_INPUT = By.xpath("//input[@id=\"order-city\"]");
    private static final By ADDRESS_INPUT = By.xpath("//input[@id=\"ac-address-line1\"]");
    private static final By ZIP_INPUT = By.xpath("//input[@id=\"order-postal-code\"]");
    private static final By SHIPPING_METHODS = By.cssSelector("label[for^='radio-shipping-method-']");
    private static final By SHIPPING_VALUE = By.xpath("//div[@data-testid=\"div_shipping\"]/span");
    private static final By SHIPPING_FREE_ORDER_SUMMARY = By.xpath("//span[@class=\"price shipping-value\" and text()=\"FREE\"]");
    private static final By SHIPPING_FREE_METHOD = By.xpath("//span[@class=\"price shipping-value\"]");

    private final Map<String, String> shippingInfo = new HashMap<>();

    @Step("Fill email: {email}")
    public ShippingSection fillEmail(String email) {
        typeAndEnter(EMAIL_INPUT, email);
        return this;
    }

    @Step("Fill shipping info")
    public ShippingSection fillShippingInfo(String fullName, String email, String phone,
                                            String country, String state, String city,
                                            String address, String zipCode) {
        log.info("Filling shipping: email={}, country={}, state={}", email, country, state);
        shippingInfo.put("FullName", fullName);
        shippingInfo.put("Email", email);
        shippingInfo.put("PhoneNumber", phone);
        shippingInfo.put("Country", country);
        shippingInfo.put("States", state);
        shippingInfo.put("City", city);
        shippingInfo.put("StreetAddress", address);
        shippingInfo.put("Zipcode", zipCode);

        sleepSilently(2000);
        typeAndEnter(EMAIL_INPUT, email);

        // Select country
        click(COUNTRY_SELECT);
        By countryOption = By.xpath("//option[normalize-space(text())=\"" + country + "\"]");
        sleepSilently(500);
        click(countryOption);
        sleepSilently(1000);

        type(FULL_NAME_INPUT, fullName);
        type(PHONE_INPUT, phone);
        type(ADDRESS_INPUT, address);
        type(CITY_INPUT, city);

        // Select state
        click(STATE_CONTAINER);
        By stateOption = By.xpath("//option[normalize-space(text())=\"" + state + "\"]");
        sleepSilently(500);
        click(stateOption);

        type(ZIP_INPUT, zipCode);

        // Store for later verification
        setData("DATA_BUYER_EMAIL_EXPECT", email);
        setData("DATA_BUYER_FULL_NAME_EXPECT", fullName);
        setData("DATA_BUYER_PHONE_NUMBER_EXPECT", phone);
        setData("DATA_BUYER_COUNTRY_EXPECT", country);
        setData("DATA_BUYER_STATES_EXPECT", state);
        setData("DATA_BUYER_CITY_EXPECT", city);
        setData("DATA_BUYER_STREET_ADDRESS_EXPECT", address);
        setData("DATA_BUYER_ZIP_CODE_EXPECT", zipCode);
        return this;
    }

    @Step("Fill default shipping info from test data")
    public ShippingSection fillDefaultShippingInfo() {
        return fillShippingInfo(
                "Test Automation User",
                "vohoainam650@gmail.com",
                "0866843576",
                "United States",
                "CA - California",
                "Los Angeles",
                "123 Test Street, Suite 100",
                "90001"
        );
    }

    @Step("Select random shipping method")
    public ShippingSection selectRandomShippingMethod() {
        log.info("Selecting random shipping method");
        List<WebElement> methods = waitAndFindAll(SHIPPING_METHODS, WaitStrategy.VISIBLE);
        Random random = new Random();
        WebElement selected = methods.get(random.nextInt(methods.size()));
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", selected);
        String methodText = selected.getText();
        sleepSilently(5000);
        setData("DATA_BUYER_PAGE_CHECK_OUT_METHOD_SHIPPING_SELECT", methodText);
        log.info("Selected shipping method: {}", methodText);
        return this;
    }

    @Step("Get shipping method price from order summary")
    public String getOrderSummaryShipping() {
        WaitUtils.waitForSpinnerToDisappear();
        String value = getTextSafe(SHIPPING_VALUE);
        int attempts = 0;
        while ("Calculating...".equalsIgnoreCase(value) && attempts < 10) {
            sleepSilently(1000);
            value = getTextSafe(SHIPPING_VALUE);
            attempts++;
        }
        return value;
    }

    @Step("Check if free shipping is applied")
    public boolean isFreeShippingApplied() {
        boolean hasFreeText = isElementDisplayed(By.xpath("//span[@class=\"price shipping-value\" and text()=\"FREE\"]"), 5);
        boolean hasStrikethrough = isElementDisplayed(By.xpath("//span[@class=\"text-decoration-line-through me-2\"]"), 5);
        return hasFreeText && hasStrikethrough;
    }

    public Map<String, String> getShippingInfo() {
        return shippingInfo;
    }

    // === Private helpers ===

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
