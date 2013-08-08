package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("EmbeddedEngine")
public interface EmbeddedEngine extends RemoteService {

    void executeJob(String dataPath, String flow) throws Exception;

    public static class Factory {

        private static EmbeddedEngineAsync asyncInstance = null;

        public static EmbeddedEngineAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(EmbeddedEngine.class);
            }
            return asyncInstance;
        }
    }
}
