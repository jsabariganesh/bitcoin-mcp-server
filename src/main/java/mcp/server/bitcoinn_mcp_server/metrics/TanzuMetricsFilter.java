package mcp.server.bitcoinn_mcp_server.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;

import java.util.List;

/**
 * Filters metrics between JVM/system metrics and application custom metrics.
 *
 * JVM/system metric prefixes:
 * - "jvm."     (e.g., jvm.memory., jvm.gc., jvm.threads., jvm.buffer.)
 * - "process." (e.g., process.cpu.usage, process.uptime, process.start.time)
 * - "system."  (e.g., system.cpu., system.load.)
 * - "disk."    (e.g., disk.free, disk.total)
 *
 * All other metrics are considered app custom metrics (e.g., mcp.bitcoin.*).
 */
public final class TanzuMetricsFilter {

    private static final List<String> JVM_PREFIXES = List.of(
            "jvm.",
            "process.",
            "system.",
            "disk."
    );

    private TanzuMetricsFilter() {}

    public static boolean isJvmOrSystemMetric(String metricName) {
        if (metricName == null || metricName.isBlank()) {
            return false;
        }
        for (String prefix : JVM_PREFIXES) {
            if (metricName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Accepts only metrics prefixed with jvm., process., system., or disk.
     * Denies all other metrics.
     */
    public static MeterFilter createJvmFilter() {
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                return isJvmOrSystemMetric(id.getName()) ? MeterFilterReply.ACCEPT : MeterFilterReply.DENY;
            }
        };
    }

    /**
     * Accepts only app custom metrics (denies jvm., process., system., disk.).
     */
    public static MeterFilter createCustomFilter() {
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                return isJvmOrSystemMetric(id.getName()) ? MeterFilterReply.DENY : MeterFilterReply.ACCEPT;
            }
        };
    }
}
