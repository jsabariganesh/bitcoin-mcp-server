package mcp.server.bitcoinn_mcp_server.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for Tanzu OTel metrics export.
 * Supports configuration via environment variables:
 * - TANZU_METRICS_JVM_ENABLED (default: true)
 * - TANZU_METRICS_JVM_ENDPOINT (default: https://otel-collector:9565/v1/metrics)
 * - TANZU_METRICS_CUSTOM_ENABLED (default: false)
 * - TANZU_METRICS_CUSTOM_ENDPOINT (default: https://otel-collector:9566/v1/metrics)
 *
 * Also maps to Spring properties under "tanzu.metrics.*".
 */
@ConfigurationProperties(prefix = "tanzu.metrics")
public class TanzuMetricsProperties {

    public static final String DEFAULT_JVM_ENDPOINT = "https://otel-collector:9565/v1/metrics";
    public static final String DEFAULT_CUSTOM_ENDPOINT = "https://otel-collector:9566/v1/metrics";

    private final Jvm jvm = new Jvm();
    private final Custom custom = new Custom();
    private Duration step = Duration.ofSeconds(60);
    private String caPath;

    public TanzuMetricsProperties() {
        // Read directly from system environment variables if set, overriding defaults
        String jvmEnabledEnv = System.getenv("TANZU_METRICS_JVM_ENABLED");
        if (jvmEnabledEnv != null && !jvmEnabledEnv.isBlank()) {
            this.jvm.setEnabled(Boolean.parseBoolean(jvmEnabledEnv.trim()));
        }

        String jvmEndpointEnv = System.getenv("TANZU_METRICS_JVM_ENDPOINT");
        if (jvmEndpointEnv != null && !jvmEndpointEnv.isBlank()) {
            this.jvm.setEndpoint(jvmEndpointEnv.trim());
        }

        String customEnabledEnv = System.getenv("TANZU_METRICS_CUSTOM_ENABLED");
        if (customEnabledEnv != null && !customEnabledEnv.isBlank()) {
            this.custom.setEnabled(Boolean.parseBoolean(customEnabledEnv.trim()));
        }

        String customEndpointEnv = System.getenv("TANZU_METRICS_CUSTOM_ENDPOINT");
        if (customEndpointEnv != null && !customEndpointEnv.isBlank()) {
            this.custom.setEndpoint(customEndpointEnv.trim());
        }

        String caPathEnv = System.getenv("TANZU_METRICS_CA_PATH");
        if (caPathEnv != null && !caPathEnv.isBlank()) {
            this.caPath = caPathEnv.trim();
        }
    }

    public Jvm getJvm() {
        return jvm;
    }

    public Custom getCustom() {
        return custom;
    }

    public Duration getStep() {
        return step;
    }

    public void setStep(Duration step) {
        this.step = step;
    }

    public String getCaPath() {
        return caPath;
    }

    public void setCaPath(String caPath) {
        this.caPath = caPath;
    }

    public static class Jvm {
        private boolean enabled = true;
        private String endpoint = DEFAULT_JVM_ENDPOINT;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }

    public static class Custom {
        private boolean enabled = false;
        private String endpoint = DEFAULT_CUSTOM_ENDPOINT;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }
}
