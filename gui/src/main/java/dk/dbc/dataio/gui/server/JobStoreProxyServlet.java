/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.server;


import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxy;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

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
    public List<JobModel> listJobs(JobListCriteria model) throws ProxyException {
        return jobStoreProxy.listJobs(model);
    }

    @Override
    public JobModel fetchEarliestActiveJob(int sinkId) throws ProxyException {
        return jobStoreProxy.fetchEarliestActiveJob(sinkId);
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
    public String getItemData(ItemModel itemModel, ItemModel.LifeCycle lifeCycle) throws ProxyException {
        return jobStoreProxy.getItemData(itemModel, lifeCycle);
    }

    @Override
    public String getProcessedNextResult(int jobId, int chunkId, short itemId) throws ProxyException {
        return jobStoreProxy.getProcessedNextResult(jobId, chunkId, itemId);
    }

    @Override
    public List<Notification> listJobNotificationsForJob(int jobId) throws ProxyException {
        return jobStoreProxy.listJobNotificationsForJob(jobId);
    }

    @Override
    public JobModel reSubmitJob(JobModel jobModel) throws ProxyException {
        return jobStoreProxy.reSubmitJob(jobModel);
    }

    @Override
    public List<JobModel> reSubmitJobs(List<JobModel> jobModels) throws ProxyException {
        return jobStoreProxy.reSubmitJobs(jobModels);
    }

    @Override
    public List<Notification> listInvalidTransfileNotifications() throws ProxyException {
        return jobStoreProxy.listInvalidTransfileNotifications();
    }

    @Override
    public JobModel setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId) throws ProxyException {
        return jobStoreProxy.setWorkflowNote(workflowNoteModel, jobId);
    }

@Override
    public ItemModel setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId, int chunkId, short itemId) throws ProxyException {
        return jobStoreProxy.setWorkflowNote(workflowNoteModel, jobId, chunkId, itemId);
    }

    @Override
    public List<SinkStatusTable.SinkStatusModel> getSinkStatusModels() throws ProxyException {
        return jobStoreProxy.getSinkStatusModels();
    }

    @Override
    public void createJobRerun(int jobId, boolean isFailedItemsOnly) throws ProxyException {
        jobStoreProxy.createJobRerun(jobId, isFailedItemsOnly);
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
