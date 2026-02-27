package com.automation.pages.buyer.checkout;

import com.automation.enums.WaitStrategy;
import com.automation.pages.BasePage;
import com.automation.utils.CurrencyUtils;
import com.automation.utils.PriceUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class TipSection extends BasePage {

    private static final By CUSTOM_TIP_INPUT = By.xpath("//input[@id=\"custom-tip-input\"]");
    private static final By UPDATE_TIP_BUTTON = By.xpath("//div[@id=\"update-tip-button\"]");
    private static final By TIP_VALUE = By.xpath("//div[@data-testid=\"div_tip\"]/span");
    private static final By SUBTOTAL = By.xpath("//div[@data-testid=\"div_subtotal\"]/span");
    private static final By SHOW_ORDER_SUMMARY = By.xpath("//span[text()=\"Show order summary\"]");
    private static final String TIP_OPTIONS_XPATH =
            "//div[contains(@class,'order-tip-options')]//div[contains(concat(' ',normalize-space(@class),' '),' tip-option ')]";

    @Step("Enter custom tip: {tip}")
    public TipSection enterCustomTip(String tip) {
        type(CUSTOM_TIP_INPUT, tip);
        clickJs(UPDATE_TIP_BUTTON);
        return this;
    }

    @Step("Verify tip update")
    public Map<String, BigDecimal> getTipUpdateComparison(String tip) {
        String tipUi = waitForPriceReady(TIP_VALUE);
        log.info("TipUI: {}", tipUi);
        BigDecimal expected = PriceUtils.parsePrice(tip);
        BigDecimal actual = PriceUtils.parsePrice(tipUi);
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("expected", expected);
        result.put("actual", actual);
        return result;
    }

    @Step("Get tip options")
    public List<WebElement> getTipOptions() {
        return getElements(By.xpath(TIP_OPTIONS_XPATH));
    }

    @Step("Validate tip options")
    public Map<String, Object> getTipOptionsValidationResult() {
        String subtotalText = waitForPriceReady(SUBTOTAL);
        BigDecimal subtotal = PriceUtils.parsePrice(subtotalText);
        List<WebElement> options = getTipOptions();

        Map<String, Object> result = new HashMap<>();
        result.put("hasOptions", !options.isEmpty());
        result.put("optionCount", options.size());
        result.put("baseAmount", subtotal);

        if (options.isEmpty()) return result;

        // Try percent validation first
        Map<String, Object> percentResult = validateTipAmountMatchesPercent(options, subtotal);
        if ((Boolean) percentResult.get("isValid")) {
            result.put("validationMode", "percent");
            result.put("validationResult", percentResult);
            return result;
        }

        // Try fixed validation
        Map<String, Object> fixedResult = validateTipPercentMatchesAmount(options, subtotal);
        if ((Boolean) fixedResult.get("isValid")) {
            result.put("validationMode", "fixed");
            result.put("validationResult", fixedResult);
            return result;
        }

        result.put("validationMode", "both_failed");
        result.put("percentResult", percentResult);
        result.put("fixedResult", fixedResult);
        return result;
    }

    @Step("Validate tip options update order summary")
    public Map<String, Object> getTipOptionsOrderSummaryValidation() {
        ensureOrderSummaryVisible();
        waitForPriceReady(SUBTOTAL);

        List<WebElement> allOptions = getTipOptions();
        List<WebElement> clickableOptions = new ArrayList<>();
        for (WebElement opt : allOptions) {
            if (!isNoTipOption(opt)) clickableOptions.add(opt);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("hasOptions", !clickableOptions.isEmpty());
        result.put("optionCount", clickableOptions.size());

        if (clickableOptions.isEmpty()) return result;

        List<Map<String, Object>> validations = new ArrayList<>();
        for (int i = 0; i < clickableOptions.size(); i++) {
            // Re-fetch to avoid stale references
            List<WebElement> fresh = getTipOptions();
            List<WebElement> freshClickable = new ArrayList<>();
            for (WebElement opt : fresh) {
                if (!isNoTipOption(opt)) freshClickable.add(opt);
            }
            if (i >= freshClickable.size()) break;

            WebElement optionEl = freshClickable.get(i);
            String amountBefore = readTipAmountText(optionEl);

            ((JavascriptExecutor) getDriver()).executeScript("arguments[0].scrollIntoView({block: 'center'});", optionEl);
            ((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", optionEl);
            sleepSilently(1000);

            String tipText = waitForPriceReady(TIP_VALUE);

            // Re-read amount after click
            fresh = getTipOptions();
            freshClickable = new ArrayList<>();
            for (WebElement opt : fresh) {
                if (!isNoTipOption(opt)) freshClickable.add(opt);
            }
            String amountAfter = i < freshClickable.size() ? readTipAmountText(freshClickable.get(i)) : amountBefore;

            BigDecimal expected = PriceUtils.parsePrice(amountAfter);
            BigDecimal actual = PriceUtils.parsePrice(tipText);

            Map<String, Object> validation = new HashMap<>();
            validation.put("expected", expected);
            validation.put("actual", actual);
            validation.put("matches", PriceUtils.priceEquals(actual, expected));
            validations.add(validation);
        }

        result.put("validations", validations);
        result.put("allValid", validations.stream().allMatch(v -> (Boolean) v.get("matches")));
        return result;
    }

    @Step("Validate tip options currency")
    public Map<String, Object> getTipOptionsCurrencyValidation(String expectedCurrency) {
        String expected = CurrencyUtils.normalize(expectedCurrency);
        List<WebElement> options = getTipOptions();

        Map<String, Object> result = new HashMap<>();
        result.put("expectedCurrency", expected);
        result.put("hasOptions", !options.isEmpty());
        result.put("allValid", true);

        if (options.isEmpty()) return result;

        List<Map<String, Object>> validations = new ArrayList<>();
        for (WebElement option : options) {
            if (isNoTipOption(option)) continue;
            String amountText = readTipAmountText(option);
            if (amountText.isEmpty()) continue;
            String resolved = CurrencyUtils.resolveCodeFromText(amountText, expected);

            Map<String, Object> v = new HashMap<>();
            v.put("amountText", amountText);
            v.put("resolved", resolved);
            v.put("matches", resolved.equals(expected));
            if (!resolved.equals(expected)) result.put("allValid", false);
            validations.add(v);
        }
        result.put("validations", validations);
        return result;
    }

    // === Private helpers ===

    private Map<String, Object> validateTipAmountMatchesPercent(List<WebElement> options, BigDecimal subtotal) {
        Map<String, Object> result = new HashMap<>();
        result.put("isValid", true);
        boolean checked = false;
        for (WebElement option : options) {
            if (isNoTipOption(option)) continue;
            BigDecimal percent = parsePercentValue(readTipPercentText(option));
            BigDecimal actual = PriceUtils.parsePrice(readTipAmountText(option));
            BigDecimal expected = subtotal.multiply(percent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (!PriceUtils.priceEquals(actual, expected)) {
                result.put("isValid", false);
            }
            checked = true;
        }
        if (!checked) result.put("isValid", false);
        return result;
    }

    private Map<String, Object> validateTipPercentMatchesAmount(List<WebElement> options, BigDecimal subtotal) {
        Map<String, Object> result = new HashMap<>();
        result.put("isValid", true);
        boolean checked = false;
        for (WebElement option : options) {
            if (isNoTipOption(option)) continue;
            BigDecimal amount = PriceUtils.parsePrice(readTipAmountText(option));
            BigDecimal percent = parsePercentValue(readTipPercentText(option));
            BigDecimal expectedPercent = BigDecimal.ZERO;
            if (subtotal.compareTo(BigDecimal.ZERO) > 0) {
                expectedPercent = amount.multiply(new BigDecimal("100")).divide(subtotal, 2, RoundingMode.HALF_UP);
            }
            if (!PriceUtils.priceEquals(percent, expectedPercent)) {
                result.put("isValid", false);
            }
            checked = true;
        }
        if (!checked) result.put("isValid", false);
        return result;
    }

    private void ensureOrderSummaryVisible() {
        if (isElementDisplayed(SHOW_ORDER_SUMMARY, 3)) {
            click(SHOW_ORDER_SUMMARY);
        }
    }

    private boolean isNoTipOption(WebElement option) {
        String classAttr = option.getAttribute("class");
        return classAttr != null && classAttr.contains("no-tip");
    }

    private String readTipPercentText(WebElement option) {
        try {
            WebElement el = option.findElement(By.xpath(".//span[contains(@class,'tip-percentage')]"));
            String text = el.getText();
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            String fallback = option.getAttribute("data-value");
            return fallback != null ? fallback.trim() : "";
        }
    }

    private String readTipAmountText(WebElement option) {
        try {
            WebElement el = option.findElement(By.xpath(".//span[contains(@class,'tip-amount')]"));
            String text = el.getText();
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private BigDecimal parsePercentValue(String raw) {
        if (raw == null) return BigDecimal.ZERO;
        String cleaned = raw.replace("%", "").trim();
        if (cleaned.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return PriceUtils.parsePrice(raw);
        }
    }

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
