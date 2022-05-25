package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;


@RemoteServiceRelativePath("UrlResolverProxy")
public interface UrlResolverProxy extends RemoteService {

    String getUrl(String name);

    class Factory {
        private static UrlResolverProxyAsync asyncInstance = null;

        public static UrlResolverProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(UrlResolverProxy.class);
            }
            return asyncInstance;
        }
    }
}
