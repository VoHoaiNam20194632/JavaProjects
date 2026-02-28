package com.automation.bot.parser;

import com.automation.bot.parser.model.TestSuite;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SurefireReportParserTest {

    private final SurefireReportParser parser = new SurefireReportParser();

    @Test
    void parseValidXml(@TempDir Path tempDir) throws Exception {
        // Tạo thư mục target/surefire-reports
        Path reportsDir = tempDir.resolve("target/surefire-reports");
        Files.createDirectories(reportsDir);

        // Tạo file XML giả lập
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="com.automation.LoginTest" tests="3" failures="1" errors="0" skipped="0" time="10.5">
                    <testcase name="testLoginSuccess" classname="com.automation.LoginTest" time="3.2"/>
                    <testcase name="testLoginFailed" classname="com.automation.LoginTest" time="4.1">
                        <failure message="Expected 200 but got 401" type="AssertionError"/>
                    </testcase>
                    <testcase name="testLoginEmpty" classname="com.automation.LoginTest" time="3.2"/>
                </testsuite>
                """;

        Files.writeString(reportsDir.resolve("TEST-com.automation.LoginTest.xml"), xml);

        List<TestSuite> suites = parser.parseReports(tempDir.toString());
        assertEquals(1, suites.size());

        TestSuite suite = suites.get(0);
        assertEquals("com.automation.LoginTest", suite.getName());
        assertEquals(3, suite.getTests());
        assertEquals(1, suite.getFailures());
        assertEquals(0, suite.getErrors());
        assertEquals(2, suite.getPassed());
        assertEquals(3, suite.getTestCases().size());
    }

    @Test
    void parseEmptyDirectory(@TempDir Path tempDir) throws Exception {
        Path reportsDir = tempDir.resolve("target/surefire-reports");
        Files.createDirectories(reportsDir);

        List<TestSuite> suites = parser.parseReports(tempDir.toString());
        assertTrue(suites.isEmpty());
    }

    @Test
    void parseNonExistentDirectory() {
        List<TestSuite> suites = parser.parseReports("/nonexistent/path");
        assertTrue(suites.isEmpty());
    }

    @Test
    void getFailedTests(@TempDir Path tempDir) throws Exception {
        Path reportsDir = tempDir.resolve("target/surefire-reports");
        Files.createDirectories(reportsDir);

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="com.automation.LoginTest" tests="2" failures="1" errors="0" skipped="0" time="5.0">
                    <testcase name="testOk" classname="com.automation.LoginTest" time="2.0"/>
                    <testcase name="testFail" classname="com.automation.LoginTest" time="3.0">
                        <failure message="assertion failed" type="AssertionError"/>
                    </testcase>
                </testsuite>
                """;

        Files.writeString(reportsDir.resolve("TEST-com.automation.LoginTest.xml"), xml);

        List<TestSuite> suites = parser.parseReports(tempDir.toString());
        var failedTests = parser.getFailedTests(suites);
        assertEquals(1, failedTests.size());
        assertEquals("testFail", failedTests.get(0).getName());
    }
}
