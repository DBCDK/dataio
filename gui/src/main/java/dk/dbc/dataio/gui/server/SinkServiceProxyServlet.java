package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.pages.sink.sinkmodify.SinkModel;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxy;

import javax.servlet.ServletException;

public class SinkServiceProxyServlet extends RemoteServiceServlet implements SinkServiceProxy {
    private static final long serialVersionUID = 6389757968008300151L;

    private transient SinkServiceProxy sinkServiceProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        sinkServiceProxy = new SinkServiceProxyImpl();
    }

    @Override
    public PingResponse ping(SinkModel model) throws ProxyException {
        return sinkServiceProxy.ping(model);
    }

    @Override
    public void close() {
        if (sinkServiceProxy != null) {
            sinkServiceProxy.close();
            sinkServiceProxy = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        close();
    }
}
