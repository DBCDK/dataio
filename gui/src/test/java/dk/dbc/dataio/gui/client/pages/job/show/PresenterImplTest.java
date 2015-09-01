
package dk.dbc.dataio.gui.client.pages.job.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
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

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getJobsShowView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);

        mockedView.selectionModel = mockedSingleSelectionModel;
        mockedView.dataProvider = mockedAsyncJobViewDataProvider;
        mockedView.jobsTable = mockedJobsTable;
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

            JobListCriteria criteria = new JobListCriteria()
                     .where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                     .or(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"));

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

}