package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.jobfilter.JobFilter;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class AsyncJobViewDataProviderTest {

    @Mock
    CommonGinjector mockedCommonInjector;
    @Mock
    View mockedView;
    @Mock
    JobStoreProxyAsync mockedJobStoreProxy;
    @Mock
    SingleSelectionModel<JobModel> mockedSelectionModel;
    @Mock
    JobFilter mockedJobFilter;
    @Mock
    ProvidesKey mockedKeyProvider;

    private AsyncJobViewDataProvider objectUnderTest;


    @Before
    public void setUp() throws Exception {
        when(mockedCommonInjector.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
        mockedView.selectionModel = mockedSelectionModel;
        mockedView.jobFilter = mockedJobFilter;
    }

    @Test
    public void InitialSetup() throws Exception {
        objectUnderTest = new AsyncJobViewDataProvider(mockedView, mockedKeyProvider);
    }

    @Test
    public void testUpdateCurrentCriteria_jobFilterEmpty_baseCriteria() throws Exception {
        objectUnderTest = new AsyncJobViewDataProvider(mockedView, mockedKeyProvider);
        when(mockedJobFilter.getValue()).thenReturn(new JobListCriteria());

        JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"INFOMEDIA\"}"));
        objectUnderTest.baseCriteria = criteria;

        objectUnderTest.updateCurrentCriteria();

        assertThat(objectUnderTest.currentCriteria, is(criteria));
        verify(mockedView, times(1)).loadJobsTable();
    }

    @Test
    public void testUpdateCurrentCriteria_jobFilterNotEmpty_baseCriteria() throws Exception {
        objectUnderTest = new AsyncJobViewDataProvider(mockedView, mockedKeyProvider);
        when(mockedJobFilter.getValue()).thenReturn(new JobListCriteria().
                where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, "dummy")));

        JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"));
        objectUnderTest.baseCriteria = criteria;

        objectUnderTest.updateCurrentCriteria();

        assertThat(objectUnderTest.currentCriteria, not(is(criteria)));
        verify(mockedView, times(1)).loadJobsTable();
    }

    @Test
    public void testSetBaseCriteria() throws Exception {

        objectUnderTest = new AsyncJobViewDataProvider(mockedView, mockedKeyProvider);
        when(mockedView.jobFilter.getValue()).thenReturn(new JobListCriteria());

        JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"));

        objectUnderTest.setBaseCriteria(criteria);

        assertThat(objectUnderTest.currentCriteria, is(criteria));
        verify(mockedView, times(1)).loadJobsTable();
    }

}


