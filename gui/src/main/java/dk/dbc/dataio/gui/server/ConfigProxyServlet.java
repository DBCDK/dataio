package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.proxies.ConfigProxy;

import javax.servlet.ServletException;

public class ConfigProxyServlet extends RemoteServiceServlet implements ConfigProxy {
    private static final long serialVersionUID = 6535390072221653885L;
    private transient ConfigProxy configProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        configProxy = new ConfigProxyImpl();
    }

    @Override
    public String getConfigResource(String configName) {
        return configProxy.getConfigResource(configName);
    }

}
