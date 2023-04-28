package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.FtpFileModel;

import java.util.List;


public interface FtpProxyAsync {

    void put(String fileName, String content, AsyncCallback<Void> async);
    void ftpFiles(AsyncCallback<List<FtpFileModel>> async);
}
