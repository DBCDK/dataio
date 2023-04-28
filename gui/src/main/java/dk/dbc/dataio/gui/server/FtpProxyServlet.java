package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FtpFileModel;
import dk.dbc.dataio.gui.client.proxies.FtpProxy;

import javax.servlet.ServletException;
import java.util.List;

public class FtpProxyServlet extends RemoteServiceServlet implements FtpProxy {
    private static final long serialVersionUID = 284209582492514623L;

    private transient FtpProxy ftpProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            ftpProxy = new FtpProxyImpl();
        } catch (ProxyException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void put(String fileName, String content) throws ProxyException {
        ftpProxy.put(fileName, content);
    }

    @Override
    public List<FtpFileModel> ftpFiles() throws ProxyException {

        return ftpProxy.ftpFiles();
    }

}
