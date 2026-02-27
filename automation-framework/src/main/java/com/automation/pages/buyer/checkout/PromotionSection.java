package com.automation.pages.buyer.checkout;

import com.automation.enums.WaitStrategy;
import com.automation.pages.BasePage;
import com.automation.utils.PriceUtils;
import com.automation.utils.WaitUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PromotionSection extends BasePage {

    private static final By COUPON_LINK = By.xpath("//*[@aria-controls=\"collapse-coupon\"]");
    private static final By COUPON_INPUT = By.xpath("//input[@data-testid=\"coupon_code\"]");
    private static final By APPLY_BUTTON = By.xpath("//button[@data-testid=\"btn-apply-promotion\"]");
    private static final By COUPON_SUCCESS_MSG = By.xpath("//p[normalize-space(text())=\"The promo code you entered is already applied\"]");
    private static final By COUPON_ERROR_MSG = By.xpath("//p[normalize-space(text())=\"The promo code you entered is invalid or already expired\"]");
    private static final By DISCOUNT_VALUE = By.xpath("//div[@data-testid=\"div_total_saving\"]//span");
    private static final By SUBTOTAL = By.xpath("//div[@data-testid=\"div_subtotal\"]/span");

    @Step("Apply coupon code: {code}")
    public PromotionSection applyCoupon(String code) {
        log.info("Applying coupon: {}", code);
        clickJs(COUPON_LINK);
        if (!isElementDisplayed(COUPON_INPUT, 3)) {
            clickJs(COUPON_LINK);
        }
        type(COUPON_INPUT, code);
        clickJs(APPLY_BUTTON);
        return this;
    }

    @Step("Verify coupon applied successfully")
    public boolean isCouponAppliedSuccessfully() {
        WaitUtils.waitForSpinnerToDisappear();
        return isElementDisplayed(COUPON_SUCCESS_MSG, 30);
    }

    @Step("Verify coupon application failed")
    public boolean isCouponApplicationFailed() {
        WaitUtils.waitForSpinnerToDisappear();
        return isElementDisplayed(COUPON_ERROR_MSG, 15);
    }

    @Step("Get discount amount text")
    public String getDiscountAmountText() {
        try {
            return getText(DISCOUNT_VALUE);
        } catch (Exception e) {
            return "";
        }
    }

    @Step("Get discount amount as BigDecimal")
    public BigDecimal getDiscountAmount() {
        return PriceUtils.parsePrice(getDiscountAmountText());
    }

    @Step("Check if discount is displayed")
    public boolean isDiscountDisplayed() {
        return isElementDisplayed(DISCOUNT_VALUE, 5);
    }

    @Step("Get promotion invalid check result")
    public Map<String, Object> getPromotionInvalidCheckResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("discountDisplayed", isDiscountDisplayed());
        result.put("errorMessageDisplayed", isElementDisplayed(COUPON_ERROR_MSG, 5));
        return result;
    }
}
