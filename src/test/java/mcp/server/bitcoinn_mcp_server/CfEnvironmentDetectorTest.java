package mcp.server.bitcoinn_mcp_server;

import mcp.server.bitcoinn_mcp_server.metrics.CfEnvironmentDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CfEnvironmentDetectorTest {

    @Test
    void testParseFullCfEnvironment() {
        String vcapApp = "{"
                + "\"application_id\": \"app-guid-1234\","
                + "\"application_name\": \"test-bitcoin-server\","
                + "\"space_id\": \"space-guid-5678\","
                + "\"space_name\": \"development\","
                + "\"organization_id\": \"org-guid-9012\","
                + "\"organization_name\": \"prod-org\""
                + "}";

        Map<String, String> mockEnv = new HashMap<>();
        mockEnv.put("VCAP_APPLICATION", vcapApp);
        mockEnv.put("CF_INSTANCE_GUID", "instance-guid-abcd");
        mockEnv.put("CF_INSTANCE_INDEX", "0");

        CfEnvironmentDetector detector = new CfEnvironmentDetector(mockEnv);
        Map<String, String> attrs = detector.getResourceAttributes();

        assertEquals("app-guid-1234", attrs.get("app_id"));
        assertEquals("test-bitcoin-server", attrs.get("app_name"));
        assertEquals("instance-guid-abcd", attrs.get("app_instance_id"));
        assertEquals("0", attrs.get("instance_index"));
        assertEquals("space-guid-5678", attrs.get("space_id"));
        assertEquals("development", attrs.get("space_name"));
        assertEquals("org-guid-9012", attrs.get("org_id"));
        assertEquals("prod-org", attrs.get("org_name"));
    }

    @Test
    void testNonCfEnvironmentFallback() {
        Map<String, String> mockEnv = new HashMap<>();
        mockEnv.put("SPRING_APPLICATION_NAME", "local-app");

        CfEnvironmentDetector detector = new CfEnvironmentDetector(mockEnv);
        Map<String, String> attrs = detector.getResourceAttributes();

        assertNull(attrs.get("app_id"));
        assertEquals("local-app", attrs.get("app_name"));
        assertNull(attrs.get("space_id"));
        assertFalse(detector.hasInstanceIdentity());
    }

    @Test
    void testMalformedVcapApplication() {
        Map<String, String> mockEnv = new HashMap<>();
        mockEnv.put("VCAP_APPLICATION", "{ invalid json ");
        mockEnv.put("CF_INSTANCE_GUID", "guid-1");

        CfEnvironmentDetector detector = new CfEnvironmentDetector(mockEnv);
        Map<String, String> attrs = detector.getResourceAttributes();

        assertEquals("guid-1", attrs.get("app_instance_id"));
        assertNull(attrs.get("app_id"));
    }

    @Test
    void testInstanceIdentityCertResolution(@TempDir Path tempDir) throws Exception {
        Path certFile = tempDir.resolve("instance.crt");
        Path keyFile = tempDir.resolve("instance.key");

        Files.writeString(certFile, "-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----");
        Files.writeString(keyFile, "-----BEGIN PRIVATE KEY-----\ntest\n-----END PRIVATE KEY-----");

        Map<String, String> mockEnv = new HashMap<>();
        mockEnv.put("CF_INSTANCE_CERT", certFile.toString());
        mockEnv.put("CF_INSTANCE_KEY", keyFile.toString());

        CfEnvironmentDetector detector = new CfEnvironmentDetector(mockEnv);

        assertTrue(detector.hasInstanceIdentity());
        assertEquals(certFile.toAbsolutePath().toString(), detector.getCertPath());
        assertEquals(keyFile.toAbsolutePath().toString(), detector.getKeyPath());
        assertNotNull(detector.getCertBytes());
        assertNotNull(detector.getKeyBytes());
    }

    @Test
    void testMissingCertFilesIgnored() {
        Map<String, String> mockEnv = new HashMap<>();
        mockEnv.put("CF_INSTANCE_CERT", "/non/existent/path/instance.crt");
        mockEnv.put("CF_INSTANCE_KEY", "/non/existent/path/instance.key");

        CfEnvironmentDetector detector = new CfEnvironmentDetector(mockEnv);
        assertFalse(detector.hasInstanceIdentity());
        assertNull(detector.getCertPath());
        assertNull(detector.getKeyPath());
    }
}
