package com.covenantiq.controller;

import com.covenantiq.config.AppModeProperties;
import com.covenantiq.dto.response.RuntimeConfigResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/runtime-config")
public class RuntimeConfigController {

    private final AppModeProperties appModeProperties;

    public RuntimeConfigController(AppModeProperties appModeProperties) {
        this.appModeProperties = appModeProperties;
    }

    @GetMapping
    public RuntimeConfigResponse getRuntimeConfig() {
        return new RuntimeConfigResponse(
                appModeProperties.backendMode().name(),
                appModeProperties.isDemoEnabled(),
                appModeProperties.isTestEnabled(),
                appModeProperties.sampleContentAvailable(),
                appModeProperties.strictSecretValidationEnabled()
        );
    }
}
