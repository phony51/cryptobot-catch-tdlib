package org.topsmoker.cryptobot.config;


import lombok.Getter;

import jakarta.xml.bind.annotation.*;

@Getter
@XmlRootElement(name = "Config")
@XmlAccessorType(XmlAccessType.FIELD)
public class Config {
    @XmlElement(name = "ApiId", required = true)
    private int apiId;
    @XmlElement(name = "ApiHash", required = true)
    private String apiHash;

    @Getter
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Catcher {
        @XmlElement(name = "Credentials")
        private Credentials credentials;
        @XmlElement(name = "RegexThreadsCount")
        private int regexThreadsCount;
        @XmlElement(name = "InlineThreadsCount")
        private int inlineThreadsCount;
        @XmlElement(name = "PollingPeriodMs")
        private long pollingPeriodMs;
        @XmlElement(name = "PollingTimeoutMs")
        private long pollingTimeoutMs;
    }

    @Getter
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Activator {
        @XmlElement(name = "Credentials")
        private Credentials credentials;
        @XmlElement(name = "RetryCount")
        private int retryCount;
    }

    @XmlElement(name = "Catcher")
    private Catcher catcher;

    @XmlElement(name = "Activator")
    private Activator activator;
}



