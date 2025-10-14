package com.jobforge.jobboard.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

// Get the public path prefixes that should be ignored by the JWT filter from the application.yml config
// Look for the public-paths key under "security".
@Component
@ConfigurationProperties(prefix = "security")
@Data
public class PublicPaths {private List<String> publicPaths;}