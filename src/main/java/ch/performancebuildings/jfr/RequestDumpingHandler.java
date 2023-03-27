package ch.performancebuildings.jfr;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RequestDumpingHandler implements HttpHandler {

    private static final Logger LOG = LogManager.getLogger(RequestDumpingHandler.class);

    private static final String hostname;

    private String source;

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
        final HttpRequestEvent event = new HttpRequestEvent(getSourceOrHost(), exchange);

        exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
            event.begin();
            nextListener.proceed();
        });

        event.commit();
        next.handleRequest(exchange);
    }

    @SuppressWarnings("unused")
    public void setSource(String source) {
        this.source = source;
    }

    private String getSourceOrHost() {
        return source == null ? hostname : source;
    }
}
