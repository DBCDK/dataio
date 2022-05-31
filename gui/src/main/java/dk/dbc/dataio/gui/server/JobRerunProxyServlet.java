package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobRerunProxy;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;

import javax.servlet.ServletException;

public class JobRerunProxyServlet extends RemoteServiceServlet implements JobRerunProxy {
    private static final long serialVersionUID = 350209395900092220L;

    private transient JobRerunProxy jobRerunProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        jobRerunProxy = new JobRerunProxyImpl();
    }

    @Override
    public JobRerunScheme parse(JobModel jobModel) throws ProxyException {
        return jobRerunProxy.parse(jobModel);
    }

    @Override
    public void close() {
        if (jobRerunProxy != null) {
            jobRerunProxy.close();
            jobRerunProxy = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        close();
    }
}
