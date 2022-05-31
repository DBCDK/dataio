package dk.dbc.dataio.gui.client.util;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
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

@GinModules(CommonModule.class)
public interface CommonGinjector extends Ginjector {
    Texts getMenuTexts();

    ProxyErrorTexts getProxyErrorTexts();

    FileStoreProxyAsync getFileStoreProxyAsync();

    FlowStoreProxyAsync getFlowStoreProxyAsync();

    PeriodicJobsHarvesterProxyAsync getPeriodicJobsHarvesterProxy();

    JobStoreProxyAsync getJobStoreProxyAsync();

    LogStoreProxyAsync getLogStoreProxyAsync();

    JavaScriptProjectFetcherAsync getJavaScriptProjectFetcherAsync();

    UrlResolverProxyAsync getUrlResolverProxyAsync();

    FtpProxyAsync getFtpProxyAsync();

    TickleHarvesterProxyAsync getTickleHarvesterProxyAsync();

    JobRerunProxyAsync getJobRerunProxyAsync();

    ConfigProxyAsync getConfigProxyAsync();

    Resources getResources();
}
