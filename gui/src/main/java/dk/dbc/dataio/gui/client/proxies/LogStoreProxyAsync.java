package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface LogStoreProxyAsync {

    void getItemLog(String jobId, Long chunkId, Long itemId, AsyncCallback<String> async);

    void close(AsyncCallback<Void> async);
}
