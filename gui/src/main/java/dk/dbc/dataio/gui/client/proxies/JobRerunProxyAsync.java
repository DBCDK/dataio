package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;

public interface JobRerunProxyAsync {
    void parse(JobModel jobModel, AsyncCallback<JobRerunScheme> async);

    void close(AsyncCallback<Void> async);
}
