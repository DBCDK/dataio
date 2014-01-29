package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.exceptions.JobStoreProxyException;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import java.util.List;
import javax.servlet.ServletException;

public class JobStoreProxyServlet extends RemoteServiceServlet implements JobStoreProxy {
    private static final long serialVersionUID = 358109395377092220L;

    private transient JobStoreProxy jobStoreProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        jobStoreProxy = new JobStoreProxyImpl();
    }

    @Override
    public List<JobInfo> findAllJobs() throws JobStoreProxyException {
        return jobStoreProxy.findAllJobs();
    }

    @Override
    public void close() {
        if (jobStoreProxy != null) {
            jobStoreProxy.close();
            jobStoreProxy = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        close();
    }

}
