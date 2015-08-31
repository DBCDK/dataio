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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by ja7 on 24-08-15.
 */
@RunWith(GwtMockitoTestRunner.class)
public class AsyncJobViewDataProviderTest {

    @Mock ClientFactory mockedClientFactory;
    @Mock View mockedView;
    @Mock JobStoreProxyAsync mockedJobStoreProxy;
    @Mock SingleSelectionModel<JobModel> mockedSelectionModel;
    @Mock JobFilter mockedJobFilter;
    @Mock RadioButton mockedProcessingFailedJobsButton;

    private AsyncJobViewDataProvider objectUnderTest;


    @Before
    public void setUp() throws Exception {
        when(mockedClientFactory.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
        mockedView.selectionModel=mockedSelectionModel;
        mockedView.jobFilter=mockedJobFilter;
        mockedView.processingFailedJobsButton=mockedProcessingFailedJobsButton;
    }

    @Test
    public void InitialSetup() throws Exception {

        objectUnderTest=new AsyncJobViewDataProvider(mockedClientFactory,mockedView );

        verify(mockedView, times(1)).refreshJobsTable();

    }

    @Test
    public void testSetBaseCriteria() throws Exception {

        objectUnderTest=new AsyncJobViewDataProvider(mockedClientFactory,mockedView );

        JobListCriteria criteria=new JobListCriteria()
                 .where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                 .or(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"));

        objectUnderTest.setBaseCriteria(criteria);

        // One from the constructor and one from the setBaseQuery
        verify(mockedView, times(2)).refreshJobsTable();

        verify(mockedView, times(2)).refreshJobsTable();
    }


    @Test
    public void testUpdateUserCriteria() throws Exception {
        objectUnderTest=new AsyncJobViewDataProvider(mockedClientFactory,mockedView );


        JobListCriteria jobListCriteria=new JobListCriteria();

        when(mockedJobFilter.getValue()).thenReturn(jobListCriteria);
        when(mockedProcessingFailedJobsButton.getValue()).thenReturn(true);

        objectUnderTest.updateUserCriteria();

        verify(mockedView, times(2)).refreshJobsTable();

    }
}