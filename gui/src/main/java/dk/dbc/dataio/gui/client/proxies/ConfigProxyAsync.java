package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;


public interface ConfigProxyAsync {
    void getConfigResource(String configName, AsyncCallback<String> async);
}
