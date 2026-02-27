package com.automation.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

@LoadPolicy(LoadType.MERGE)
@Sources({
        "system:properties",
        "system:env",
        "classpath:config/${env}.properties",
        "classpath:config/config.properties"
})
public interface FrameworkConfig extends Config {

    @DefaultValue("chrome")
    @Key("browser")
    String browser();

    @DefaultValue("https://admin-dev.burgershop.io")
    @Key("base.url")
    String baseUrl();

    @DefaultValue("false")
    @Key("headless")
    boolean headless();

    @DefaultValue("dev")
    @Key("env")
    String environment();

    @DefaultValue("false")
    @Key("remote")
    boolean isRemote();

    @DefaultValue("http://localhost:4444/wd/hub")
    @Key("remote.url")
    String remoteUrl();

    @DefaultValue("1920")
    @Key("browser.width")
    int browserWidth();

    @DefaultValue("1080")
    @Key("browser.height")
    int browserHeight();

    @DefaultValue("true")
    @Key("maximize")
    boolean maximize();

    @DefaultValue("1")
    @Key("retry.count")
    int retryCount();

    @DefaultValue("true")
    @Key("screenshot.on.failure")
    boolean screenshotOnFailure();

    @Key("admin.email")
    String adminEmail();

    @Key("admin.password")
    String adminPassword();

    @DefaultValue("true")
    @Key("session.reuse")
    boolean sessionReuse();

    @Key("store.name")
    String storeName();

    @DefaultValue("")
    @Key("buyer.url")
    String buyerUrl();
}
