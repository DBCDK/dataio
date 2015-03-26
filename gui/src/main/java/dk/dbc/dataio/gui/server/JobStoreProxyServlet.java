package dk.dbc.dataio.gui.server;


import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import java.util.List;

public class JobStoreProxyServlet extends RemoteServiceServlet implements JobStoreProxy {
    private static final long serialVersionUID = 358109395377092220L;

    private transient JobStoreProxy jobStoreProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            jobStoreProxy = new JobStoreProxyImpl();
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public List<JobModel> listJobs(JobListCriteriaModel model) throws ProxyException {
        return jobStoreProxy.listJobs(model);
    }

    @Override
    public List<ItemModel> listItems(ItemListCriteriaModel model) throws ProxyException {
        return jobStoreProxy.listItems(model);
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
