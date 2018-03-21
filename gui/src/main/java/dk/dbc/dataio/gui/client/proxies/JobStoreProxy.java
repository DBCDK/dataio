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

package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

import java.util.List;

@RemoteServiceRelativePath("JobStoreProxy")
public interface JobStoreProxy extends RemoteService {
    List<JobModel> listJobs(JobListCriteria model) throws ProxyException;
    JobModel fetchEarliestActiveJob(int sinkId) throws ProxyException;
    long countJobs(JobListCriteria model) throws ProxyException;
    List<ItemModel> listItems(ItemListCriteria.Field searchType, ItemListCriteria criteria) throws ProxyException;
    long countItems(ItemListCriteria criteria) throws ProxyException;
    String getItemData(ItemModel itemModel, ItemModel.LifeCycle lifeCycle) throws ProxyException;
    String getProcessedNextResult(int jobId, int chunkId, short itemId) throws ProxyException;
    List<Notification> listJobNotificationsForJob(int jobId) throws ProxyException;
    JobModel reSubmitJob(JobModel jobModel) throws ProxyException;
    List<JobModel> reSubmitJobs(List<JobModel> jobModels) throws ProxyException;
    List<Notification> listInvalidTransfileNotifications() throws ProxyException;
    JobModel setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId) throws ProxyException;
    ItemModel setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId, int chunkId, short itemId) throws ProxyException;
    List<SinkStatusTable.SinkStatusModel> getSinkStatusModels() throws ProxyException;
    void createJobRerun(int jobId, boolean failedItemsOnly) throws ProxyException;

    void close();

    class Factory {

        private static JobStoreProxyAsync asyncInstance = null;

        public static JobStoreProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(JobStoreProxy.class);
            }
            return asyncInstance;
        }
    }
}
