package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;

/**
 * View Module for the Io Traffic page
 */
public class ViewModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(View.class).in(Singleton.class);
        bind(Texts.class).in(Singleton.class);
        bind(FlowStoreProxyAsync.class).in(Singleton.class);
    }
}
