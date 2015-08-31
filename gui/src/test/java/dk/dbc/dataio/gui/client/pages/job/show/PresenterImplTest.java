
package dk.dbc.dataio.gui.client.pages.job.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.modelBuilders.JobModelBuilder;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
@Ignore
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest {
    @Mock ClientFactory mockedClientFactory;
    @Mock PlaceController mockedPlaceController;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;
    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock Throwable mockedException;
    @Mock RadioButton mockedAllJobsButton;
    @Mock RadioButton mockedProcessingFailedJobsButton;
    @Mock RadioButton mockedDeliveringFailedJobsButton;
    @Mock SingleSelectionModel<JobModel> mockedSingleSelectionModel;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getJobsShowView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);

        mockedView.allJobsButton = mockedAllJobsButton;
        mockedView.processingFailedJobsButton = mockedProcessingFailedJobsButton;
        mockedView.deliveringFailedJobsButton = mockedDeliveringFailedJobsButton;
        mockedView.selectionModel = mockedSingleSelectionModel;
    }

    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
            view = mockedView;
        }

        @Override
        protected void updateBaseQuery() {

            JobListCriteria criteria=new JobListCriteria()
                     .where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                     .or(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"));

            view.dataProvider.setBaseCriteria( criteria );
        }
    }


    // Test Data
    private JobModel testModel1 = new JobModelBuilder()
            .setJobId("1418716277429")
            .setSubmitterNumber("150014")
            .setItemCounter(20)
            .setFailedCounter(5)
            .setIgnoredCounter(5)
            .setPartitionedCounter(31)
            .setProcessedCounter(32)
            .setDeliveredCounter(33)
            .build();

    private JobModel testModel2 = new JobModelBuilder()
            .setJobId("1418773068083")
            .setSubmitterNumber("424242")
            .setItemCounter(10)
            .setFailedCounter(0)
            .setIgnoredCounter(5)
            .setPartitionedCounter(34)
            .setProcessedCounter(35)
            .setDeliveredCounter(36)
            .build();

    private List<JobModel> testModels = new ArrayList<JobModel>(Arrays.asList(testModel1, testModel2));

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        // Verify Test
        verify(mockedClientFactory).getJobStoreProxyAsync();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterJobs_processingFailedSelected_jobsFailedInProcessingRequested() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProcessingFailedJobsButton.getValue()).thenReturn(true);

        // Subject under test
        presenterImpl.updateSelectedJobs();

        // Verify Test
        verify(mockedView.selectionModel).clear();
        verify(mockedProcessingFailedJobsButton, times(2)).getValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterJobs_deliveringFailedSelected_jobsFailedInDeliveringRequested() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProcessingFailedJobsButton.getValue()).thenReturn(false);
        when(mockedDeliveringFailedJobsButton.getValue()).thenReturn(true);

        // Subject under test
        presenterImpl.updateSelectedJobs();

        // Verify Test
        verify(mockedView.selectionModel).clear();
        verify(mockedProcessingFailedJobsButton, times(2)).getValue();
        verify(mockedDeliveringFailedJobsButton, times(2)).getValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterJobs_allJobsSelected_allJobsRequested() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProcessingFailedJobsButton.getValue()).thenReturn(false);
        when(mockedDeliveringFailedJobsButton.getValue()).thenReturn(false);

        // Subject under test
        presenterImpl.updateSelectedJobs();

        // Verify Test
        verify(mockedView.selectionModel).clear();
        verify(mockedProcessingFailedJobsButton, times(2)).getValue();
        verify(mockedDeliveringFailedJobsButton, times(2)).getValue();
        verifyZeroInteractions(mockedAllJobsButton);
    }

}