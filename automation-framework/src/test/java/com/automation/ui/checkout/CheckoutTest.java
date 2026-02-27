package com.automation.ui.checkout;

import com.automation.annotations.FrameworkAnnotation;
import com.automation.base.BaseTest;
import com.automation.dataproviders.CheckoutDataProvider;
import com.automation.driver.DriverManager;
import com.automation.enums.CategoryType;
import com.automation.enums.WaitStrategy;
import com.automation.pages.buyer.CartPage;
import com.automation.pages.buyer.checkout.*;
import com.automation.utils.WaitUtils;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("UI Tests")
@Feature("Checkout")
public class CheckoutTest extends BaseTest {

    private CartPage cartPage;
    private ShippingSection shippingSection;
    private PaymentSection paymentSection;
    private OrderSummarySection orderSummarySection;
    private TipSection tipSection;
    private PromotionSection promotionSection;
    private ThankYouSection thankYouSection;

    @BeforeMethod(alwaysRun = true)
    public void navigateAndAddProduct() {
        String buyerUrl = config.buyerUrl();
        navigateToBuyerStoreAndAddProduct(buyerUrl);

        cartPage = new CartPage();
        shippingSection = new ShippingSection();
        paymentSection = new PaymentSection();
        orderSummarySection = new OrderSummarySection();
        tipSection = new TipSection();
        promotionSection = new PromotionSection();
        thankYouSection = new ThankYouSection();
    }

    @Test(description = "Complete checkout with Cash on Delivery",
            dataProvider = "defaultCheckoutData", dataProviderClass = CheckoutDataProvider.class)
    @FrameworkAnnotation(category = {CategoryType.SMOKE, CategoryType.REGRESSION},
            author = "Framework", description = "Checkout end-to-end with COD payment")
    @Severity(SeverityLevel.BLOCKER)
    @Story("Checkout with COD")
    public void testCheckoutWithCashOnDelivery(Map<String, String> data) {
        log.info("Starting COD checkout test");

        cartPage.navigateToCart();
        cartPage.clickCheckout();
        orderSummarySection.waitForCheckoutPage();

        shippingSection.fillShippingInfo(
                data.get("fullName"), data.get("email"), data.get("phone"),
                data.get("country"), data.get("state"), data.get("city"),
                data.get("address"), data.get("zipCode")
        );
        shippingSection.selectRandomShippingMethod();

        paymentSection.selectCashOnDelivery();
        paymentSection.placeOrder();

        thankYouSection.waitForOrderSuccessMessage();
        Map<String, BigDecimal> totals = thankYouSection.getOrderSuccessTotalCalculation();

        assertThat(totals.get("actual"))
                .as("Thank you page total should match calculated total")
                .isEqualByComparingTo(totals.get("expected"));
    }

    @Test(description = "Complete checkout with Credit Card (Stripe)",
            dataProvider = "creditCardCheckoutData", dataProviderClass = CheckoutDataProvider.class)
    @FrameworkAnnotation(category = {CategoryType.REGRESSION},
            author = "Framework", description = "Checkout end-to-end with Stripe credit card")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Checkout with Credit Card")
    public void testCheckoutWithCreditCard(Map<String, String> data) {
        log.info("Starting credit card checkout test");

        cartPage.navigateToCart();
        cartPage.clickCheckout();
        orderSummarySection.waitForCheckoutPage();

        shippingSection.fillShippingInfo(
                data.get("fullName"), data.get("email"), data.get("phone"),
                data.get("country"), data.get("state"), data.get("city"),
                data.get("address"), data.get("zipCode")
        );
        shippingSection.selectRandomShippingMethod();

        paymentSection.fillCreditCardStripe(
                data.get("cardNumber"), data.get("cardExpiry"), data.get("cardCvc")
        );
        paymentSection.placeOrder();

        thankYouSection.waitForOrderSuccessMessage();
        Map<String, BigDecimal> totals = thankYouSection.getOrderSuccessTotalCalculation();

        assertThat(totals.get("actual"))
                .as("Thank you page total should match calculated total")
                .isEqualByComparingTo(totals.get("expected"));
    }

    @Test(description = "Checkout with custom tip",
            dataProvider = "checkoutWithTipData", dataProviderClass = CheckoutDataProvider.class)
    @FrameworkAnnotation(category = {CategoryType.REGRESSION},
            author = "Framework", description = "Checkout with custom tip amount")
    @Severity(SeverityLevel.NORMAL)
    @Story("Checkout with Tip")
    public void testCheckoutWithTip(Map<String, String> data) {
        log.info("Starting checkout with tip test");

        cartPage.navigateToCart();
        cartPage.clickCheckout();
        orderSummarySection.waitForCheckoutPage();

        shippingSection.fillShippingInfo(
                data.get("fullName"), data.get("email"), data.get("phone"),
                data.get("country"), data.get("state"), data.get("city"),
                data.get("address"), data.get("zipCode")
        );
        shippingSection.selectRandomShippingMethod();

        String tipAmount = data.get("tipAmount");
        tipSection.enterCustomTip(tipAmount);
        Map<String, BigDecimal> tipComparison = tipSection.getTipUpdateComparison(tipAmount);

        assertThat(tipComparison.get("actual"))
                .as("Tip in order summary should match entered tip amount")
                .isEqualByComparingTo(tipComparison.get("expected"));

        paymentSection.selectCashOnDelivery();
        paymentSection.placeOrder();

        thankYouSection.waitForOrderSuccessMessage();
    }

