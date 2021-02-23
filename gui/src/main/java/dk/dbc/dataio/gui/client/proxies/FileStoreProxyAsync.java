package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface FileStoreProxyAsync {

    void removeFile(String fileId, AsyncCallback<Void> async);

    void addFile(byte[] dataSource, AsyncCallback<String> async);

    void close(AsyncCallback<Void> async);
}
