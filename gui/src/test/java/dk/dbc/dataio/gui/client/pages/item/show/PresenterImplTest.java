
package dk.dbc.dataio.gui.client.pages.item.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
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
    @Mock JobStoreProxyAsync mockedJobStoreProxy;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;
    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock Throwable mockedException;
    @Mock Place mockedPlace;

    @Mock Label mockedJobHeader;
    @Mock CellTable mockedItemsTable;
    @Mock DecoratedTabPanel mockedTabPanel;
    @Mock SimplePager mockedPager;
    @Mock RadioButton mockedAllItemsButton;
    @Mock RadioButton mockedFailedItemsButton;
    @Mock RadioButton mockedIgnoredItemsButton;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
        when(mockedClientFactory.getItemsShowView()).thenReturn(mockedView);
        mockedView.jobHeader = mockedJobHeader;
        mockedView.itemsTable = mockedItemsTable;
        mockedView.tabPanel = mockedTabPanel;
        mockedView.pager = mockedPager;
        mockedView.allItemsButton = mockedAllItemsButton;
        mockedView.failedItemsButton = mockedFailedItemsButton;
        mockedView.ignoredItemsButton = mockedIgnoredItemsButton;
        when(mockedAllItemsButton.getValue()).thenReturn(true);
        when(mockedFailedItemsButton.getValue()).thenReturn(false);
        when(mockedIgnoredItemsButton.getValue()).thenReturn(false);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
    }

    // Mocked Texts
    @Mock static Texts mockedText;
    final static String MOCKED_MENU_ITEMS = "Mocked Poster";
    final static String MOCKED_COLUMN_ITEM = "Mocked Post";
    final static String MOCKED_COLUMN_STATUS = "Mocked Status";
    final static String MOCKED_ERROR_COULDNOTFETCHITEMS = "Mocked Det var ikke muligt at hente poster fra Job Store";
    final static String MOCKED_LABEL_BACK = "Mocked Tilbage til Joboversigten";
    final static String MOCKED_BUTTON_ALLITEMS = "Mocked Alle Poster";
    final static String MOCKED_BUTTON_FAILEDITEMS = "Mocked Fejlede Poster";
    final static String MOCKED_BUTTON_IGNOREDITEMS = "Mocked Ignorerede Poster";
    final static String MOCKED_TEXT_ITEM = "Mocked Post";
    final static String MOCKED_TEXT_JOBID = "Mocked Job Id:";
    final static String MOCKED_TEXT_SUBMITTER = "Mocked Submitter:";
    final static String MOCKED_TEXT_SINK = "Mocked Sink:";
    final static String MOCKED_LIFECYCLE_PARTITIONING = "Mocked Partitioning";
    final static String MOCKED_LIFECYCLE_PROCESSING = "Mocked Processing";
    final static String MOCKED_LIFECYCLE_DELIVERING = "Mocked Delivering";
    final static String MOCKED_LIFECYCLE_DONE = "Mocked Done";
    final static String MOCKED_LIFECYCLE_UNKNOWN = "Mocked Ukendt Lifecycle";
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedText.menu_Items()).thenReturn(MOCKED_MENU_ITEMS);
        when(mockedText.column_Item()).thenReturn(MOCKED_COLUMN_ITEM);
        when(mockedText.column_Status()).thenReturn(MOCKED_COLUMN_STATUS);
        when(mockedText.error_CouldNotFetchItems()).thenReturn(MOCKED_ERROR_COULDNOTFETCHITEMS);
        when(mockedText.label_Back()).thenReturn(MOCKED_LABEL_BACK);
        when(mockedText.label_Back()).thenReturn(MOCKED_BUTTON_ALLITEMS);
        when(mockedText.label_Back()).thenReturn(MOCKED_BUTTON_FAILEDITEMS);
        when(mockedText.label_Back()).thenReturn(MOCKED_BUTTON_IGNOREDITEMS);
        when(mockedText.text_Item()).thenReturn(MOCKED_TEXT_ITEM);
        when(mockedText.text_JobId()).thenReturn(MOCKED_TEXT_JOBID);
        when(mockedText.text_Submitter()).thenReturn(MOCKED_TEXT_SUBMITTER);
        when(mockedText.text_Sink()).thenReturn(MOCKED_TEXT_SINK);
        when(mockedText.lifecycle_Partitioning()).thenReturn(MOCKED_LIFECYCLE_PARTITIONING);
        when(mockedText.lifecycle_Processing()).thenReturn(MOCKED_LIFECYCLE_PROCESSING);
        when(mockedText.lifecycle_Delivering()).thenReturn(MOCKED_LIFECYCLE_DELIVERING);
        when(mockedText.lifecycle_Done()).thenReturn(MOCKED_LIFECYCLE_DONE);
        when(mockedText.lifecycle_Unknown()).thenReturn(MOCKED_LIFECYCLE_UNKNOWN);
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(Place place, ClientFactory clientFactory, Texts texts) {
            super(place, clientFactory, texts);
        }
        public GetItemsCallback getItemsCallback = new GetItemsCallback();
    }


    // Test Data
    private ItemModel testModel1 = new ItemModel("11", "ItemId1", "ChunkId1", "JobId1", ItemModel.LifeCycle.DELIVERING);
    private ItemModel testModel2 = new ItemModel("12", "ItemId2", "ChunkId2", "JobId2", ItemModel.LifeCycle.DONE);
    private ItemModel testModel3 = new ItemModel("13", "ItemId3", "ChunkId3", "JobId3", ItemModel.LifeCycle.PARTITIONING);
    private ItemModel testModel4 = new ItemModel("14", "ItemId4", "ChunkId4", "JobId4", ItemModel.LifeCycle.PROCESSING);
    private List<ItemModel> testModels = Arrays.asList(testModel1, testModel2, testModel3, testModel4);

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory, mockedText);

        // Verify Test
        verify(mockedClientFactory).getPlaceController();
        verify(mockedClientFactory).getJobStoreProxyAsync();
        verify(mockedClientFactory).getLogStoreProxyAsync();
        verify(mockedPlace).getJobId();
        verify(mockedPlace).getSubmitterNumber();
        verify(mockedPlace).getSinkName();
    }

    @Test
    public void start_callStart_ok() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory, mockedText);
        presenterImpl.jobId = "1234";
        presenterImpl.submitterNumber = "Submi";
        presenterImpl.sinkName = "Sinki";
        when(mockedAllItemsButton.getValue()).thenReturn(false);
        when(mockedFailedItemsButton.getValue()).thenReturn(true);
        when(mockedIgnoredItemsButton.getValue()).thenReturn(false);

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedClientFactory).getItemsShowView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedJobHeader).setText("Mocked Job Id: 1234, Mocked Submitter: Submi, Mocked Sink: Sinki");
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFailedItemsButton).setValue(true);
        verify(mockedAllItemsButton).getValue();
        verify(mockedFailedItemsButton).getValue();
        verifyZeroInteractions(mockedIgnoredItemsButton);
        verify(mockedJobStoreProxy).listItems(any(ItemListCriteriaModel.class), any(AsyncCallback.class));
        verify(mockedTabPanel).clear();
        verify(mockedTabPanel).setVisible(false);
    }

    @Test
    public void filterItems_allItemsSelected_allItemsRequested() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory, mockedText);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedAllItemsButton.getValue()).thenReturn(false);
        when(mockedFailedItemsButton.getValue()).thenReturn(true);
        when(mockedIgnoredItemsButton.getValue()).thenReturn(false);

        // Subject under test
        presenterImpl.filterItems();

        // Verify Test
        verify(mockedTabPanel, times(2)).setVisible(false);
        verify(mockedTabPanel, times(2)).clear();
        verify(mockedAllItemsButton, times(2)).getValue();  // Two invocations: One during call to start, one during fiterItems()
        verify(mockedFailedItemsButton).getValue();
        verifyZeroInteractions(mockedIgnoredItemsButton);
        verify(mockedJobStoreProxy, times(2)).listItems(any(ItemListCriteriaModel.class), any(AsyncCallback.class));  // Two invocations: One during call to start, one during fiterItems()
    }

    @Test
    public void filterItems_failedItemsSelected_failedItemsRequested() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory, mockedText);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedAllItemsButton.getValue()).thenReturn(false);
        when(mockedFailedItemsButton.getValue()).thenReturn(true);
        when(mockedIgnoredItemsButton.getValue()).thenReturn(false);

        // Subject under test
        presenterImpl.filterItems();

        // Verify Test
        verify(mockedTabPanel, times(2)).setVisible(false);
        verify(mockedTabPanel, times(2)).clear();
        verify(mockedAllItemsButton, times(2)).getValue();  // Two invocations: One during call to start, one during fiterItems()
        verify(mockedFailedItemsButton).getValue();
        verifyZeroInteractions(mockedIgnoredItemsButton);
        verify(mockedJobStoreProxy, times(2)).listItems(any(ItemListCriteriaModel.class), any(AsyncCallback.class));  // Two invocations: One during call to start, one during fiterItems()
    }

    @Test
    public void filterItems_ignoredItemsSelected_ignoredItemsRequested() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory, mockedText);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedAllItemsButton.getValue()).thenReturn(false);
        when(mockedFailedItemsButton.getValue()).thenReturn(false);
        when(mockedIgnoredItemsButton.getValue()).thenReturn(true);

        // Subject under test
        presenterImpl.filterItems();

        // Verify Test
        verify(mockedTabPanel, times(2)).setVisible(false);
        verify(mockedTabPanel, times(2)).clear();
        verify(mockedAllItemsButton, times(2)).getValue();  // Two invocations: One during call to start, one during fiterItems()
        verify(mockedFailedItemsButton).getValue();
        verify(mockedIgnoredItemsButton).getValue();
        verify(mockedJobStoreProxy, times(2)).listItems(any(ItemListCriteriaModel.class), any(AsyncCallback.class));  // Two invocations: One during call to start, one during fiterItems()
    }

    @Test
    public void fetchJob_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedText);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(any(String.class));
    }

    @Test
    public void fetchJob_callbackWithSuccess_jobsAreFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedText);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onSuccess(testModels);

        // Verify Test
        verify(mockedView).setItems(testModels);
    }

}
