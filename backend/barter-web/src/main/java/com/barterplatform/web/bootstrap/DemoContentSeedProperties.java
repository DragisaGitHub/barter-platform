package com.barterplatform.web.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls whether demo/business content (categories, tags, sample data) is seeded
 * on application startup.
 *
 * <p>Disabled by default. Production databases start completely clean — admins
 * create categories, tags, and catalog content manually after launch.
 *
 * <p>Enable for local/dev/test environments via:
 * <pre>
 *   barter.seed.demo-content=true
 * </pre>
 * or environment variable:
 * <pre>
 *   BARTER_SEED_DEMO_CONTENT=true
 * </pre>
 */
@ConfigurationProperties(prefix = "barter.seed")
public record DemoContentSeedProperties(
        boolean demoContent
) {
}
