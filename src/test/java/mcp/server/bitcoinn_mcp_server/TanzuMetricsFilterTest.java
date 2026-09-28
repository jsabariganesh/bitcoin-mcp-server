package mcp.server.bitcoinn_mcp_server;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import mcp.server.bitcoinn_mcp_server.metrics.TanzuMetricsFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TanzuMetricsFilterTest {

    private Meter.Id createMeterId(String name) {
        return new Meter.Id(name, Tags.empty(), null, null, Meter.Type.COUNTER);
    }

    @Test
    void testIsJvmOrSystemMetric() {
        // JVM metrics
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("jvm.memory.used"));
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("jvm.gc.pause"));
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("jvm.threads.live"));
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("jvm.buffer.count"));

        // Process metrics
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("process.cpu.usage"));
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("process.uptime"));
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("process.start.time"));

        // System metrics
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("system.cpu.count"));
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("system.load.average.1m"));

        // Disk metrics
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("disk.free"));
        assertTrue(TanzuMetricsFilter.isJvmOrSystemMetric("disk.total"));

        // Custom application metrics (must return false)
        assertFalse(TanzuMetricsFilter.isJvmOrSystemMetric("mcp.bitcoin.requests.total"));
        assertFalse(TanzuMetricsFilter.isJvmOrSystemMetric("mcp.bitcoin.fetch.duration"));
        assertFalse(TanzuMetricsFilter.isJvmOrSystemMetric("mcp.bitcoin.price.usd"));
        assertFalse(TanzuMetricsFilter.isJvmOrSystemMetric("mcp.bitcoin.currency.requests"));
        assertFalse(TanzuMetricsFilter.isJvmOrSystemMetric("http.server.requests"));
        assertFalse(TanzuMetricsFilter.isJvmOrSystemMetric("application.ready.time"));
    }

    @Test
    void testJvmFilterAcceptance() {
        MeterFilter jvmFilter = TanzuMetricsFilter.createJvmFilter();

        assertEquals(MeterFilterReply.ACCEPT, jvmFilter.accept(createMeterId("jvm.memory.used")));
        assertEquals(MeterFilterReply.ACCEPT, jvmFilter.accept(createMeterId("process.cpu.usage")));
        assertEquals(MeterFilterReply.ACCEPT, jvmFilter.accept(createMeterId("system.cpu.count")));
        assertEquals(MeterFilterReply.ACCEPT, jvmFilter.accept(createMeterId("disk.free")));

        assertEquals(MeterFilterReply.DENY, jvmFilter.accept(createMeterId("mcp.bitcoin.requests.total")));
        assertEquals(MeterFilterReply.DENY, jvmFilter.accept(createMeterId("mcp.bitcoin.price.usd")));
    }

    @Test
    void testCustomFilterAcceptance() {
        MeterFilter customFilter = TanzuMetricsFilter.createCustomFilter();

        assertEquals(MeterFilterReply.DENY, customFilter.accept(createMeterId("jvm.memory.used")));
        assertEquals(MeterFilterReply.DENY, customFilter.accept(createMeterId("process.cpu.usage")));
        assertEquals(MeterFilterReply.DENY, customFilter.accept(createMeterId("system.cpu.count")));
        assertEquals(MeterFilterReply.DENY, customFilter.accept(createMeterId("disk.free")));

        assertEquals(MeterFilterReply.ACCEPT, customFilter.accept(createMeterId("mcp.bitcoin.requests.total")));
        assertEquals(MeterFilterReply.ACCEPT, customFilter.accept(createMeterId("mcp.bitcoin.price.usd")));
    }
}
