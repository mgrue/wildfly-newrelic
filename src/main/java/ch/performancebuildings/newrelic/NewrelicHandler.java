package ch.performancebuildings.newrelic;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NewrelicHandler extends ExtHandler {

    private static final Logger LOG = Logger.getLogger(NewrelicHandler.class);

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
        if (licenseKey == null) {
            throw new IllegalStateException("licenseKey == null");
        }

        try {
            final JsonObject obj = buildJson(record);

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

    private JsonObject buildJson(ExtLogRecord record) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("timestamp", System.currentTimeMillis())
                .add("source", getSourceOrHost());

        if (record.getMessage() == null) {
            LOG.debug("message == null");
        } else {
            builder.add("message", record.getMessage());
        }

        if (record.getLevel() == null) {
            LOG.debug("level == null");
        }

        else {
            builder.add("level", record.getLevel().getName());
        }

        if (record.getSourceClassName() == null) {
            LOG.debug("sourceClassName == null");
        }

        else {
            builder.add("class", record.getSourceClassName());
        }

        if (record.getSourceMethodName() == null) {
            LOG.debug("sourceMethodName == null");
        }

        else {
            builder.add("method", record.getSourceMethodName());
        }

        if (record.getSourceFileName() == null) {
            LOG.debug("sourceFileName == null");
        }

        else {
            builder.add("file", record.getSourceFileName());
        }


        if (record.getLoggerClassName() == null) {
            LOG.debug("loggerClassName == null");
        }

        else {
            builder.add("loggerClass", record.getLoggerClassName());
        }

        if (record.getSourceLineNumber() < 0) {
            LOG.debug("sourceLineNumber == "  + record.getSourceLineNumber());
        }

        else {
            builder.add("line", record.getSourceLineNumber());
        }

        return builder.build();
    }
}
