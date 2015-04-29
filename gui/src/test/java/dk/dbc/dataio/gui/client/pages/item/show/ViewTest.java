package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMock;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


/**
* PresenterImpl unit tests
* <p/>
* The test methods of this class uses the following naming convention:
* <p/>
* unitOfWork_stateUnderTest_expectedBehavior
*/
@RunWith(GwtMockitoTestRunner.class)
public class ViewTest {
    @Mock ClientFactory mockedClientFactory;
    @Mock Presenter mockedPresenter;
    @Mock Resources mockedResources;
    @Mock static ClickEvent mockedClickEvent;
    @Mock Widget mockedWidget;
    @Mock SelectionChangeEvent mockedSelectionChangeEvent;
    @Mock SingleSelectionModel mockedSelectionModel;
    @Mock ItemModel mockedItemModel;
    @Mock HandlerRegistration mockedHandlerRegistration;

    // Test Data
    private ItemModel testModel = new ItemModel("11", "ItemId1", "ChunkId1", "JobId1", ItemModel.LifeCycle.DELIVERING);

    /*
     * The tested view contains nested UiBinder views: ViewWidget (a UiBinder view) instantiates three ItemsListView
     * (also UiBinder views).
     * Normally, GwtMockito assures, that all @UiField's will be mocked, but for some reason, the three ItemsListView
     * objects are not mocked ???
     * Therefore, we will manually mock the ItemsListView objects so we know, that they for sure are instantiated, whenever
     * ViewWidget is instantiated (ViewWidget calls methods in ItemsListView in its constructor)
     * But when instantiating ItemsListView manually, we also have to manually mock it's three UiFields: itemsTable,
     * itemsPager and detailedTabs.
     * This is done in the following:
     */
    @GwtMock ItemsListView mockedItemsList;
    @Mock CellTable mockedItemsTable;
    @Mock SimplePager mockedItemsPager;
    @Mock DecoratedTabPanel mockedDetailedTabs;
    @Before
    public void setupMockedUiFields() {
        mockedItemsList.itemsTable = mockedItemsTable;
        mockedItemsList.itemsPager = mockedItemsPager;
        mockedItemsList.detailedTabs = mockedDetailedTabs;
    }


    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_MENU_ITEMS = "Mocked Poster";
    final static String MOCKED_COLUMN_ITEM = "Mocked Post";
    final static String MOCKED_COLUMN_STATUS = "Mocked Status";
    final static String MOCKED_ERROR_COULDNOTFETCHITEMS = "Mocked Det var ikke muligt at hente poster fra Job Store";
    final static String MOCKED_LABEL_BACK = "Mocked Tilbage til Joboversigten";
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
        when(mockedClientFactory.getItemsShowTexts()).thenReturn(mockedTexts);
        when(mockedTexts.menu_Items()).thenReturn(MOCKED_MENU_ITEMS);
        when(mockedTexts.column_Item()).thenReturn(MOCKED_COLUMN_ITEM);
        when(mockedTexts.column_Status()).thenReturn(MOCKED_COLUMN_STATUS);
        when(mockedTexts.error_CouldNotFetchItems()).thenReturn(MOCKED_ERROR_COULDNOTFETCHITEMS);
        when(mockedTexts.label_Back()).thenReturn(MOCKED_LABEL_BACK);
        when(mockedTexts.text_Item()).thenReturn(MOCKED_TEXT_ITEM);
        when(mockedTexts.text_JobId()).thenReturn(MOCKED_TEXT_JOBID);
        when(mockedTexts.text_Submitter()).thenReturn(MOCKED_TEXT_SUBMITTER);
        when(mockedTexts.text_Sink()).thenReturn(MOCKED_TEXT_SINK);
        when(mockedTexts.lifecycle_Partitioning()).thenReturn(MOCKED_LIFECYCLE_PARTITIONING);
        when(mockedTexts.lifecycle_Processing()).thenReturn(MOCKED_LIFECYCLE_PROCESSING);
        when(mockedTexts.lifecycle_Delivering()).thenReturn(MOCKED_LIFECYCLE_DELIVERING);
        when(mockedTexts.lifecycle_Done()).thenReturn(MOCKED_LIFECYCLE_DONE);
        when(mockedTexts.lifecycle_Unknown()).thenReturn(MOCKED_LIFECYCLE_UNKNOWN);
    }


    /*
     * Testing starts here...
     */
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        view = new View(mockedClientFactory);

        // Verify invocations
        verify(mockedItemsPager, times(3)).firstPage();
        verify(mockedItemsTable, times(3)).addColumn(isA(Column.class), eq(MOCKED_COLUMN_ITEM));
        verify(mockedItemsTable, times(3)).addColumn(isA(Column.class), eq(MOCKED_COLUMN_STATUS));
        verify(mockedItemsTable, times(3)).addRangeChangeHandler(any(RangeChangeEvent.Handler.class));
        verify(mockedItemsPager, times(3)).setDisplay(mockedItemsTable);
        verifyNoMoreInteractions(mockedItemsList);
        verifyNoMoreInteractions(mockedItemsTable);
        verifyNoMoreInteractions(mockedItemsPager);
        verifyNoMoreInteractions(mockedDetailedTabs);
    }

    @Test
    public void enableSelection_enableSelectionTrue_selectionEnabled() {
        // Test setup
        view = new View(mockedClientFactory);
        Context context = new Context(mockedItemsList);
        context.selectionModel = mockedSelectionModel;

        // Subject Under Test
        view.enableSelection(true, context);

        // Verification
        assertThat(context.listView, is(mockedItemsList));
        assertThat(context.selectionModel, is(not(nullValue())));
        verify(mockedSelectionModel).addSelectionChangeHandler(any(View.SelectionChangeHandlerClass.class));
        verify(mockedItemsList.itemsTable).setSelectionModel(context.selectionModel);
    }

    @Test
    public void enableSelection_enableSelectionFalse_selectionDisabled() {
        // Test setup
        view = new View(mockedClientFactory);
        Context context = new Context(mockedItemsList);
        context.handlerRegistration = mockedHandlerRegistration;

        // Subject Under Test
        view.enableSelection(false, context);

        // Verification
        verify(mockedHandlerRegistration).removeHandler();
        verify(mockedItemsList.itemsTable).setSelectionModel(null);
    }



    @Test
    public void setSelectionEnabled_setSelectionEnabled_selectionsEnabled() {
        // Test setup
        view = new View(mockedClientFactory);

        // Subject Under Test
        view.setSelectionEnabled(true);

        // Verification
        verify(mockedItemsTable).setSelectionModel(view.allContext.selectionModel);
        verify(mockedItemsTable).setSelectionModel(view.failedContext.selectionModel);
        verify(mockedItemsTable).setSelectionModel(view.ignoredContext.selectionModel);
    }

    class ConcreteView extends View {
        Context context = new Context(mockedItemsList);
        SelectionChangeHandlerClass selectionChangeHandler = new SelectionChangeHandlerClass(context);

        public ConcreteView(ClientFactory clientFactory) {
            super(clientFactory);
            context.handlerRegistration = mockedHandlerRegistration;
            context.selectionModel = mockedSelectionModel;
        }
    }

    @Test
    public void selectionChangeHandlerClass_callEventHandler_verify() {
        // Test setup
        ConcreteView concreteView = new ConcreteView(mockedClientFactory);
        when(mockedSelectionModel.getSelectedObject()).thenReturn(mockedItemModel);
        concreteView.setPresenter(mockedPresenter);

        // Subject Under Test
        concreteView.selectionChangeHandler.onSelectionChange(mockedSelectionChangeEvent);

        // Verification
        verify(mockedPresenter).itemSelected(mockedItemsList, mockedItemModel);
    }

    @Test
    public void selectionChangeHandlerClass_callEventHandlerWithEmptySelection_verifyNoSelectionSetToPresenter() {
        // Test setup
        ConcreteView concreteView = new ConcreteView(mockedClientFactory);
        when(mockedSelectionModel.getSelectedObject()).thenReturn(null);
        concreteView.setPresenter(mockedPresenter);

        // Subject Under Test
        concreteView.selectionChangeHandler.onSelectionChange(mockedSelectionChangeEvent);

        // Verification
        verifyZeroInteractions(mockedPresenter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructItemColumn_call_correctlySetup() {
        // Test setup
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructItemColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel), is(MOCKED_TEXT_ITEM + " " + testModel.getItemNumber()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructStatusColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructStatusColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel), is(MOCKED_LIFECYCLE_DELIVERING));
    }

}