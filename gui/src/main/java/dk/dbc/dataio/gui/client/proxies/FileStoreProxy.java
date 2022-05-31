package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;

import java.util.Map;


@RemoteServiceRelativePath("FileStoreProxy")
public interface FileStoreProxy extends RemoteService {
    void removeFile(String fileId) throws ProxyException;

    void addMetadata(final String fileId, final Map<String, String> metadata) throws ProxyException;

    void close();

    class Factory {
        private static FileStoreProxyAsync asyncInstance = null;

        public static FileStoreProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(FileStoreProxy.class);
            }
            return asyncInstance;
        }
    }
}
