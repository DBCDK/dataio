package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;


public interface UrlResolverProxyAsync {
    void getUrl(String name, AsyncCallback<String> async);
}
