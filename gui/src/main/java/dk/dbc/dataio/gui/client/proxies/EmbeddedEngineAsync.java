package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface EmbeddedEngineAsync {
    void executeJob(String dataPath, String flow, AsyncCallback<Void> async) throws Exception;
}
