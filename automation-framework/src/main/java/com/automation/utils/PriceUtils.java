package com.automation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PriceUtils {
    private static final Pattern PRICE_NUMBER_PATTERN = Pattern.compile("[0-9][0-9.,]*");
    private static final Logger logger = LogManager.getLogger(PriceUtils.class);

    private PriceUtils() {
    }

    public static BigDecimal parsePrice(String priceText) {
        if (priceText == null) {
            return BigDecimal.ZERO;
        }
        String trimmed = priceText.trim();
        if (trimmed.isEmpty() || "FREE".equalsIgnoreCase(trimmed)) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = PRICE_NUMBER_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return BigDecimal.ZERO;
        }
        String normalized = normalizeNumberToken(matcher.group());
        try {
            BigDecimal result = new BigDecimal(normalized);
            logger.debug("parsePrice() >> '{}' << {}", priceText, result);
            return result;
        } catch (NumberFormatException e) {
            logger.warn("parsePrice() failed to parse: '{}'", priceText);
            return BigDecimal.ZERO;
        }
    }

    public static double parsePriceToDouble(String priceText) {
        return parsePrice(priceText).doubleValue();
    }

    public static BigDecimal roundPrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    public static double roundPrice(double price) {
        return roundPrice(BigDecimal.valueOf(price)).doubleValue();
    }

    public static List<BigDecimal> extractPriceValues(String raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "FREE".equalsIgnoreCase(trimmed)) {
            return Collections.emptyList();
        }
        List<BigDecimal> values = new ArrayList<>();
        Matcher matcher = PRICE_NUMBER_PATTERN.matcher(trimmed);
        while (matcher.find()) {
            String normalized = normalizeNumberToken(matcher.group());
            try {
                values.add(new BigDecimal(normalized));
            } catch (NumberFormatException e) {
                // ignore invalid token
            }
        }
        return values;
    }

    public static String normalizeOptionalPrice(String priceText, String referencePrice) {
        if (priceText == null) {
            return formatPriceLike(referencePrice, 0.0);
        }
        String trimmed = priceText.trim();
        if (trimmed.isEmpty() || "FREE".equalsIgnoreCase(trimmed)) {
            return formatPriceLike(referencePrice, 0.0);
        }
        return priceText;
    }

    public static String selectReferencePrice(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback;
        }
        return "";
    }

    public static String calculateDifferencePrice(String priceBefore, String priceAfter) {
        double before = parsePriceToDouble(priceBefore);
        double after = parsePriceToDouble(priceAfter);
        double difference = after - before;
        String reference = selectReferencePrice(priceAfter, priceBefore);
        return formatPriceLike(reference, difference);
    }

    public static String calculateSumPrice(String priceBefore, String priceAfter) {
        double before = parsePriceToDouble(priceBefore);
        double after = parsePriceToDouble(priceAfter);
        double sum = after + before;
        String reference = selectReferencePrice(priceAfter, priceBefore);
        return formatPriceLike(reference, sum);
    }

    public static String scalePriceText(String priceText, double multiplier) {
        if (priceText == null || priceText.trim().isEmpty()) {
            return "Invalid input";
        }
        try {
            BigDecimal base = parsePrice(priceText);
            BigDecimal factor = BigDecimal.valueOf(multiplier);
            BigDecimal result = base.multiply(factor);
            return formatPriceLike(priceText, result.doubleValue());
        } catch (NumberFormatException e) {
            return "Invalid number format";
        }
    }

    public static String calculateLineTotalFromUnit(String unitPriceText, int qty) {
        return calculateLineTotalFromUnit(unitPriceText, (double) qty);
    }

    public static String calculateLineTotalFromUnit(String unitPriceText, double qty) {
        return scalePriceText(unitPriceText, qty);
    }

    public static String calculateUnitPriceFromLineTotal(String lineTotalText, int qty) {
        return calculateUnitPriceFromLineTotal(lineTotalText, (double) qty);
    }

    public static String calculateUnitPriceFromLineTotal(String lineTotalText, double qty) {
        if (qty <= 0) {
            return scalePriceText(lineTotalText, 0);
        }
        return scalePriceText(lineTotalText, 1.0 / qty);
    }

    public static boolean priceEquals(BigDecimal actual, BigDecimal expected) {
        if (actual == null || expected == null) {
            return false;
        }
        boolean result = actual.compareTo(expected) == 0;
        if (!result) {
            logger.debug("priceEquals() mismatch: actual={}, expected={}", actual, expected);
        }
        return result;
    }

    public static boolean priceEquals(double actual, double expected) {
        return priceEquals(BigDecimal.valueOf(actual), BigDecimal.valueOf(expected));
    }

    public static String formatPriceLike(String referencePrice, double value) {
        if (referencePrice == null || referencePrice.trim().isEmpty()) {
            return String.format(Locale.US, "%.2f", value);
        }
        int firstDigit = -1;
        int lastDigit = -1;
        for (int i = 0; i < referencePrice.length(); i++) {
            if (Character.isDigit(referencePrice.charAt(i))) {
                firstDigit = i;
                break;
            }
        }
        for (int i = referencePrice.length() - 1; i >= 0; i--) {
            if (Character.isDigit(referencePrice.charAt(i))) {
                lastDigit = i;
                break;
            }
        }
        if (firstDigit == -1 || lastDigit == -1 || lastDigit < firstDigit) {
            return String.format(Locale.US, "%.2f", value);
        }
        String prefix = referencePrice.substring(0, firstDigit);
        String suffix = referencePrice.substring(lastDigit + 1);
        String numericPart = referencePrice.substring(firstDigit, lastDigit + 1);
        char decimalSeparator = '.';
        int decimals = 2;
        int lastDot = numericPart.lastIndexOf('.');
        int lastComma = numericPart.lastIndexOf(',');
        if (lastDot >= 0 || lastComma >= 0) {
            if (lastComma > lastDot) {
                decimalSeparator = ',';
                decimals = numericPart.length() - lastComma - 1;
            } else {
                decimalSeparator = '.';
                decimals = numericPart.length() - lastDot - 1;
            }
        } else {
            decimals = 0;
        }
        String formatted = String.format(Locale.US, "%." + decimals + "f", value);
        if (decimalSeparator == ',') {
            formatted = formatted.replace('.', ',');
        }
        return prefix + formatted + suffix;
    }

    private static String normalizeNumberToken(String token) {
        int lastComma = token.lastIndexOf(',');
        int lastDot = token.lastIndexOf('.');
        if (lastComma > -1 && lastDot > -1) {
            if (lastComma > lastDot) {
                return token.replace(".", "").replace(",", ".");
            }
            return token.replace(",", "");
        }
        if (lastComma > -1) {
            int digitsAfter = token.length() - lastComma - 1;
            if (digitsAfter > 0 && digitsAfter <= 2) {
                return token.replace(",", ".");
            }
            return token.replace(",", "");
        }
        return token;
    }
}
