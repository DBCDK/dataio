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

package dk.dbc.dataio.gui.client.pages.job.show;

import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

import java.util.List;
import java.util.Map;

public interface Presenter extends GenericPresenter {
    void itemSelected(JobModel jobModel);
    void updateSelectedJobs();
    void refresh();
    void showJob();
    void changeColorSchemeListBoxShow();
    boolean isRawRepo();
    void changeColorScheme(Map<String, String> colorScheme);
    void editJob(boolean isFailedItemsOnly);
    void setWorkflowNote(WorkflowNoteModel workflowNoteModel, String jobId);
    WorkflowNoteModel preProcessAssignee(WorkflowNoteModel workflowNoteModel, String assignee);
    void rerunJobs(List<JobModel> jobModels);
    void setPlace(AbstractBasePlace place);
}
