package com.automation.bot.parser.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Map từ <testcase> element trong Surefire XML.
 *
 * Ví dụ XML:
 * <testcase name="testLogin" classname="com.automation.LoginTest" time="5.123">
 *   <failure message="Expected 200 but got 401" type="AssertionError">stacktrace...</failure>
 * </testcase>
 */
@Getter
@Setter
public class TestCase {

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String classname;

    @JacksonXmlProperty(isAttribute = true)
    private double time;

    /** Null nếu test passed */
    private Failure failure;

    /** Null nếu test passed (error = exception không phải assertion) */
    private Failure error;

    /** Non-null nếu test bị skip */
    private String skipped;

    public boolean isPassed() {
        return failure == null && error == null && skipped == null;
    }

    public boolean isFailed() {
        return failure != null;
    }

    public boolean isError() {
        return error != null;
    }

    public boolean isSkipped() {
        return skipped != null;
    }

    @Getter
    @Setter
    public static class Failure {
        @JacksonXmlProperty(isAttribute = true)
        private String message;

        @JacksonXmlProperty(isAttribute = true)
        private String type;
    }
}
