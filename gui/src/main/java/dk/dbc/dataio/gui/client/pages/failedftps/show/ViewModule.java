package dk.dbc.dataio.gui.client.pages.failedftps.show;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;

/**
 * View Module for the Failed Ftps page
 */
public class ViewModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(View.class).in(Singleton.class);
        bind(Texts.class).in(Singleton.class);
        bind(JobStoreProxyAsync.class).in(Singleton.class);
    }
}
