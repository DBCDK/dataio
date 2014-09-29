package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxy;

import javax.naming.NamingException;
import javax.servlet.ServletException;

public class LogStoreProxyServlet extends RemoteServiceServlet implements LogStoreProxy {

    private static final long serialVersionUID = -3088652295615752905L;
    private transient LogStoreProxy logStoreProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        try{
            logStoreProxy = new LogStoreProxyImpl();
        }catch (NamingException e){
            throw new ServletException(e);
        }
    }

    @Override
    public String getItemLog(String jobId, Long chunkId, Long itemId) throws ProxyException {
        return logStoreProxy.getItemLog(jobId, chunkId, itemId);
    }

    @Override
    public void close() {
        if (logStoreProxy != null) {
            logStoreProxy.close();
            logStoreProxy = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        close();
    }
}
