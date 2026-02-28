package com.automation.bot.command.impl;

import com.automation.bot.allure.AllureReportGenerator;
import com.automation.bot.bot.BotMessageSender;
import com.automation.bot.command.AbstractTestCommand;
import com.automation.bot.config.TestRunnerProperties;
import com.automation.bot.notification.TelegramNotifier;
import com.automation.bot.parser.SurefireReportParser;
import com.automation.bot.runner.TestRunQueue;
import com.automation.bot.runner.TestRunner;
import com.automation.bot.session.UserSessionManager;
import org.springframework.stereotype.Component;

@Component
public class RegressionCommand extends AbstractTestCommand {

    public RegressionCommand(BotMessageSender messageSender,
                             UserSessionManager sessionManager,
                             TestRunnerProperties runnerProperties,
                             TestRunner testRunner,
                             TestRunQueue testRunQueue,
                             TelegramNotifier notifier,
                             SurefireReportParser reportParser,
                             AllureReportGenerator allureGenerator) {
        super(messageSender, sessionManager, runnerProperties, testRunner, testRunQueue,
                notifier, reportParser, allureGenerator);
    }

    @Override
    public String name() {
        return "regression";
    }

    @Override
    public String description() {
        return "Run full regression test suite";
    }

    @Override
    protected String profile() {
        return "regression";
    }

    @Override
    protected String testClass() {
        return null;
    }
}
