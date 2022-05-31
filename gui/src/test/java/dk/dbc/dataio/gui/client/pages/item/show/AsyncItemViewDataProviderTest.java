package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class AsyncItemViewDataProviderTest {

    @Mock
    CommonGinjector mockedCommonInjector;
    @Mock
    View mockedView;
    @Mock
    JobStoreProxyAsync mockedJobStoreProxy;

    private AsyncItemViewDataProvider objectUnderTest;


    @Before
    public void setUp() throws Exception {
        when(mockedCommonInjector.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
    }

    @Test
    public void InitialSetup() throws Exception {

        objectUnderTest = new AsyncItemViewDataProvider(mockedView);

        assertThat(objectUnderTest.baseCriteria, is(nullValue()));
        verify(mockedView, times(0)).refreshItemsTable();
    }

    @Test
    public void testSetNewBaseCriteria_criteriaIdentical_RefreshNotInvoked() throws Exception {

        objectUnderTest = new AsyncItemViewDataProvider(mockedView);
        objectUnderTest.setBaseCriteria(ItemListCriteria.Field.JOB_ID, objectUnderTest.baseCriteria);

        verify(mockedView, times(0)).refreshItemsTable();
    }

    @Test
    public void testSetNewBaseCriteria_criteriaNotIdentical_RefreshInvoked() throws Exception {

        objectUnderTest = new AsyncItemViewDataProvider(mockedView);
        objectUnderTest.setBaseCriteria(ItemListCriteria.Field.STATE_FAILED, new ItemListCriteria());

        objectUnderTest.setBaseCriteria(ItemListCriteria.Field.STATE_IGNORED, new ItemListCriteria().where(new ListFilter<>(ItemListCriteria.Field.STATE_FAILED)));

        verify(mockedView, times(1)).refreshItemsTable();
    }
}
