package mcp.server.bitcoinn_mcp_server;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "tanzu.metrics.jvm.enabled=true",
        "tanzu.metrics.jvm.endpoint=https://otel-collector:9565/v1/metrics",
        "tanzu.metrics.custom.enabled=true",
        "tanzu.metrics.custom.endpoint=https://otel-collector:9566/v1/metrics"
})
class TanzuOtelMetricsIntegrationTest {

    @Autowired
    @Qualifier("tanzuJvmOtelMeterRegistry")
    private MeterRegistry jvmOtelRegistry;

    @Autowired
    @Qualifier("tanzuCustomOtelMeterRegistry")
    private MeterRegistry customOtelRegistry;

    @Autowired
    private MeterRegistry compositeMeterRegistry;

    @Autowired
    private BitcoinServiceClient bitcoinServiceClient;

    @Test
    void testBothRegistriesActiveAndProperlyFiltered() {
        assertNotNull(jvmOtelRegistry, "JVM OTel MeterRegistry must be active");
        assertNotNull(customOtelRegistry, "Custom OTel MeterRegistry must be active");
        assertNotNull(compositeMeterRegistry, "Composite MeterRegistry must be active");

        // Record a JVM metric and an application custom metric on the composite registry
        Counter jvmCounter = compositeMeterRegistry.counter("jvm.memory.used");
        jvmCounter.increment(10.0);

        Counter customCounter = compositeMeterRegistry.counter("mcp.bitcoin.requests.total");
        customCounter.increment(1.0);

        // Verify JVM registry accepted JVM metric but denied custom metric
        assertNotNull(jvmOtelRegistry.find("jvm.memory.used").counter(),
                "JVM OTel registry should contain jvm.memory.used");
        assertNull(jvmOtelRegistry.find("mcp.bitcoin.requests.total").counter(),
                "JVM OTel registry should NOT contain mcp.bitcoin.requests.total");

        // Verify Custom registry accepted custom metric but denied JVM metric
        assertNotNull(customOtelRegistry.find("mcp.bitcoin.requests.total").counter(),
                "Custom OTel registry should contain mcp.bitcoin.requests.total");
        assertNull(customOtelRegistry.find("jvm.memory.used").counter(),
                "Custom OTel registry should NOT contain jvm.memory.used");
    }
}
