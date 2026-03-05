package com.covenantiq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mode")
public class AppModeProperties {

    private boolean demoEnabled;
    private boolean testEnabled;

    public boolean isDemoEnabled() {
        return demoEnabled;
    }

    public void setDemoEnabled(boolean demoEnabled) {
        this.demoEnabled = demoEnabled;
    }

    public boolean isTestEnabled() {
        return testEnabled;
    }

    public void setTestEnabled(boolean testEnabled) {
        this.testEnabled = testEnabled;
    }

    public BackendMode backendMode() {
        if (testEnabled) {
            return BackendMode.TEST;
        }
        if (demoEnabled) {
            return BackendMode.DEMO;
        }
        return BackendMode.NORMAL;
    }

    public boolean sampleContentAvailable() {
        return demoEnabled || testEnabled;
    }

    public boolean strictSecretValidationEnabled() {
        return !sampleContentAvailable();
    }
}
