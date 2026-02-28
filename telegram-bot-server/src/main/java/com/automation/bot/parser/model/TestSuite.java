package com.automation.bot.parser.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Map từ <testsuite> root element trong Surefire XML.
 *
 * Ví dụ XML:
 * <testsuite name="com.automation.LoginTest" tests="5" failures="1" errors="0" skipped="0" time="15.5">
 *   <testcase .../>
 *   <testcase .../>
 * </testsuite>
 */
@Getter
@Setter
@JacksonXmlRootElement(localName = "testsuite")
public class TestSuite {

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private int tests;

    @JacksonXmlProperty(isAttribute = true)
    private int failures;

    @JacksonXmlProperty(isAttribute = true)
    private int errors;

    @JacksonXmlProperty(isAttribute = true)
    private int skipped;

    @JacksonXmlProperty(isAttribute = true)
    private double time;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "testcase")
    private List<TestCase> testCases = new ArrayList<>();

    public int getPassed() {
        return tests - failures - errors - skipped;
    }
}
