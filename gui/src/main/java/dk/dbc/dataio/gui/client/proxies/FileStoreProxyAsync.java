package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.Map;

public interface FileStoreProxyAsync {

    void removeFile(String fileId, AsyncCallback<Void> async);

    void addMetadata(final String fileId, final Map<String, String> metadata, AsyncCallback<Void> async);

    void close(AsyncCallback<Void> async);
}
