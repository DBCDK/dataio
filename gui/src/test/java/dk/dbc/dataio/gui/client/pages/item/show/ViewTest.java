package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

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
    @Mock Presenter mockedPresenter;
    @Mock Resources mockedResources;
    @Mock static ClickEvent mockedClickEvent;
    @Mock Widget mockedWidget;
    @Mock SelectionChangeEvent mockedSelectionChangeEvent;
    @Mock SingleSelectionModel mockedSelectionModel;
    @Mock ItemModel mockedItemModel;

    // Test Data
    private ItemModel testModel1 = new ItemModel("11", "ItemId1", "ChunkId1", "JobId1", ItemModel.LifeCycle.DELIVERING);
    private ItemModel testModel2 = new ItemModel("12", "ItemId2", "ChunkId2", "JobId2", ItemModel.LifeCycle.DONE);
    private ItemModel testModel3 = new ItemModel("13", "ItemId3", "ChunkId3", "JobId3", ItemModel.LifeCycle.PARTITIONING);
    private ItemModel testModel4 = new ItemModel("14", "ItemId4", "ChunkId4", "JobId4", ItemModel.LifeCycle.PROCESSING);
    private List<ItemModel> testModels = Arrays.asList(testModel1, testModel2, testModel3, testModel4);

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


    @Test
    public void testsToBeAdded() {
        assert(true);
    }

//    /*
//     * Testing starts here...
//     */
//    @Test
//    @SuppressWarnings("unchecked")
//    public void constructor_instantiate_objectCorrectInitialized() {
//        // Subject Under Test
//        view = new View("Header Text", mockedTexts);
//
//        // Verify invocations
//        verify(view.itemsTable).addColumn(view.itemNumberColumn, MOCKED_COLUMN_ITEM);
//        verify(view.itemsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_STATUS));
//        verify(view.itemsTable).addRangeChangeHandler(any(RangeChangeEvent.Handler.class));
//        verify(view.pager).setDisplay(view.itemsTable);
//    }
//
//    @Test
//    public void setItems_setItemsValidData_dataSetupCorrect() {
//        // Test setup
//        view = new View("Header Text", mockedTexts);
//        final int OFFSET = 0;
//
//        // Subject Under Test
//        view.setItems(testModels, OFFSET, testModels.size());
//
//        // Verification
//        verify(view.itemsTable).setRowCount(testModels.size());
//        verify(view.itemsTable).setRowData(OFFSET, testModels);
//    }
//
//    @Test
//    public void addTab_addTabCalledWithValidWidget_tabSetupCorrectly() {
//        // Test setup
//        view = new View("Header Text", mockedTexts);
//        final String TITLE = "Title";
//
//        // Subject Under Test
//        view.addTab(mockedWidget, TITLE);
//
//        // Verification
//        verify(view.tabPanel).add(mockedWidget, TITLE);
//    }
//
//    @Test
//    public void addTab_addTabCalledWithInvalidWidget_tabNotSet() {
//        // Test setup
//        view = new View("Header Text", mockedTexts);
//        final String TITLE = "Title";
//
//        // Subject Under Test
//        view.addTab(null, TITLE);
//
//        // Verification
//        verifyZeroInteractions(view.tabPanel);
//    }
//
//    @Test
//    public void enableSelection_setSelectionEnabled_selectionEnabled() {
//        // Test setup
//        view = new View("Header Text", mockedTexts);
//
//        // Subject Under Test
//        view.setSelectionEnabled(true);
//
//        // Verification
//        verify(view.itemsTable).setSelectionModel(view.selectionModel);
//    }
//
//    @Test
//    public void disableSelection_setSelectionEnabled_selectionDisabled() {
//        // Test setup
//        view = new View("Header Text", mockedTexts);
//
//        // Subject Under Test
//        view.setSelectionEnabled(false);
//
//        // Verification
//        verify(view.itemsTable).setSelectionModel(null);
//    }
//
//    class ConcreteView extends View {
//        SelectionChangeHandlerClass selectionChangeHandler = new SelectionChangeHandlerClass();
//
//        public ConcreteView(String header, Texts texts) {
//            super(header, texts);
//        }
//    }
//
//    @Test
//    public void selectionChangeHandlerClass_callEventHandler_verify() {
//        // Test setup
//        ConcreteView concreteView = new ConcreteView("Header", mockedTexts);
//        concreteView.selectionModel = mockedSelectionModel;
//        when(mockedSelectionModel.getSelectedObject()).thenReturn(mockedItemModel);
//        concreteView.setPresenter(mockedPresenter);
//
//        // Subject Under Test
//        concreteView.selectionChangeHandler.onSelectionChange(mockedSelectionChangeEvent);
//
//        // Verification
//        verify(mockedPresenter).itemSelected(mockedItemModel);
//    }
//
//    @Test
//    public void selectionChangeHandlerClass_callEventHandlerWithEmptySelection_verifyNoSelectionSetToPresenter() {
//        // Test setup
//        ConcreteView concreteView = new ConcreteView("Header", mockedTexts);
//        concreteView.selectionModel = mockedSelectionModel;
//        when(mockedSelectionModel.getSelectedObject()).thenReturn(null);
//        concreteView.setPresenter(mockedPresenter);
//
//        // Subject Under Test
//        concreteView.selectionChangeHandler.onSelectionChange(mockedSelectionChangeEvent);
//
//        // Verification
//        verifyZeroInteractions(mockedPresenter);
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    public void constructItemColumn_call_correctlySetup() {
//        // Test setup
//        view = new View("Header Text", mockedTexts);
//
//        // Subject Under Test
//        Column column = view.constructItemColumn();
//
//        // Test that correct getValue handler has been setup
//        assertThat((String) column.getValue(testModel1), is(MOCKED_TEXT_ITEM + " " + testModel1.getItemNumber()));
//    }
//
//    @Test
//    @SuppressWarnings("unchecked")
//    public void constructStatusColumn_call_correctlySetup() {
//        view = new View("Header Text", mockedTexts);
//
//        // Subject Under Test
//        Column column = view.constructStatusColumn();
//
//        // Test that correct getValue handler has been setup
//        assertThat((String) column.getValue(testModel1), is(MOCKED_LIFECYCLE_DELIVERING));
//    }

}