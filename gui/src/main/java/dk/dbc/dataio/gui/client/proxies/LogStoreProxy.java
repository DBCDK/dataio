package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;

@RemoteServiceRelativePath("LogStoreProxy")
public interface LogStoreProxy extends RemoteService {

    String getItemLog(String jobId, Long chunkId, Long itemId) throws ProxyException;

    void close();

    class Factory {

        private static LogStoreProxyAsync asyncInstance = null;

        public static LogStoreProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(LogStoreProxy.class);
            }
            return asyncInstance;
        }
    }
}
