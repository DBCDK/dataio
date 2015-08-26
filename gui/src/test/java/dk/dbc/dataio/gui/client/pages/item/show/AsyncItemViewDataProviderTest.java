package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
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

        assertThat(objectUnderTest.baseCriteria, is(new ItemListCriteriaModel()));
        verify(mockedView, times(1)).refreshItemsTable();
    }

    @Test
    public void testSetNewBaseCriteria_criteriaIdentical_RefreshNotInvoked() throws Exception {

        objectUnderTest = new AsyncItemViewDataProvider(mockedClientFactory, mockedView );
        objectUnderTest.setBaseCriteria(itemsListView, objectUnderTest.baseCriteria);

        // One from the constructor and one from the setBaseQuery
        verify(mockedView, times(1)).refreshItemsTable();
    }

    @Test
    public void testSetNewBaseCriteria_criteriaNotIdentical_RefreshInvoked() throws Exception {

        objectUnderTest = new AsyncItemViewDataProvider(mockedClientFactory, mockedView );
        ItemListCriteriaModel model = new ItemListCriteriaModel();
        model.setItemSearchType(ItemListCriteriaModel.ItemSearchType.IGNORED);

        objectUnderTest.setBaseCriteria(itemsListView, model);

        // One from the constructor and one from the setBaseQuery
        verify(mockedView, times(2)).refreshItemsTable();
    }

}