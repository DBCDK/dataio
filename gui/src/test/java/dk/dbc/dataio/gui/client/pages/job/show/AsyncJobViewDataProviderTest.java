package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.jobfilter.JobFilter;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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

        assertThat(objectUnderTest.baseCriteria, is(new JobListCriteriaModel()));
        verify(mockedView, times(1)).refreshJobsTable();

    }

    @Test
    public void testSetBaseCriteria() throws Exception {

        objectUnderTest=new AsyncJobViewDataProvider(mockedClientFactory,mockedView );


        JobListCriteriaModel model=new JobListCriteriaModel();
        model.getJobTypes().remove(JobListCriteriaModel.JobType.TRANSIENT.name());
        model.getJobTypes().remove(JobListCriteriaModel.JobType.PERSISTENT.name());


        objectUnderTest.setBaseCriteria(model);

        // One from the constructor and one from the setBaseQuery
        verify(mockedView, times(2)).refreshJobsTable();

        JobListCriteriaModel model2=new JobListCriteriaModel();
        model2.getJobTypes().remove(JobListCriteriaModel.JobType.TRANSIENT.name());
        model2.getJobTypes().remove(JobListCriteriaModel.JobType.PERSISTENT.name());
        verify(mockedView, times(2)).refreshJobsTable();
    }


    @Test
    public void testUpdateUserCriteria() throws Exception {
        objectUnderTest=new AsyncJobViewDataProvider(mockedClientFactory,mockedView );


        JobListCriteriaModel jobListCriteriaModel=new JobListCriteriaModel();

        when(mockedJobFilter.getValue()).thenReturn(jobListCriteriaModel);
        when(mockedProcessingFailedJobsButton.getValue()).thenReturn(true);
        //when(mockedSelectionModel.getSelectedObject()).thenReturn((JobModel) nullValue());

        objectUnderTest.updateUserCriteria();
        assertThat(jobListCriteriaModel.getSearchType(), is(JobListCriteriaModel.JobSearchType.PROCESSING_FAILED));

        verify(mockedView, times(2)).refreshJobsTable();

    }
}