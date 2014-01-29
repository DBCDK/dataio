package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.JobInfo;
import java.util.List;

public interface JobStoreProxyAsync {
    void findAllJobs(AsyncCallback<List<JobInfo>> async);

    void close(AsyncCallback<Void> async);
}
