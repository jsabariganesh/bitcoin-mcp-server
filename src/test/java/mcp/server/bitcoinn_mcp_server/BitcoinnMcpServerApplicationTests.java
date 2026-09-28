package mcp.server.bitcoinn_mcp_server;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BitcoinnMcpServerApplicationTests {

    @Autowired
    private BitcoinServiceClient bitcoinServiceClient;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void contextLoads() {
        assertNotNull(bitcoinServiceClient);
        assertNotNull(meterRegistry);
        assertNotNull(meterRegistry.find("mcp.bitcoin.requests.total").counter());
        assertNotNull(meterRegistry.find("mcp.bitcoin.requests.failed").counter());
        assertNotNull(meterRegistry.find("mcp.bitcoin.fetch.duration").timer());
    }
}
