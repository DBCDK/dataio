package dk.dbc.dataio.gui.client.util;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.pages.navigation.Texts;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;

/**
 * Created by ThomasBerg on 09/11/15.
 */
@GinModules(CommonModule.class)
public interface CommonGinjector extends Ginjector {
    Texts getMenuTexts();
    ProxyErrorTexts getProxyErrorTexts();
    FlowStoreProxyAsync getFlowStoreProxyAsync();
}
