package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;

@RemoteServiceRelativePath("JobRerunProxy")
public interface JobRerunProxy extends RemoteService {

    JobRerunScheme parse(JobModel jobModel) throws ProxyException;

    void close();

    class Factory {

        private static JobRerunProxyAsync asyncInstance = null;

        public static JobRerunProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(JobRerunProxy.class);
            }
            return asyncInstance;
        }
    }
}
