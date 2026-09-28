# Bitcoin Model Context Protocol Server
This MCP Server provides an LLM interface for tracking Bitcoin prices using CoinGecko APIs (api.coingecko.com/api/v3). It was built with the [Spring AI MCP](https://docs.spring.io/spring-ai-mcp/reference/spring-mcp.html) project.

![Sample](images/sample.png)

## Building the Server

```bash
./mvnw clean package
```
## Building the Server

```bash
./mvnw test
```

## Configuration

You will need to supply a configuration for the server for your MCP Client. Here's what the configuration looks like for [claude_desktop_config.json](https://modelcontextprotocol.io/quickstart/user):

```
{
  "mcpServers": {
     "bitcoin-mcp-server": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/bitcoinn-mcp-server/target/bitcoinn-mcp-server-0.0.1-SNAPSHOT.jar"
      ]
    },
}
```

## Deploy on to Cloud Foundry
Login to Cloud Foundry instance 
```bash
cf push -f manifest.yml
```
### Binding to MCP Agents
Model Context Protocol (MCP) servers are lightweight programs that expose specific capabilities to AI models through a standardized interface. These servers act as bridges between LLMs and external tools, data sources, or services, allowing your AI application to perform actions like searching databases, accessing files, or calling external APIs without complex custom integrations.

### Create a user-provided service that provides the URL for an existing MCP server:
```
cf cups bitcoin-mcp-server -p '{"mcpServiceURL":"https://your-bitcoin-mcp-server.example.com"}'
```
### Bind the MCP service to your application:
```
cf bind-service ai-tool-chat bitcoin-mcp-server
```
### Restart your application:
```
cf restart ai-tool-chat
```
Your chatbot will now register with the bitcoin MCP agent, and the LLM will be able to invoke the agent's capabilities when responding to chat requests.



## Tanzu OTel Collector Metrics Integration

The Spring MCP Server exports metrics via OpenTelemetry (OTel) to the Tanzu Application Service (TAS) cell-local OpenTelemetry Collector using dual pipelines:

1. **JVM & System Metrics Pipeline** (Port `9565` - Hub)
2. **App Custom Metrics Pipeline** (Port `9566` - Edge)

### Environment Variables

| Environment Variable | Default | Description |
| :--- | :--- | :--- |
| `TANZU_METRICS_JVM_ENABLED` | `true` | Enable JVM and system metrics export to Tanzu Hub |
| `TANZU_METRICS_JVM_ENDPOINT` | `https://otel-collector:9565/v1/metrics` | JVM metrics OTel endpoint |
| `TANZU_METRICS_CUSTOM_ENABLED` | `false` | Enable application custom metrics export to Tanzu Edge |
| `TANZU_METRICS_CUSTOM_ENDPOINT` | `https://otel-collector:9566/v1/metrics` | Custom metrics OTel endpoint |

### Metric Routing Rules

- When `TANZU_METRICS_JVM_ENABLED=true`:
  - Routes all metrics matching prefixes:
    - `jvm.` (e.g. `jvm.memory.*`, `jvm.gc.*`, `jvm.threads.*`, `jvm.buffer.*`)
    - `process.` (e.g. `process.cpu.usage`, `process.uptime`, `process.start.time`)
    - `system.` (e.g. `system.cpu.*`, `system.load.*`)
    - `disk.` (e.g. `disk.free`, `disk.total`)
  - Target: `TANZU_METRICS_JVM_ENDPOINT` (`https://otel-collector:9565/v1/metrics`)

- When `TANZU_METRICS_CUSTOM_ENABLED=true`:
  - Routes application custom metrics (e.g. `mcp.bitcoin.*`)
  - Target: `TANZU_METRICS_CUSTOM_ENDPOINT` (`https://otel-collector:9566/v1/metrics`)

### Cloud Foundry Resource Attributes

The SDK automatically parses Cloud Foundry environment variables (`VCAP_APPLICATION` and CF container variables) and attaches standard resource attributes to all emitted metrics:

| Resource Attribute | Source / CF Environment Variable |
| :--- | :--- |
| `app_id` | `VCAP_APPLICATION.application_id` |
| `app_name` | `VCAP_APPLICATION.application_name` |
| `app_instance_id` | `CF_INSTANCE_GUID` |
| `instance_index` | `CF_INSTANCE_INDEX` |
| `space_id` | `VCAP_APPLICATION.space_id` |
| `space_name` | `VCAP_APPLICATION.space_name` |
| `org_id` | `VCAP_APPLICATION.organization_id` |
| `org_name` | `VCAP_APPLICATION.organization_name` |

### mTLS with Instance Identity Certificates

The server establishes mutual TLS (mTLS) with the OTel Collector using Cloud Foundry Instance Identity certificates provided by Diego:
- `CF_INSTANCE_CERT`: PEM certificate chain
- `CF_INSTANCE_KEY`: PEM private key

When running outside Cloud Foundry or in local development environments where these variables are not present, mTLS client authentication is automatically bypassed without errors.
