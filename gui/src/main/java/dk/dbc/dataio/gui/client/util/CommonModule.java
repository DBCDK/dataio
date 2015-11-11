package dk.dbc.dataio.gui.client.util;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.pages.navigation.Texts;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;

/**
 * Created by ThomasBerg on 09/11/15.
 */
public class CommonModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(ProxyErrorTexts.class).in(Singleton.class);
        bind(FlowStoreProxyAsync.class).in(Singleton.class);
        bind(Texts.class).in(Singleton.class);
    }
}
