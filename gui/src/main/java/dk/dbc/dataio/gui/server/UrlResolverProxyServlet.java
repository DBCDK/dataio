package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.proxies.UrlResolverProxy;

public class UrlResolverProxyServlet extends RemoteServiceServlet implements UrlResolverProxy {
    private static final long serialVersionUID = 284209582492514343L;

    @Override
    public String getUrl(String name) {
        return Urls.getInstance().get(name);
    }
}
