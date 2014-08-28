package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;

import javax.servlet.ServletException;
import java.util.List;

public class JobStoreProxyServlet extends RemoteServiceServlet implements JobStoreProxy {
    private static final long serialVersionUID = 358109395377092220L;

    private transient JobStoreProxy jobStoreProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        jobStoreProxy = new JobStoreProxyImpl();
    }

    @Override
    public String getJobStoreFilesystemUrl() throws ProxyException {
        return jobStoreProxy.getJobStoreFilesystemUrl();
    }

    @Override
    public List<JobInfo> findAllJobs() throws ProxyException {
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
