package com.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CurrencyUtils {

    private static final Logger logger = LogManager.getLogger(CurrencyUtils.class);

    public static final Pattern CURRENCY_CODE_PATTERN = Pattern.compile("\\b([A-Za-z]{3})\\b");
    public static final Pattern CURRENCY_CODE_UPPER_PATTERN = Pattern.compile("\\b([A-Z]{3})\\b");
    public static final Pattern PRICE_TOKEN_PATTERN = Pattern.compile("(\\p{Sc}|[A-Z]{3})?\\s*\\d[\\d.,]*");
    public static final Pattern PRICE_TOKEN_SUFFIX_PATTERN = Pattern.compile("\\d[\\d.,]*\\s*(\\p{Sc}|[A-Z]{3})");
    public static final Pattern CURRENCY_SYMBOL_PATTERN = Pattern.compile("\\p{Sc}");

    public static final int DEFAULT_RATE_SCALE = 8;

    private static final Map<String, String> CURRENCY_SYMBOL_MAP = Map.ofEntries(
            Map.entry("USD", "$"), Map.entry("CAD", "$"), Map.entry("AUD", "$"),
            Map.entry("NZD", "$"), Map.entry("SGD", "$"), Map.entry("HKD", "$"), Map.entry("MXN", "$"),
            Map.entry("EUR", "\u20ac"), Map.entry("GBP", "\u00a3"),
            Map.entry("JPY", "\u00a5"), Map.entry("CNY", "\u00a5"),
            Map.entry("VND", "\u20ab"), Map.entry("KRW", "\u20a9"), Map.entry("INR", "\u20b9"),
            Map.entry("THB", "\u0e3f"), Map.entry("RUB", "\u20bd"), Map.entry("BRL", "R$"),
            Map.entry("CHF", "CHF"), Map.entry("SEK", "kr"), Map.entry("NOK", "kr"), Map.entry("DKK", "kr"),
            Map.entry("PLN", "z\u0142"), Map.entry("ILS", "\u20aa"), Map.entry("ZAR", "R"),
            Map.entry("AED", "\u062f.\u0625"), Map.entry("SAR", "\ufdfc"), Map.entry("TWD", "NT$"),
            Map.entry("PHP", "\u20b1"), Map.entry("MYR", "RM"), Map.entry("IDR", "Rp"), Map.entry("TRY", "\u20ba")
    );

    private static final Map<String, String> SYMBOL_TO_CODE_MAP = Map.of(
            "\u20ac", "EUR", "\u00a3", "GBP", "\u00a5", "JPY", "\u20ab", "VND",
            "\u20a9", "KRW", "\u20b9", "INR", "\u0e3f", "THB", "\u20bd", "RUB"
    );

    private CurrencyUtils() {
    }

    public static String normalize(String currencyCode) {
        if (currencyCode == null) {
            return "";
        }
        String normalized = currencyCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? "" : normalized;
    }

    public static String normalizeOrNull(String currencyCode) {
        if (currencyCode == null) {
            return null;
        }
        String normalized = currencyCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    public static String extractFirstCode(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = CURRENCY_CODE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return normalizeOrNull(matcher.group(1));
    }

    public static String extractToken(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String trimmed = text.trim();
        if ("FREE".equalsIgnoreCase(trimmed)) {
            return "";
        }
        Matcher prefix = PRICE_TOKEN_PATTERN.matcher(trimmed);
        while (prefix.find()) {
            String token = safeTrim(prefix.group(1));
            if (!token.isEmpty()) {
                return token;
            }
        }
        Matcher suffix = PRICE_TOKEN_SUFFIX_PATTERN.matcher(trimmed);
        if (suffix.find()) {
            String token = safeTrim(suffix.group(1));
            if (!token.isEmpty()) {
                return token;
            }
        }
        Matcher symbolMatcher = CURRENCY_SYMBOL_PATTERN.matcher(trimmed);
        if (symbolMatcher.find()) {
            return symbolMatcher.group();
        }
        Matcher codeMatcher = CURRENCY_CODE_UPPER_PATTERN.matcher(trimmed);
        if (codeMatcher.find()) {
            return codeMatcher.group(1);
        }
        return "";
    }

    public static String resolveCodeFromToken(String token, String fallbackCurrency) {
        String normalized = normalize(token);
        if (normalized.length() == 3) {
            return normalized;
        }
        String lettersOnly = normalized.replaceAll("[^A-Z]", "");
        if (lettersOnly.length() == 3) {
            return lettersOnly;
        }
        if ("$".equals(token)) {
            String fallback = normalize(fallbackCurrency);
            return fallback.isEmpty() ? "USD" : fallback;
        }
        String mappedCode = SYMBOL_TO_CODE_MAP.get(token);
        if (mappedCode != null) {
            return mappedCode;
        }
        String fallback = normalize(fallbackCurrency);
        return fallback.isEmpty() ? "USD" : fallback;
    }

    public static String resolveCodeFromText(String priceText, String fallbackCurrency) {
        String token = extractToken(priceText);
        return resolveCodeFromToken(token, fallbackCurrency);
    }

    public static String getSymbol(String currencyCode) {
        if (currencyCode == null) {
            return "";
        }
        String normalized = currencyCode.toUpperCase(Locale.ROOT);
        String symbol = CURRENCY_SYMBOL_MAP.get(normalized);
        return symbol != null ? symbol : currencyCode;
    }

    public static BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency,
                                     BigDecimal fromRate, BigDecimal toRate) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        String from = normalize(fromCurrency);
        String to = normalize(toCurrency);
        if (from.isEmpty()) from = "USD";
        if (to.isEmpty()) to = "USD";
        if (from.equals(to)) {
            return amount;
        }
        if (fromRate == null || toRate == null) {
            throw new IllegalStateException("Missing publish rate. from=" + from + ", to=" + to);
        }
        if (fromRate.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Publish rate is zero for currency: " + from);
        }
        if (toRate.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Publish rate is zero for currency: " + to);
        }
        BigDecimal result = amount.multiply(toRate).divide(fromRate, DEFAULT_RATE_SCALE, RoundingMode.HALF_UP);
        logger.debug("convert() >> amount={}, from={}, to={} << result={} (fromRate={}, toRate={})",
                amount, from, to, result, fromRate, toRate);
        return result;
    }

    public static BigDecimal parseRateValue(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        BigDecimal value = PriceUtils.parsePrice(text);
        return value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
    }

    private static String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }
}
