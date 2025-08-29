package org.topsmoker.cryptobot.config;

import jakarta.xml.bind.annotation.*;

@XmlRootElement(name = "Config")
@XmlAccessorType(XmlAccessType.FIELD)
public class Config {
    @XmlElement(name = "ApiId", required = true)
    private int apiId;
    @XmlElement(name = "ApiHash", required = true)
    private String apiHash;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Catcher {
        @XmlElement(name = "Credentials")
        private Credentials credentials;
        @XmlElement(name = "RegexThreadsCount")
        private int regexThreadsCount;
        @XmlElement(name = "PollingPeriodMs")
        private long pollingPeriodMs;
        @XmlElement(name = "PollingTimeoutMs")
        private long pollingTimeoutMs;

        public Credentials getCredentials() {
            return credentials;
        }

        public int getRegexThreadsCount() {
            return regexThreadsCount;
        }

        public long getPollingPeriodMs() {
            return pollingPeriodMs;
        }

        public long getPollingTimeoutMs() {
            return pollingTimeoutMs;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Activator {
        @XmlElement(name = "Credentials")
        private Credentials credentials;

        public Credentials getCredentials() {
            return credentials;
        }
    }

    @XmlElement(name = "Catcher")
    private Catcher catcher;

    @XmlElement(name = "Activator")
    private Activator activator;

    public int getApiId() {
        return apiId;
    }

    public String getApiHash() {
        return apiHash;
    }

    public Catcher getCatcher() {
        return catcher;
    }

    public Activator getActivator() {
        return activator;
    }
}