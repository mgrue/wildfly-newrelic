package ch.performancebuildings.newrelic;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RequestDumpingHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(RequestDumpingHandler.class);

    private static final String hostname;

    private String licenseKey;

    private String source;

    private String baseURI = "https://log-api.eu.newrelic.com/log/v1";

    private final HttpHandler next;

    static {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public RequestDumpingHandler(final HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        // dump the request once the exchange is completed so the response is available
        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
            if (licenseKey == null) {
                throw new IllegalStateException("licenseKey == null");
            }

            try {
                final JsonObject obj = buildJson(exchange);

                final HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(baseURI))
                        .setHeader("Content-Type", "application/json")
                        .setHeader("Api-Key", licenseKey)
                        .POST(HttpRequest.BodyPublishers.ofString(obj.toString()))
                        .build();

                final HttpClient client = HttpClient.newBuilder().build();
                final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (LOG.isDebugEnabled() || response.statusCode() != 202) {
                    LOG.log(response.statusCode() == 202 ? Level.DEBUG : Level.ERROR,
                            String.format("Got %d from %s. Response body: '%s', Request body: '%s'",
                                    response.statusCode(),
                                    baseURI,
                                    response.body(),
                                    obj));
                }

            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }

            finally {
                nextListener.proceed();
            }
        });

        next.handleRequest(exchange);
    }

    @SuppressWarnings("unused")
    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
        LOG.debug("Newrelic license key set");
    }

    @SuppressWarnings("unused")
    public void setSource(String source) {
        this.source = source;
    }

    @SuppressWarnings("unused")
    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
        LOG.debug("Newrelic base uri: " + baseURI);
    }

    private String getSourceOrHost() {
        return source == null ? hostname : source;
    }

    private JsonObject buildJson(HttpServerExchange exchange) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("timestamp", System.currentTimeMillis())
                .add("source", getSourceOrHost());

        if (exchange.getRequestURI() == null) {
            LOG.debug("requestURI == null");
        } else {
            builder.add("requestURI", exchange.getRequestURI());
        }

        if (exchange.getQueryString() == null) {
            LOG.debug("queryString == null");
        }

        else {
            builder.add("queryString", exchange.getQueryString());
        }

        if (exchange.getConnection() == null) {
            LOG.debug("connection == null");
        }

        else {
            if(exchange.getConnection().getLocalAddress() == null) {
                LOG.debug("connection.localAddress == null");
            }

            else {
                builder.add("localAddress", exchange.getConnection().getLocalAddress().toString());
            }

            if(exchange.getConnection().getPeerAddress() == null) {
                LOG.debug("connection.peerAddress == null");
            }

            else {
                builder.add("peerAddress", exchange.getConnection().getPeerAddress().toString());
            }
        }

        return builder.build();
    }
}
