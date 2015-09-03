package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class AsyncItemViewDataProviderTest {

    @Mock ClientFactory mockedClientFactory;
    @Mock View mockedView;
    @Mock JobStoreProxyAsync mockedJobStoreProxy;
    @Mock SingleSelectionModel<JobModel> mockedSelectionModel;
    @Mock ItemsListView itemsListView;

    private AsyncItemViewDataProvider objectUnderTest;


    @Before
    public void setUp() throws Exception {
        when(mockedClientFactory.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
    }

    @Test
    public void InitialSetup() throws Exception {

        objectUnderTest = new AsyncItemViewDataProvider(mockedClientFactory, mockedView );

        assertThat(objectUnderTest.baseCriteria, is(nullValue()));
        verify(mockedView, times(0)).refreshItemsTable();
    }

    @Test
    public void testSetNewBaseCriteria_criteriaIdentical_RefreshNotInvoked() throws Exception {

        objectUnderTest = new AsyncItemViewDataProvider(mockedClientFactory, mockedView );
        objectUnderTest.setBaseCriteria(ItemListCriteria.Field.JOB_ID, itemsListView, objectUnderTest.baseCriteria);

        verify(mockedView, times(0)).refreshItemsTable();
    }

    @Test
    public void testSetNewBaseCriteria_criteriaNotIdentical_RefreshInvoked() throws Exception {

        objectUnderTest = new AsyncItemViewDataProvider(mockedClientFactory, mockedView );
        objectUnderTest.setBaseCriteria(ItemListCriteria.Field.STATE_FAILED, itemsListView, new ItemListCriteria());

        objectUnderTest.setBaseCriteria(ItemListCriteria.Field.STATE_IGNORED, itemsListView, new ItemListCriteria().where(new ListFilter<>(ItemListCriteria.Field.STATE_FAILED)));

        verify(mockedView, times(1)).refreshItemsTable();
    }

}