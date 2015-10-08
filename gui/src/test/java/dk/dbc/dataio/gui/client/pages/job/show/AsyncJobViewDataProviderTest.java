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

import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.jobfilter.JobFilter;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class AsyncJobViewDataProviderTest {

    @Mock ClientFactory mockedClientFactory;
    @Mock View mockedView;
    @Mock JobStoreProxyAsync mockedJobStoreProxy;
    @Mock SingleSelectionModel<JobModel> mockedSelectionModel;
    @Mock JobFilter mockedJobFilter;
    @Mock RadioButton mockedProcessingFailedJobsButton;
    @Mock RadioButton mockedDeliveringFailedJobsButton;
    @Mock RadioButton mockedFatalJobsButton;

    private AsyncJobViewDataProvider objectUnderTest;


    @Before
    public void setUp() throws Exception {
        when(mockedClientFactory.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
        mockedView.selectionModel = mockedSelectionModel;
        mockedView.jobFilter = mockedJobFilter;
        mockedView.processingFailedJobsButton = mockedProcessingFailedJobsButton;
        mockedView.deliveringFailedJobsButton = mockedDeliveringFailedJobsButton;
        mockedView.fatalJobsButton = mockedFatalJobsButton;
    }

    @Test
    public void InitialSetup() throws Exception {
        objectUnderTest = new AsyncJobViewDataProvider(mockedClientFactory, mockedView);
    }

    @Test
    public void testSetBaseCriteria() throws Exception {

        objectUnderTest = new AsyncJobViewDataProvider(mockedClientFactory, mockedView);

        JobListCriteria criteria = new JobListCriteria()
                 .where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                 .or(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"));

        objectUnderTest.setBaseCriteria(criteria);

        // One from the constructor and one from the setBaseQuery
        verify(mockedView, times(1)).loadJobsTable();
    }

    @Test
    public void testUpdateUserCriteria_processingFailedSelected_jobsFailedInProcessingRequested() throws Exception {
        genericUpdateUserCriteriaAssertSearchType(mockedProcessingFailedJobsButton, JobListCriteria.Field.STATE_PROCESSING_FAILED);
    }

    @Test
    public void testUpdateUserCriteria_deliveringFailedSelected_jobsFailedInDeliveringRequested() throws Exception {
        genericUpdateUserCriteriaAssertSearchType(mockedDeliveringFailedJobsButton, JobListCriteria.Field.STATE_DELIVERING_FAILED);
    }

    @Test
    public void testUpdateUserCriteria_jobsWithFatalErrorSelected_jobsWithFatalErrorRequested() throws Exception {
        genericUpdateUserCriteriaAssertSearchType(mockedFatalJobsButton, JobListCriteria.Field.WITH_FATAL_ERROR);
    }

    @Test
    public void testUpdateUserCriteria_initialSearch_allJobsRequested() throws Exception {
        objectUnderTest = new AsyncJobViewDataProvider(mockedClientFactory, mockedView );
        when(mockedJobFilter.getValue()).thenReturn(new JobListCriteria());

        objectUnderTest.updateUserCriteria();
        assertThat(mockedView.jobFilter.getValue().getFiltering().size(), is(0));
        verify(mockedView, times(0)).refreshJobsTable();
    }

    /*
     * Private methods
     */

    private void genericUpdateUserCriteriaAssertSearchType(RadioButton radioButton, JobListCriteria.Field field) {
        objectUnderTest = new AsyncJobViewDataProvider(mockedClientFactory, mockedView );
        when(mockedJobFilter.getValue()).thenReturn(new JobListCriteria());
        when(radioButton.getValue()).thenReturn(true);
        objectUnderTest.updateUserCriteria();

        assertThat(mockedView.jobFilter.getValue().getFiltering().get(0).getMembers().get(0).getFilter().getField(), is(field));
        verify(mockedView, times(1)).loadJobsTable();
    }
}


