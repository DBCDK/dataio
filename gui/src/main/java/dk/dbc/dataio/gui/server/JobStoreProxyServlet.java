package dk.dbc.dataio.gui.server;


import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

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
    public List<JobModel> listJobs(JobListCriteria model) throws ProxyException {
        return jobStoreProxy.listJobs(model);
    }

    @Override
    public long countJobs(JobListCriteria model) throws ProxyException {
        return jobStoreProxy.countJobs(model);
    }

    @Override
    public List<ItemModel> listItems(ItemListCriteria.Field searchType, ItemListCriteria criteria) throws ProxyException {
        return jobStoreProxy.listItems(searchType, criteria);
    }

    @Override
    public long countItems(ItemListCriteria criteria) throws ProxyException {
        return jobStoreProxy.countItems(criteria);
    }

    @Override
    public String getItemData(int jobId, int chunkId, short itemId, ItemModel.LifeCycle lifeCycle) throws ProxyException {
        return jobStoreProxy.getItemData(jobId, chunkId, itemId, lifeCycle);
    }

    @Override
    public String getProcessedNextResult(int jobId, int chunkId, short itemId) throws ProxyException {
        return jobStoreProxy.getProcessedNextResult(jobId, chunkId, itemId);
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
