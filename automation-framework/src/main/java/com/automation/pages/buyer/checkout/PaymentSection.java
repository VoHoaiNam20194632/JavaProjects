package com.automation.pages.buyer.checkout;

import com.automation.enums.WaitStrategy;
import com.automation.pages.BasePage;
import com.automation.utils.WaitUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.util.*;

public class PaymentSection extends BasePage {

    private static final By PLACE_ORDER_BUTTON = By.xpath("//button[@id=\"submit-button\"]");
    private static final By CARD_NUMBER_INPUT = By.xpath("//input[@id=\"payment-numberInput\"]");
    private static final By CARD_EXPIRY_INPUT = By.xpath("//input[@id=\"payment-expiryInput\"]");
    private static final By CARD_CVC_INPUT = By.xpath("//input[@id=\"payment-cvcInput\"]");
    private static final By CREDIT_CARD_CHECKBOX = By.xpath("//label[@for=\"payment-method-card\"]");
    private static final By CASH_ON_DELIVERY = By.xpath("//label[normalize-space(text())=\"Cash on Delivery\"]");
    private static final By PAYPAL_LABEL = By.xpath("//label[@for=\"payment-method-paypal\"]");
    private static final By STRIPE_IFRAME = By.xpath("//div[@id=\"credit-card\"]//iframe");
    private static final By CARDS_SHIELD_IFRAME = By.xpath("//div[@id=\"credit-card\"]//iframe[@id=\"mecom_stripe_payment_form\"]");
    private static final By NESTED_PAYMENT_IFRAME = By.xpath("//div[@id=\"payment_container\"]//iframe[@title=\"Secure payment input frame\"]");
    private static final By PAYPAL_IFRAME = By.xpath("//div[@id=\"paypal-button-container\"]//iframe[@title=\"PayPal\"]");
    private static final By PAYPAL_BUTTON_CONTAINER = By.xpath("//div[@id=\"paypal-button-container\"]");
    private static final By PAYMENT_BLOCK = By.xpath("//div[@class=\"payment-block\"]");
    private static final By CREDIT_CARD_BLOCK = By.xpath("//div[@id=\"credit-card\"]");

    @Step("Select Cash on Delivery")
    public PaymentSection selectCashOnDelivery() {
        log.info("Selecting Cash on Delivery");
        if (isElementDisplayed(CASH_ON_DELIVERY, 5)) {
            clickJs(CASH_ON_DELIVERY);
        }
        return this;
    }

    @Step("Fill Stripe credit card info")
    public PaymentSection fillCreditCardStripe(String cardNumber, String expiry, String cvc) {
        log.info("Filling Stripe credit card");
        switchToIFrame(STRIPE_IFRAME);
        type(CARD_NUMBER_INPUT, cardNumber);
        type(CARD_EXPIRY_INPUT, expiry);
        type(CARD_CVC_INPUT, cvc);
        return this;
    }

    @Step("Fill Cards Shield Stripe credit card info")
    public PaymentSection fillCreditCardCardsShieldStripe(String cardNumber, String expiry, String cvc) {
        log.info("Filling Cards Shield Stripe credit card");
        switchToIFrame(CARDS_SHIELD_IFRAME);
        switchToIFrame(NESTED_PAYMENT_IFRAME);
        type(CARD_NUMBER_INPUT, cardNumber);
        type(CARD_EXPIRY_INPUT, expiry);
        type(CARD_CVC_INPUT, cvc);
        return this;
    }

    @Step("Fill Mesh Stripe credit card info")
    public PaymentSection fillCreditCardMeshStripe(String cardNumber, String expiry, String cvc) {
        log.info("Filling Mesh Stripe credit card");
        switchToIFrame(By.xpath("//div[@id=\"credit-card\"]//iframe[@id=\"mecom_stripe_payment_form\"]"));
        switchToIFrame(NESTED_PAYMENT_IFRAME);
        type(CARD_NUMBER_INPUT, cardNumber);
        type(CARD_EXPIRY_INPUT, expiry);
        type(CARD_CVC_INPUT, cvc);
        return this;
    }

    @Step("Auto-detect and fill payment method")
    public PaymentSection fillPaymentInfo() {
        switchToDefaultContent();
        WaitUtils.waitForSpinnerToDisappear();

        // Try COD first
        if (isElementDisplayed(CASH_ON_DELIVERY, 5)) {
            clickJs(CASH_ON_DELIVERY);
            return this;
        }

        // Fall back to Stripe credit card
        if (!getElements(By.xpath("//label[@for=\"payment-method-card\"]")).isEmpty()) {
            clickJs(CREDIT_CARD_CHECKBOX);
        }
        fillCreditCardStripe("4242424242424242", "12/28", "123");
        switchToDefaultContent();
        return this;
    }

