package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FtpFileModel;

import java.util.List;


@RemoteServiceRelativePath("FtpProxy")
public interface FtpProxy extends RemoteService {

    void put(String fileName, String content) throws ProxyException;
    List<FtpFileModel> ftpFiles() throws ProxyException;

    class Factory {
        private static FtpProxyAsync asyncInstance = null;

        public static FtpProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(FtpProxy.class);
            }
            return asyncInstance;
        }
    }
}
