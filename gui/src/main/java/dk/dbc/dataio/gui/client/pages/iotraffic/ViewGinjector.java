package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;

/**
 * Ginjector for the Io Traffic page
 */
@GinModules(ViewModule.class)
public interface ViewGinjector extends Ginjector {
    View getView();

    Texts getTexts();

    FlowStoreProxyAsync getFlowStoreProxyAsync();
}
