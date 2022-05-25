package dk.dbc.dataio.gui.client.util;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.pages.navigation.Texts;
import dk.dbc.dataio.gui.client.proxies.ConfigProxyAsync;
import dk.dbc.dataio.gui.client.proxies.FileStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.FtpProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.JobRerunProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.PeriodicJobsHarvesterProxyAsync;
import dk.dbc.dataio.gui.client.proxies.TickleHarvesterProxyAsync;
import dk.dbc.dataio.gui.client.proxies.UrlResolverProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;

public class CommonModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(Texts.class).in(Singleton.class);
        bind(ProxyErrorTexts.class).in(Singleton.class);
        bind(FileStoreProxyAsync.class).in(Singleton.class);
        bind(FlowStoreProxyAsync.class).in(Singleton.class);
        bind(PeriodicJobsHarvesterProxyAsync.class).in(Singleton.class);
        bind(JavaScriptProjectFetcherAsync.class).in(Singleton.class);
        bind(JobStoreProxyAsync.class).in(Singleton.class);
        bind(LogStoreProxyAsync.class).in(Singleton.class);
        bind(UrlResolverProxyAsync.class).in(Singleton.class);
        bind(FtpProxyAsync.class).in(Singleton.class);
        bind(TickleHarvesterProxyAsync.class).in(Singleton.class);
        bind(JobRerunProxyAsync.class).in(Singleton.class);
        bind(ConfigProxyAsync.class).in(Singleton.class);
        bind(Resources.class).in(Singleton.class);
    }
}
