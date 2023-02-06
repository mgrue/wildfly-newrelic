package ch.performancebuildings.newrelic;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NewrelicHandler extends ExtHandler {

    private static final Logger LOG = LogManager.getLogger(NewrelicHandler.class);

    private static final String hostname;

    private String licenseKey;

    private String source;

    private String baseURI = "https://log-api.eu.newrelic.com/log/v1";

    static {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPublish(final ExtLogRecord record) {
        if(licenseKey == null) {
            throw new IllegalStateException("licenseKey == null");
        }

        try {
            final JsonObject obj = Json.createObjectBuilder()
                    .add("timestamp", System.currentTimeMillis())
                    .add("message", record.getMessage())
                    .add("hostname", hostname)
                    .add("source", getSourceOrHost())
                    .add("level", record.getLevel().getName())
                    .add("class", record.getSourceClassName())
                    .add("method", record.getSourceMethodName())
                    .add("file", record.getSourceFileName())
                    .add("line", record.getSourceLineNumber())
                    .add("loggerClass", record.getLoggerClassName())
                    .build();

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
    }

    @SuppressWarnings("unused")
    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
        LOG.debug("Newrelic license key set");
    }

    @SuppressWarnings("unused")
    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
        LOG.debug("Newrelic base uri: " + baseURI);
    }

    @SuppressWarnings("unused")
    public void setSource(String source) {
        this.source = source;
    }

    private String getSourceOrHost() {
        return source == null ? hostname : source;
    }
}
