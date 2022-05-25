package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;


public interface FtpProxyAsync {

    void put(String fileName, String content, AsyncCallback<Void> async);
}
