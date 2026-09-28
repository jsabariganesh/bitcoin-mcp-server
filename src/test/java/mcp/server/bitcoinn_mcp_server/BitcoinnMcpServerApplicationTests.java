package mcp.server.bitcoinn_mcp_server;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BitcoinnMcpServerApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BitcoinServiceClient bitcoinServiceClient;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void contextLoads() {
        assertNotNull(bitcoinServiceClient);
        assertNotNull(meterRegistry);

        Map<String, MeterRegistry> registries = applicationContext.getBeansOfType(MeterRegistry.class);
        System.out.println("=== Registries in Context ===");
        registries.forEach((name, reg) -> {
            System.out.println("Bean name: " + name + ", class: " + reg.getClass().getName());
            System.out.println("  find total requests counter: " + reg.find("mcp.bitcoin.requests.total").counter());
        });
        System.out.println("Injected meterRegistry is class: " + meterRegistry.getClass().getName());
    }
}
