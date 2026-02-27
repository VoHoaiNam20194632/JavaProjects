package com.automation.pages;

import com.automation.driver.DriverManager;
import com.automation.enums.WaitStrategy;
import com.automation.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(getClass());

    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }

    protected WebElement waitAndFind(By locator, WaitStrategy strategy) {
        return WaitUtils.waitForElement(locator, strategy);
    }

    protected List<WebElement> waitAndFindAll(By locator, WaitStrategy strategy) {
        return WaitUtils.waitForElements(locator, strategy);
    }

    protected void click(By locator, WaitStrategy strategy) {
        log.info("Clicking on: {}", locator);
        waitAndFind(locator, strategy).click();
    }

    protected void click(By locator) {
        click(locator, WaitStrategy.CLICKABLE);
    }

    protected void type(By locator, String text, WaitStrategy strategy) {
        log.info("Typing '{}' into: {}", text, locator);
        WebElement element = waitAndFind(locator, strategy);
        element.clear();
        element.sendKeys(text);
    }

    protected void type(By locator, String text) {
        type(locator, text, WaitStrategy.VISIBLE);
    }

    protected String getText(By locator, WaitStrategy strategy) {
        return waitAndFind(locator, strategy).getText();
    }

    protected String getText(By locator) {
        return getText(locator, WaitStrategy.VISIBLE);
    }

    protected boolean isDisplayed(By locator) {
        try {
            return getDriver().findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected String getPageTitle() {
        return getDriver().getTitle();
    }

    protected String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        getDriver().get(url);
    }

    protected void switchToIFrame(By iframeLocator) {
        log.info("Switching to iframe: {}", iframeLocator);
        WebElement iframe = waitAndFind(iframeLocator, WaitStrategy.PRESENCE);
        getDriver().switchTo().frame(iframe);
    }

    protected void switchToDefaultContent() {
        log.info("Switching to default content");
        getDriver().switchTo().defaultContent();
    }

    protected void uploadFile(By fileInput, String absolutePath) {
        log.info("Uploading file: {}", absolutePath);
        getDriver().findElement(fileInput).sendKeys(absolutePath);
    }

    // === Helper methods for buyer pages ===

    private static final Map<Long, Map<String, String>> CONTEXT_DATA = new ConcurrentHashMap<>();

    protected void clickJs(By locator) {
        log.info("JS clicking on: {}", locator);
        WebElement element = waitAndFind(locator, WaitStrategy.PRESENCE);
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", element);
    }

    protected void selectByVisibleText(By locator, String text) {
        log.info("Selecting '{}' from dropdown: {}", text, locator);
        WebElement element = waitAndFind(locator, WaitStrategy.VISIBLE);
        new Select(element).selectByVisibleText(text);
    }

    protected List<WebElement> getElements(By locator) {
        try {
            return getDriver().findElements(locator);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    protected boolean isElementDisplayed(By locator, int timeoutSeconds) {
        return WaitUtils.isElementDisplayed(locator, timeoutSeconds);
    }

    protected void scrollToElement(By locator) {
        log.info("Scrolling to: {}", locator);
        WebElement element = waitAndFind(locator, WaitStrategy.PRESENCE);
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    protected void waitForUrlContains(String urlPart, int timeoutSeconds) {
        WaitUtils.waitForUrlContains(urlPart, timeoutSeconds);
    }

    protected void typeAndEnter(By locator, String text) {
        log.info("Typing '{}' and pressing Enter into: {}", text, locator);
        WebElement element = waitAndFind(locator, WaitStrategy.VISIBLE);
        element.clear();
        element.sendKeys(text);
        element.sendKeys(Keys.ENTER);
    }

    protected String getAttribute(By locator, String attribute) {
        return waitAndFind(locator, WaitStrategy.VISIBLE).getAttribute(attribute);
    }

    protected String getAttribute(By locator, String attribute, WaitStrategy strategy) {
        return waitAndFind(locator, strategy).getAttribute(attribute);
    }

    protected void setData(String key, String value) {
        long threadId = Thread.currentThread().threadId();
        CONTEXT_DATA.computeIfAbsent(threadId, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    protected String getData(String key) {
        long threadId = Thread.currentThread().threadId();
        Map<String, String> data = CONTEXT_DATA.get(threadId);
        return data != null ? data.get(key) : null;
    }

    protected void clearContextData() {
        long threadId = Thread.currentThread().threadId();
        CONTEXT_DATA.remove(threadId);
    }
}
