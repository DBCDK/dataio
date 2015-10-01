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


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest {
    @Mock ClientFactory mockedClientFactory;
    @Mock PlaceController mockedPlaceController;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;
    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock Throwable mockedException;
    @Mock SingleSelectionModel<JobModel> mockedSingleSelectionModel;
    @Mock AsyncJobViewDataProvider mockedAsyncJobViewDataProvider;
    @Mock CellTable mockedJobsTable;
    @Mock TextBox mockedJobIdInputField;
    @Mock JobStoreProxyAsync mockedJobStoreProxy;

    // Mocked Texts
    @Mock static Texts mockedText;
    final static String MOCKED_INPUT_FIELD_VALIDATION_ERROR = "mocked error_InputFieldValidationError";
    final static String MOCKED_NUMERIC_INPUT_FIELD_VALIDATION_ERROR = "mocked error_InputFieldValidationError";
    final static String MOCKED_JOB_NOT_FOUND_ERROR = "mocked error_JobNotFound()";


    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getJobsShowView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);

        mockedView.selectionModel = mockedSingleSelectionModel;
        mockedView.dataProvider = mockedAsyncJobViewDataProvider;
        mockedView.jobsTable = mockedJobsTable;
        mockedView.jobIdInputField = mockedJobIdInputField;

        mockedView.texts = mockedText;
        when(mockedText.error_InputFieldValidationError()).thenReturn(MOCKED_INPUT_FIELD_VALIDATION_ERROR);
        when(mockedText.error_NumericInputFieldValidationError()).thenReturn(MOCKED_NUMERIC_INPUT_FIELD_VALIDATION_ERROR);
        when(mockedText.error_JobNotFound()).thenReturn(MOCKED_JOB_NOT_FOUND_ERROR);
    }

    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public CountExistingJobsWithJobIdCallBack getJobCountCallback;
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
            view = mockedView;
            this.getJobCountCallback = new CountExistingJobsWithJobIdCallBack();
        }

        @Override
        protected void updateBaseQuery() {

            JobListCriteria criteria = new JobListCriteria()
                     .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                     .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"));

            view.dataProvider.setBaseCriteria( criteria );
        }
    }

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        // Verify Test
        verify(mockedClientFactory).getJobStoreProxyAsync();
    }

    @Test
    public void start_callStart_ok() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
    }

    @Test
    public void filterJobs_updateSelectedJobs() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.updateSelectedJobs();

        // Verify Test
        verify(mockedView.selectionModel).clear();
        verify(mockedAsyncJobViewDataProvider).updateUserCriteria();
        verify(mockedAsyncJobViewDataProvider).updateCurrentCriteria();
        verify(mockedAsyncJobViewDataProvider).setBaseCriteria(any(JobListCriteria.class));
    }

    @Test
    public void showJob_jobIdInputFieldIsEmpty_errorMessageInView() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedJobIdInputField.getValue()).thenReturn("");

        // Subject under test
        presenterImpl.showJob();

        // Verify Test
        verify(mockedView).setErrorText(MOCKED_INPUT_FIELD_VALIDATION_ERROR);
        verify(mockedJobStoreProxy, times(0)).countJobs(any(JobListCriteria.class), any(PresenterImpl.CountExistingJobsWithJobIdCallBack.class));
    }

    @Test
    public void showJob_jobIdInputFieldContainsNoneNumericValue_errorMessageInView() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedJobIdInputField.getValue()).thenReturn("test123");

        // Subject under test
        presenterImpl.showJob();

        // Verify Test
        verify(mockedView).setErrorText(MOCKED_NUMERIC_INPUT_FIELD_VALIDATION_ERROR);
        verify(mockedJobStoreProxy, times(0)).countJobs(any(JobListCriteria.class), any(PresenterImpl.CountExistingJobsWithJobIdCallBack.class));
    }

    @Test
    public void showJob_jobIdInputFieldContainsValidJobId_countJobsCalled() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedJobIdInputField.getValue()).thenReturn("140");
        presenterImpl.jobStoreProxy = mockedJobStoreProxy;

        // Subject under test
        presenterImpl.showJob();

        // Verify Test
        verify(mockedJobStoreProxy).countJobs(any(JobListCriteria.class), any(PresenterImpl.CountExistingJobsWithJobIdCallBack.class));
    }

    @Test
    public void showJob_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCountCallback.onFailure(mockedException);

        // Verify Test
        verifyZeroInteractions(mockedView.jobIdInputField);
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void showJob_callbackWithSuccess_JobNotFound() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCountCallback.onSuccess(0L);

        // Verify Test
        verifyZeroInteractions(mockedView.jobIdInputField);
        verify(mockedView).setErrorText(MOCKED_JOB_NOT_FOUND_ERROR);
    }

    @Test
    public void showJob_callbackWithSuccess_jobFound() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCountCallback.onSuccess(1L);

        // Verify Test
        verify(mockedView.jobIdInputField).setText("");
        verify(mockedPlaceController).goTo(any(dk.dbc.dataio.gui.client.pages.item.show.Place.class));
    }

}