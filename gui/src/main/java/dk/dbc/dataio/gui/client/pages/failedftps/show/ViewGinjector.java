package dk.dbc.dataio.gui.client.pages.failedftps.show;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;

/**
 * Ginjector for the Failed Ftp Show page
 */
@GinModules(ViewModule.class)
public interface ViewGinjector extends Ginjector {
    View getView();

    Texts getTexts();

    JobStoreProxyAsync getJobStoreProxyAsync();
}
