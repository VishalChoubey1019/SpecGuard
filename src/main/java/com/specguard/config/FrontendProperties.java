package com.specguard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** Binds {@code specguard.frontend.*} config from environment variables (see .env.example). */
@Component
@ConfigurationProperties(prefix = "specguard.frontend")
@Data
public class FrontendProperties {

    /** Origins allowed to call this API from the browser (CORS). Comma-separated in .env. */
    private List<String> origins;
}