package mcp.server.bitcoinn_mcp_server;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

@Service
public class BitcoinServiceClient {

    private static final Logger log = LoggerFactory.getLogger(BitcoinServiceClient.class);
    private static final String BASE_URL = "https://api.coingecko.com/api/v3";

    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;

    private final Counter totalRequestsCounter;
    private final Counter failedRequestsCounter;
    private final Timer priceFetchTimer;
    private final AtomicReference<Double> latestUsdPriceGauge;
    private final Map<String, Counter> currencyCounterMap = new ConcurrentHashMap<>();

    public BitcoinServiceClient(MeterRegistry meterRegistry) {
        this.restTemplate = new RestTemplate();
        this.meterRegistry = meterRegistry;

        this.totalRequestsCounter = Counter.builder("mcp.bitcoin.requests.total")
                .description("Total number of Bitcoin price requests received by MCP server")
                .register(meterRegistry);

        this.failedRequestsCounter = Counter.builder("mcp.bitcoin.requests.failed")
                .description("Total number of failed Bitcoin price requests")
                .register(meterRegistry);

        this.priceFetchTimer = Timer.builder("mcp.bitcoin.fetch.duration")
                .description("Time taken to fetch Bitcoin price from upstream API")
                .publishPercentileHistogram()
                .register(meterRegistry);

        this.latestUsdPriceGauge = new AtomicReference<>(0.0);
        meterRegistry.gauge("mcp.bitcoin.price.usd", latestUsdPriceGauge, ref -> ref.get() != null ? ref.get() : 0.0);
    }

    private Counter getCurrencyCounter(String currency) {
        return currencyCounterMap.computeIfAbsent(currency.toLowerCase(), c ->
                Counter.builder("mcp.bitcoin.currency.requests")
                        .tag("currency", c)
                        .description("Number of Bitcoin requests by target currency")
                        .register(meterRegistry)
        );
    }

    @Tool(description = "Get the current price of Bitcoin in a fiat currency (such as USD, EUR, GBP). Defaults to USD if no currency is specified.")
    public Integer getBitcoinPrice(
            @ToolParam(description = "The target fiat currency symbol, e.g. USD, EUR, GBP (default is USD)", required = false)
            String currency) {
        String curr = (currency != null && !currency.isBlank()) ? currency.trim().toLowerCase() : "usd";
        totalRequestsCounter.increment();
        getCurrencyCounter(curr).increment();

        return priceFetchTimer.record(() -> {
            try {
                String url = BASE_URL + "/simple/price?ids=bitcoin&vs_currencies=" + curr;
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response == null || !response.containsKey("bitcoin")) {
                    log.warn("Empty or invalid response for currency {}: {}", curr, response);
                    failedRequestsCounter.increment();
                    return null;
                }
                Map<String, Object> bitcoinData = (Map<String, Object>) response.get("bitcoin");
                Object priceObject = bitcoinData.get(curr);
                if (priceObject instanceof Number num) {
                    if ("usd".equals(curr)) {
                        latestUsdPriceGauge.set(num.doubleValue());
                    }
                    return num.intValue();
                }
                return null;
            } catch (Exception e) {
                log.error("Error fetching Bitcoin price for currency {}: {}", curr, e.getMessage());
                failedRequestsCounter.increment();
                throw e;
            }
        });
    }

    public Integer getBitcoinPrice() {
        return getBitcoinPrice("usd");
    }

    public Integer getBitcoinPriceByCurrency(String currency) {
        return getBitcoinPrice(currency);
    }
}
