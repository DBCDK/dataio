package dk.dbc.dataio.jse.artemis.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class JseProxySelector extends ProxySelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(JseProxySelector.class);
    private final String host;
    private final int port;
    private final Pattern nonProxyHosts;
    private static final List<Proxy> NO_PROXY = List.of(Proxy.NO_PROXY);

    public JseProxySelector(String proxyString) {
        String[] sa = proxyString.split(":", 3);
        if(sa.length != 3) {
            String msg = "Proxy config string " + proxyString + " did not have the expected format proxyHost:proxyPort:nonProxyRegex";
            throw new IllegalArgumentException(msg);
        }
        host = sa[0];
        port = Integer.parseInt(sa[1]);
        nonProxyHosts = Pattern.compile(sa[2]);
    }

    public JseProxySelector(String host, int port, String nonProxyHosts) {
        this.host = host;
        this.port = port;
        this.nonProxyHosts = Pattern.compile(nonProxyHosts);
    }

    @Override
    public List<Proxy> select(URI uri) {
        if(uri.getHost().indexOf('.') < 0) return NO_PROXY;
        Matcher matcher = nonProxyHosts.matcher(uri.getHost());
        if(matcher.matches()) return NO_PROXY;
        return List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        LOGGER.warn("Connection to " + uri + " failed.");
    }
}