    @Test(description = "Checkout with coupon code",
            dataProvider = "checkoutWithCouponData", dataProviderClass = CheckoutDataProvider.class)
    @FrameworkAnnotation(category = {CategoryType.REGRESSION},
            author = "Framework", description = "Checkout with coupon/promotion code")
    @Severity(SeverityLevel.NORMAL)
    @Story("Checkout with Coupon")
    public void testCheckoutWithCoupon(Map<String, String> data) {
        log.info("Starting checkout with coupon test");

        cartPage.navigateToCart();
        cartPage.clickCheckout();
        orderSummarySection.waitForCheckoutPage();

        shippingSection.fillShippingInfo(
                data.get("fullName"), data.get("email"), data.get("phone"),
                data.get("country"), data.get("state"), data.get("city"),
                data.get("address"), data.get("zipCode")
        );
        shippingSection.selectRandomShippingMethod();

        String couponCode = data.get("couponCode");
        promotionSection.applyCoupon(couponCode);

        boolean couponApplied = promotionSection.isCouponAppliedSuccessfully();
        assertThat(couponApplied)
                .as("Coupon '%s' should be applied successfully", couponCode)
                .isTrue();

        assertThat(promotionSection.isDiscountDisplayed())
                .as("Discount should be displayed after applying coupon")
                .isTrue();

        paymentSection.selectCashOnDelivery();
        paymentSection.placeOrder();

        thankYouSection.waitForOrderSuccessMessage();
    }

    @Test(description = "Validate order summary items match cart items")
    @FrameworkAnnotation(category = {CategoryType.REGRESSION},
            author = "Framework", description = "Compare cart items with checkout order summary items")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Order Summary Validation")
    public void testCheckoutOrderSummaryValidation() {
        log.info("Starting order summary validation test");

        // Capture cart items before checkout
        cartPage.navigateToCart();
        int cartItemCount = cartPage.getCartItemCount();
        assertThat(cartItemCount).as("Cart should have at least 1 item").isGreaterThan(0);

        List<Map<String, String>> cartItems = new ArrayList<>();
        for (int i = 1; i <= cartItemCount; i++) {
            cartItems.add(cartPage.getCartItemInfo(i));
        }

        cartPage.clickCheckout();
        orderSummarySection.waitForCheckoutPage();
        orderSummarySection.ensureOrderSummaryVisible();

        List<Map<String, String>> checkoutItems = orderSummarySection.captureCheckoutItems();

        Map<String, Object> comparison = orderSummarySection.getCheckoutItemsCartComparison(cartItems, checkoutItems);

        assertThat(comparison.get("allMatch"))
                .as("All cart items should match checkout order summary items")
                .isEqualTo(true);
    }

    @Test(description = "Validate checkout total calculation (subtotal + shipping + tax + tip - savings = total)")
    @FrameworkAnnotation(category = {CategoryType.REGRESSION},
            author = "Framework", description = "Verify checkout total breakdown calculation")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Total Calculation Validation")
    public void testCheckoutTotalCalculation() {
        log.info("Starting checkout total calculation test");

        cartPage.navigateToCart();
        cartPage.clickCheckout();
        orderSummarySection.waitForCheckoutPage();

        shippingSection.fillDefaultShippingInfo();
        shippingSection.selectRandomShippingMethod();

        orderSummarySection.ensureOrderSummaryVisible();
        Map<String, String> totals = orderSummarySection.captureCheckoutTotals();

        Map<String, Object> breakdownResult = orderSummarySection.getTotalBreakdownComparison(totals);

        assertThat(breakdownResult.get("hasTotals"))
                .as("Totals should be available")
                .isEqualTo(true);

        assertThat(breakdownResult.get("matches"))
                .as("Total (%s) should equal subtotal + shipping + tax + tip - savings (%s)",
                        breakdownResult.get("actual"), breakdownResult.get("expected"))
                .isEqualTo(true);
    }

    // === Private helpers ===

    @Step("Navigate to buyer store and add first product to cart")
    private void navigateToBuyerStoreAndAddProduct(String buyerUrl) {
        log.info("Navigating to buyer store: {}", buyerUrl);
        org.openqa.selenium.WebDriver driver = DriverManager.getDriver();
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
        driver.get(buyerUrl);

        // Dismiss cookie consent bar if present
        By cookieBar = By.cssSelector("div.cookie-bar");
        if (WaitUtils.isElementDisplayed(cookieBar, 3)) {
            js.executeScript("document.querySelector('div.cookie-bar').style.display='none';");
            log.info("Cookie bar dismissed");
        }

        // Wait for page to load and click first product via JS to avoid overlay issues
        By firstProduct = By.xpath("//a[@class='product_title']");
        WebElement productLink = WaitUtils.waitForElement(firstProduct, WaitStrategy.VISIBLE);
        js.executeScript("arguments[0].click();", productLink);

        // Wait for product detail page to load
        WaitUtils.waitForUrlContains("/products/", 15);

        // Dismiss cookie bar again on product detail page if needed
        if (WaitUtils.isElementDisplayed(cookieBar, 2)) {
            js.executeScript("document.querySelector('div.cookie-bar').style.display='none';");
        }

        // Click Add to Cart button
        By addToCartBtn = By.xpath("//button[contains(@class,'btn-add-cart')]");
        WebElement addToCart = WaitUtils.waitForElement(addToCartBtn, WaitStrategy.CLICKABLE);
        addToCart.click();

        // Wait for cart badge to appear confirming product was added
        By cartBadge = By.xpath("//a[@aria-label='Order Cart']//span[contains(@class,'noti-cart')]");
        WaitUtils.waitForElement(cartBadge, WaitStrategy.VISIBLE, 10);
        log.info("Product added to cart successfully");
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