    @Step("Click Place Order button")
    public PaymentSection placeOrder() {
        log.info("Placing order");
        switchToDefaultContent();
        clickJs(PLACE_ORDER_BUTTON);
        return this;
    }

    @Step("Get payment form display status for {paymentName}")
    public Map<String, Boolean> getPaymentFormDisplayStatus(String paymentName) {
        Map<String, Boolean> status = new HashMap<>();
        switch (paymentName) {
            case "Stripe":
                status.put("paymentBlockDisplayed", isElementDisplayed(PAYMENT_BLOCK, 30));
                status.put("creditCardDisplayed", isElementDisplayed(CREDIT_CARD_BLOCK, 30));
                break;
            case "PayPal":
                scrollToElement(By.xpath("//div[.//h4[normalize-space()=\"Payment\"] and @class=\"payment-block\"]//div[contains(concat(' ',normalize-space(@class),' '),' paypal-info ')]"));
                status.put("paypalDisplayed", isElementDisplayed(PAYPAL_BUTTON_CONTAINER, 30) || isElementDisplayed(PAYPAL_IFRAME, 30));
                break;
            default:
                status.put("unsupportedPayment", true);
        }
        return status;
    }

    @Step("Handle PayPal payment flow")
    public PaymentSection handlePayPalPayment(String email, String password) {
        log.info("Handling PayPal payment flow");
        Set<String> baselineHandles = new HashSet<>(getDriver().getWindowHandles());
        String originalWindow = getDriver().getWindowHandle();

        // Click PayPal label
        if (!getElements(PAYPAL_LABEL).isEmpty()) {
            clickJs(PAYPAL_LABEL);
        }

        // Click PayPal button in iframe
        scrollToElement(By.xpath("//div[.//h4[normalize-space()=\"Payment\"] and @class=\"payment-block\"]//div[contains(concat(' ',normalize-space(@class),' '),' paypal-info ')]"));
        baselineHandles = new HashSet<>(getDriver().getWindowHandles());

        if (isElementDisplayed(PAYPAL_IFRAME, 35)) {
            switchToIFrame(PAYPAL_IFRAME);
            clickJs(By.xpath("//*[@id=\"buttons-container\"]/div/div/div"));
            switchToDefaultContent();
        }

        // Switch to PayPal window
        switchToNewWindow(baselineHandles);

        try {
            waitForUrlContains("paypal", 20);
            type(By.xpath("//input[@id='email' or @name='login_email']"), email);

            By nextBtn = By.xpath("//button[@id='btnNext' or @name='btnNext']");
            if (isElementDisplayed(nextBtn, 5)) {
                click(nextBtn);
            }
            sleepSilently(5000);

            By loginWithPassword = By.xpath("//a[normalize-space()=\"Log in with a password instead\"]");
            if (isElementDisplayed(loginWithPassword, 5)) {
                click(loginWithPassword);
                sleepSilently(1000);
            }

            type(By.xpath("//input[@id=\"password\"]"), password);
            sleepSilently(1000);

            click(By.xpath("//button[@id='btnLogin' or @name='btnLogin' or contains(normalize-space(.),'Log In')]"));

            By notNow = By.xpath("//button[contains(.,'Not now') or contains(.,'Not Now') or contains(.,'Skip')]");
            if (isElementDisplayed(notNow, 8)) {
                click(notNow);
            }

            By completePurchase = By.xpath("//div[@data-dd-action-name=\"Pay\"]");
            By completePurchaseText = By.xpath("//button[contains(.,'Complete Purchase') or contains(.,'Pay Now')]");
            if (isElementDisplayed(completePurchase, 10)) {
                click(completePurchase);
            } else if (isElementDisplayed(completePurchaseText, 10)) {
                click(completePurchaseText);
            }

            // Wait for PayPal window to close
            waitForWindowCount(baselineHandles.size(), 90000);
        } finally {
            getDriver().switchTo().window(originalWindow);
            switchToDefaultContent();
        }
        return this;
    }

    // === Private helpers ===

    private void switchToNewWindow(Set<String> baselineHandles) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 20000) {
            Set<String> current = getDriver().getWindowHandles();
            for (String handle : current) {
                if (!baselineHandles.contains(handle)) {
                    getDriver().switchTo().window(handle);
                    return;
                }
            }
            sleepSilently(500);
        }
    }

    private void waitForWindowCount(int expectedCount, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (getDriver().getWindowHandles().size() <= expectedCount) {
                return;
            }
            sleepSilently(500);
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
