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

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;

import java.util.List;

public interface JobStoreProxyAsync {
    void listJobs(JobListCriteria model, AsyncCallback<List<JobModel>> async);
    void fetchEarliestActiveJob(int sinkId, AsyncCallback<JobModel> async);
    void countJobs(JobListCriteria model, AsyncCallback<Long> async);
    void listItems(ItemListCriteria.Field searchType, ItemListCriteria criteria, AsyncCallback<List<ItemModel>> async);
    void countItems(ItemListCriteria criteria, AsyncCallback<Long> async);
    void getItemData(ItemModel itemModel, ItemModel.LifeCycle lifeCycle, AsyncCallback<String> async);
    void getProcessedNextResult(int jobId, int chunkId, short itemId, AsyncCallback<String> async);
    void listJobNotificationsForJob(int jobId, AsyncCallback<List<Notification>> async);
    void reSubmitJob(JobModel jobModel, AsyncCallback<JobModel> async);
    void reSubmitJobs(List<JobModel> jobModels, AsyncCallback<List<JobModel>> async);
    void listInvalidTransfileNotifications(AsyncCallback<List<Notification>> async);
    void setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId, AsyncCallback<JobModel> async);
    void setWorkflowNote(WorkflowNoteModel workflowNoteModel, int jobId, int chunkId, short itemId, AsyncCallback<ItemModel> async);
    void getSinkStatusModels(AsyncCallback<List<SinkStatusTable.SinkStatusModel>> async);
    void createJobRerun(int jobId, boolean failedItemsOnly, AsyncCallback<Void> async);
    void close(AsyncCallback<Void> async);

}
