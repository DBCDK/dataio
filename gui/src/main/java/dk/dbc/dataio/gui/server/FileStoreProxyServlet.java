package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FileStoreProxy;

import javax.servlet.ServletException;
import java.util.Map;

public class FileStoreProxyServlet extends RemoteServiceServlet implements FileStoreProxy {
    private transient FileStoreProxyImpl fileStoreProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        fileStoreProxy = new FileStoreProxyImpl();
    }

    @Override
    public void removeFile(String fileId) throws ProxyException {
        fileStoreProxy.removeFile(fileId);
    }

    @Override
    public void addMetadata(final String fileId, final Map<String, String> metadata) throws ProxyException {
        fileStoreProxy.addMetadata(fileId, metadata);
    }

    @Override
    public void close() {
        if (fileStoreProxy != null) {
            fileStoreProxy.close();
            fileStoreProxy = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        close();
    }
}
