package com.automation.pages.buyer;

import com.automation.enums.WaitStrategy;
import com.automation.pages.BasePage;
import com.automation.utils.PriceUtils;
import com.automation.utils.WaitUtils;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CartPage extends BasePage {

    // Mini cart locators
    private final By miniCartIcon = By.xpath("//a[@aria-label=\"Order Cart\"]");
    private final By miniCartCheckoutBtn = By.xpath("//button[normalize-space(text())=\"Checkout\"]");
    private final By miniCartViewCartBtn = By.xpath("//button[normalize-space(text())=\"View Cart\"]");
    private final By miniCartQtyBadge = By.xpath("//a[@aria-label=\"Order Cart\"]//span[contains(concat(' ',normalize-space(@class),' '),' noti-cart ')]");
    private final By miniCartSubtotal = By.xpath("//span[contains(concat(' ',normalize-space(@class),' '),' cart-total-price ')]");
    private final By miniCartListItems = By.xpath("//div[@class=\"cart-list\"]/div[@class=\"list-item\"]");
    private final By miniCartRemoveItem = By.xpath("//div[@class=\"remove-item\"]/a/i");

    // Cart page locators
    private final By cartEmptyText = By.xpath("//p[text()=\"Your Shopping Cart Empty\"]");
    private final By cartSubtotal = By.xpath("//div[.//span[normalize-space(text())=\"Subtotal\"] and contains(concat(' ',normalize-space(@class),' '),' cart-head-button ')]//span[not(contains(normalize-space(.), \"Subtotal\"))][last()]");
    private final By cartProducts = By.xpath("//form[@id=\"form-submit-cart\"]/div/div[1]/div[@class=\"row g-0 cart-line\"]");
    private final By cartCheckoutBtn = By.xpath("//a[normalize-space(.)=\"Checkout\"]");
    private final By cartProductTitle = By.xpath("//a[@class=\"title-cart-item\"]");

    // Coupon locators
    private final By couponLink = By.xpath("//a[text()=\"Have a gift card or coupon code?\"] | //p[text()=\"Have a gift card or coupon code?\"]");
    private final By couponInput = By.xpath("//div[@id=\"collapse-coupon\"]//input | //div[@class=\"div-promotion\"]//input");
    private final By couponApplyBtn = By.cssSelector("#btn-apply-promotion");
    private final By discountValue = By.xpath("//div[text()=\"Discount\"]/span");

    // === Mini Cart Actions ===

    @Step("Click mini cart icon")
    public CartPage clickMiniCart() {
        click(miniCartIcon);
        return this;
    }

    @Step("Check if mini cart has products")
    public boolean isMiniCartHasProduct() {
        return isElementDisplayed(miniCartQtyBadge, 5);
    }

    @Step("Click View Cart from mini cart")
    public CartPage clickViewCartFromMiniCart() {
        click(miniCartViewCartBtn);
        return this;
    }

    @Step("Click Checkout from mini cart")
    public CartPage clickCheckoutFromMiniCart() {
        click(miniCartCheckoutBtn);
        return this;
    }

    @Step("Remove item from mini cart")
    public CartPage removeItemFromMiniCart() {
        click(miniCartRemoveItem);
        WaitUtils.waitForSpinnerToDisappear();
        return this;
    }

    // === Mini Cart Data ===

    @Step("Get mini cart subtotal")
    public String getMiniCartSubtotal() {
        return getText(miniCartSubtotal);
    }

    @Step("Get mini cart item count")
    public int getMiniCartItemCount() {
        List<WebElement> items = getElements(miniCartListItems);
        return items.size();
    }

    @Step("Get mini cart item info at index {index}")
    public Map<String, String> getMiniCartItemInfo(int index) {
        Map<String, String> info = new HashMap<>();
        String base = "//div[@class=\"cart-list\"]/div[@class=\"list-item\"][" + index + "]";
        String name = getText(By.xpath(base + "/div/div[2]/p[1]"));
        String price = getText(By.xpath(base + "/div/div[2]//span"));
        info.put("Title", name);
        info.put("Price Product", price);
        return info;
    }

    // === Cart Page Actions ===

    @Step("Navigate to cart page")
    public CartPage navigateToCart() {
        String currentUrl = getCurrentUrl();
        String origin = currentUrl.split("//")[0] + "//" + currentUrl.split("//")[1].split("/")[0];
        navigateTo(origin + "/cart");
        WaitUtils.waitForSpinnerToDisappear();
        waitForUrlContains("cart", 15);
        return this;
    }

    @Step("Click Checkout on cart page")
    public void clickCheckout() {
        click(cartCheckoutBtn);
    }

    @Step("Check if cart is empty")
    public boolean isCartEmpty() {
        return isElementDisplayed(cartEmptyText, 7);
    }

    @Step("Get cart subtotal")
    public String getCartSubtotal() {
        return getText(cartSubtotal);
    }

    @Step("Get cart item count")
    public int getCartItemCount() {
        if (isCartEmpty()) {
            return 0;
        }
        return getElements(cartProducts).size();
    }

    // === Cart Item Data ===

    @Step("Get cart item info at row {rowIndex}")
    public Map<String, String> getCartItemInfo(int rowIndex) {
        Map<String, String> info = new HashMap<>();
        String base = "(//form[@id=\"form-submit-cart\"]//div[contains(concat(' ',normalize-space(@class),' '),' cart-line ')])[" + rowIndex + "]";
        String nameXpath = base + "//a[contains(concat(' ',normalize-space(@class),' '),' title-cart-item ')]";
        String qtyXpath = base + "//div[contains(concat(' ',normalize-space(@class),' '),' qty-select ')]//input";
        String priceXpath = base + "//div[contains(concat(' ',normalize-space(@class),' '),' total-cart-item ')]";

        String name = getText(By.xpath(nameXpath));
        String qty = getAttribute(By.xpath(qtyXpath), "value");
        String priceTotal = getText(By.xpath(priceXpath));
        double qtyValue = Double.parseDouble(qty);
        String priceProduct = PriceUtils.calculateUnitPriceFromLineTotal(priceTotal, qtyValue);

        info.put("Title", name);
        info.put("Qty", qty);
        info.put("Price Total", priceTotal);
        info.put("Price Product", priceProduct);
        return info;
    }

    @Step("Get item quantity at row {rowIndex}")
    public String getItemQuantity(int rowIndex) {
        String qtyXpath = "(//div[contains(concat(' ',normalize-space(@class),' '),' cart-line ')])[" + rowIndex + "]//div[contains(concat(' ',normalize-space(@class),' '),' qty-select ')]//input";
        return getAttribute(By.xpath(qtyXpath), "value");
    }

    // === Cart Quantity Actions ===

    @Step("Set quantity to {qty} for row {rowIndex}")
    public CartPage setQuantity(int rowIndex, String qty) {
        String qtyXpath = "(//div[contains(concat(' ',normalize-space(@class),' '),' cart-line ')])[" + rowIndex + "]//div[contains(concat(' ',normalize-space(@class),' '),' qty-select ')]//input";
        typeAndEnter(By.xpath(qtyXpath), qty);
        WaitUtils.waitForSpinnerToDisappear();
        return this;
    }

    @Step("Remove cart item at row {rowIndex}")
    public boolean removeCartItem(int rowIndex) {
        String rowXpath = "(//form[@id=\"form-submit-cart\"]//div[contains(concat(' ',normalize-space(@class),' '),' cart-line ')])[" + rowIndex + "]";
        String[] removeXpaths = {
                rowXpath + "//div[contains(@class,'remove-item')]//i",
                rowXpath + "//a[contains(translate(normalize-space(.),'REMOVE','remove'),'remove')]",
                rowXpath + "//i[contains(@class,'trash') or contains(@class,'remove') or contains(@class,'bi-x')]"
        };
        for (String xpath : removeXpaths) {
            By locator = By.xpath(xpath);
            if (isElementDisplayed(locator, 3)) {
                clickJs(locator);
                WaitUtils.waitForSpinnerToDisappear();
                return true;
            }
        }
        return false;
    }

    // === Coupon ===

    @Step("Apply coupon code: {code}")
    public CartPage applyCouponCode(String code) {
        click(couponLink);
        if (!isElementDisplayed(couponInput, 3)) {
            click(couponLink);
        }
        type(couponInput, code);
        click(couponApplyBtn);
        return this;
    }
}
