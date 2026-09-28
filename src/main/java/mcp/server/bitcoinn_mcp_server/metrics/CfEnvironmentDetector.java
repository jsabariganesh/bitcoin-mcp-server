package mcp.server.bitcoinn_mcp_server.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Detects Cloud Foundry (TAS) container environment variables and parses
 * VCAP_APPLICATION metadata and instance identity certificates.
 */
public class CfEnvironmentDetector {

    private static final Logger log = LoggerFactory.getLogger(CfEnvironmentDetector.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, String> env;
    private final Map<String, String> resourceAttributes;
    private final String certPath;
    private final String keyPath;

    public CfEnvironmentDetector() {
        this(System.getenv());
    }

    public CfEnvironmentDetector(Map<String, String> env) {
        this.env = env != null ? env : Collections.emptyMap();
        this.resourceAttributes = parseResourceAttributes();
        this.certPath = resolveCertPath();
        this.keyPath = resolveKeyPath();
    }

    private Map<String, String> parseResourceAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>();

        // Parse VCAP_APPLICATION JSON if present
        String vcapApp = env.get("VCAP_APPLICATION");
        if (vcapApp != null && !vcapApp.isBlank()) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(vcapApp);
                putIfPresent(attrs, "app_id", root.path("application_id").asText(null));
                putIfPresent(attrs, "app_name", root.path("application_name").asText(null));
                putIfPresent(attrs, "space_id", root.path("space_id").asText(null));
                putIfPresent(attrs, "space_name", root.path("space_name").asText(null));
                putIfPresent(attrs, "org_id", root.path("organization_id").asText(null));
                putIfPresent(attrs, "org_name", root.path("organization_name").asText(null));
            } catch (Exception e) {
                log.warn("Failed to parse VCAP_APPLICATION environment variable: {}", e.getMessage());
            }
        }

        // Parse container instance environment variables
        putIfPresent(attrs, "app_instance_id", env.get("CF_INSTANCE_GUID"));
        putIfPresent(attrs, "instance_index", env.get("CF_INSTANCE_INDEX"));

        // Fallback for local development or non-CF environment if app_name is not yet set
        if (!attrs.containsKey("app_name")) {
            String fallbackName = env.getOrDefault("SPRING_APPLICATION_NAME", "bitcoin-mcp-server");
            attrs.put("app_name", fallbackName);
        }

        return Collections.unmodifiableMap(attrs);
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
            map.put(key, value.trim());
        }
    }

    private String resolveCertPath() {
        String path = env.get("CF_INSTANCE_CERT");
        if (path != null && !path.isBlank()) {
            File f = new File(path.trim());
            if (f.exists() && f.canRead()) {
                return f.getAbsolutePath();
            } else {
                log.warn("CF_INSTANCE_CERT specified ({}) but file does not exist or is unreadable", path);
            }
        }
        return null;
    }

    private String resolveKeyPath() {
        String path = env.get("CF_INSTANCE_KEY");
        if (path != null && !path.isBlank()) {
            File f = new File(path.trim());
            if (f.exists() && f.canRead()) {
                return f.getAbsolutePath();
            } else {
                log.warn("CF_INSTANCE_KEY specified ({}) but file does not exist or is unreadable", path);
            }
        }
        return null;
    }

    public Map<String, String> getResourceAttributes() {
        return resourceAttributes;
    }

    public boolean hasInstanceIdentity() {
        return certPath != null && keyPath != null;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public byte[] getCertBytes() throws IOException {
        return certPath != null ? Files.readAllBytes(new File(certPath).toPath()) : null;
    }

    public byte[] getKeyBytes() throws IOException {
        return keyPath != null ? Files.readAllBytes(new File(keyPath).toPath()) : null;
    }
}
