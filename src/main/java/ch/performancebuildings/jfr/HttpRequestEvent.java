package ch.performancebuildings.jfr;

import io.undertow.server.HttpServerExchange;
import jdk.jfr.Category;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name("HttpRequest")
@Category("Undertow")
@StackTrace(false)
@Enabled
public class HttpRequestEvent extends Event {

    public HttpRequestEvent(String source, HttpServerExchange exchange) {
        this.source = source;

        if (exchange.getRequestURI() != null) {
            this.URI = exchange.getRequestURI();
        }

        if(exchange.getQueryString() != null) {
            this.query = exchange.getQueryString();
        }

        if(exchange.getConnection() != null) {
            if(exchange.getConnection().getLocalAddress() != null) {
                this.localAddress = exchange.getConnection().getLocalAddress().toString();
            }

            if(exchange.getConnection().getPeerAddress() != null) {
                this.peerAddress = exchange.getConnection().getPeerAddress().toString();
            }
        }
    }

    @Label("Source")
    private String source;

    @Label("URI")
    private String URI;

    @Label("Query")
    private String query;

    @Label("LocalAddress")
    private String localAddress;

    @Label("PeerAddress")
    private String peerAddress;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getURI() {
        return URI;
    }

    public void setURI(String URI) {
        this.URI = URI;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public void setPeerAddress(String peerAddress) {
        this.peerAddress = peerAddress;
    }
}
