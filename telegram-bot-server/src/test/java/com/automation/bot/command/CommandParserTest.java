package com.automation.bot.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    private final CommandParser parser = new CommandParser();

    @Test
    void parseSimpleCommand() {
        CommandParser.ParsedCommand result = parser.parse("/smoke");
        assertNotNull(result);
        assertEquals("smoke", result.name());
        assertEquals("", result.args());
    }

    @Test
    void parseCommandWithArgs() {
        CommandParser.ParsedCommand result = parser.parse("/smoke dev");
        assertNotNull(result);
        assertEquals("smoke", result.name());
        assertEquals("dev", result.args());
    }

    @Test
    void parseCommandWithBotMention() {
        CommandParser.ParsedCommand result = parser.parse("/smoke@MyTestBot dev");
        assertNotNull(result);
        assertEquals("smoke", result.name());
        assertEquals("dev", result.args());
    }

    @Test
    void parseCommandUpperCase() {
        CommandParser.ParsedCommand result = parser.parse("/SMOKE Dev");
        assertNotNull(result);
        assertEquals("smoke", result.name());
        assertEquals("Dev", result.args());
    }

    @Test
    void parseNonCommand() {
        assertNull(parser.parse("hello"));
        assertNull(parser.parse(""));
        assertNull(parser.parse(null));
    }

    @Test
    void parseCommandWithMultipleArgs() {
        CommandParser.ParsedCommand result = parser.parse("/env dev staging");
        assertNotNull(result);
        assertEquals("env", result.name());
        assertEquals("dev staging", result.args());
    }
}
