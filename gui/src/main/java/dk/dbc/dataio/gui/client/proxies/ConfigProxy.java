package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;


@RemoteServiceRelativePath("ConfigProxy")
public interface ConfigProxy extends RemoteService {

    String getConfigResource(String configName);

    class Factory {
        private static ConfigProxyAsync asyncInstance = null;

        public static ConfigProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(ConfigProxy.class);
            }
            return asyncInstance;
        }
    }
}
