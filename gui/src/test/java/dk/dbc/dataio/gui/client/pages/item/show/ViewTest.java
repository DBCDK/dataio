package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMock;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.modelBuilders.DiagnosticModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.ItemModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.WorkflowNoteModelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Mock
    ViewGinjector mockedViewInjector;
    @Mock
    Presenter mockedPresenter;
    @Mock
    SelectionChangeEvent mockedSelectionChangeEvent;
    @Mock
    SingleSelectionModel mockedSelectionModel;
    @Mock
    ItemModel mockedItemModel;
    @Mock
    HandlerRegistration mockedHandlerRegistration;

    // Test Data
    private DiagnosticModel diagnosticModel = new DiagnosticModelBuilder().build();
    private ItemModel itemModel = new ItemModelBuilder().setLifeCycle(ItemModel.LifeCycle.DELIVERING).setWorkflowNoteModel(new WorkflowNoteModelBuilder().build()).build();

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

    @GwtMock
    ItemsListView mockedItemsList;
    @Mock
    CellTable mockedItemsTable;
    @GwtMock
    JobDiagnosticTabContent mockedJobDiagnosticTabContent;
    @Mock
    CellTable mockedJobDiagnosticTable;
    @Mock
    SimplePager mockedItemsPager;
    @Mock
    DecoratedTabPanel mockedDetailedTabs;

    @Before
    public void setupMockedUiFields() {
        mockedItemsList.itemsTable = mockedItemsTable;
        mockedItemsList.detailedTabs = mockedDetailedTabs;
        mockedJobDiagnosticTabContent.jobDiagnosticTable = mockedJobDiagnosticTable;
    }


    // Subject Under Test
    private ConcreteView view;

    // Mocked Texts
    @Mock
    Texts mockedTexts;
    @Mock
    dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuItems;
    final static String MOCKED_MENU_ITEMS = "Mocked Poster";
    final static String MOCKED_COLUMN_ITEM = "Mocked Post";
    final static String MOCKED_COLUMN_FIXED = "Mocked Fixed";
    final static String MOCKED_COLUMN_LEVEL = "Mocked Diagnostic level";
    final static String MOCKED_COLUMN_MESSAGE = "Mocked Diagnostic message";
    final static String MOCKED_COLUMN_RECORD_ID = "Mocked Record Id";
    final static String MOCKED_COLUMN_STATUS = "Mocked Status";
    final static String MOCKED_ERROR_COULDNOTFETCHITEMS = "Mocked Det var ikke muligt at hente poster fra Job Store";
    final static String MOCKED_LABEL_BACK = "Mocked Tilbage til Joboversigten";
    final static String MOCKED_TEXT_ITEM = "Mocked Post";
    final static String MOCKED_TEXT_JOBID = "Mocked Job Id:";
    final static String MOCKED_TEXT_SUBMITTER = "Mocked Submitter:";
    final static String MOCKED_TEXT_SINK = "Mocked Sink:";
    final static String MOCKED_TEXT_TRACKING_ID = "Mocked Text Tracking Id";
    final static String MOCKED_LIFECYCLE_PARTITIONING = "Mocked Partitioning";
    final static String MOCKED_LIFECYCLE_PROCESSING = "Mocked Processing";
    final static String MOCKED_LIFECYCLE_DELIVERING = "Mocked Delivering";
    final static String MOCKED_LIFECYCLE_DONE = "Mocked Done";
    final static String MOCKED_LIFECYCLE_UNKNOWN = "Mocked Ukendt Lifecycle";
    final static String MOCKED_TRACE = "Mocked Trace";

    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedMenuItems.menu_Items()).thenReturn(MOCKED_MENU_ITEMS);
        when(mockedTexts.column_Item()).thenReturn(MOCKED_COLUMN_ITEM);
        when(mockedTexts.column_Fixed()).thenReturn(MOCKED_COLUMN_FIXED);
        when(mockedTexts.column_Message()).thenReturn(MOCKED_COLUMN_MESSAGE);
        when(mockedTexts.column_Level()).thenReturn(MOCKED_COLUMN_LEVEL);
        when(mockedTexts.column_RecordId()).thenReturn(MOCKED_COLUMN_RECORD_ID);
        when(mockedTexts.column_Status()).thenReturn(MOCKED_COLUMN_STATUS);
        when(mockedTexts.error_CouldNotFetchItems()).thenReturn(MOCKED_ERROR_COULDNOTFETCHITEMS);
        when(mockedTexts.label_Back()).thenReturn(MOCKED_LABEL_BACK);
        when(mockedTexts.text_Item()).thenReturn(MOCKED_TEXT_ITEM);
        when(mockedTexts.text_JobId()).thenReturn(MOCKED_TEXT_JOBID);
        when(mockedTexts.text_Submitter()).thenReturn(MOCKED_TEXT_SUBMITTER);
        when(mockedTexts.text_Sink()).thenReturn(MOCKED_TEXT_SINK);
        when(mockedTexts.text_TrackingId()).thenReturn(MOCKED_TEXT_TRACKING_ID);
        when(mockedTexts.lifecycle_Partitioning()).thenReturn(MOCKED_LIFECYCLE_PARTITIONING);
        when(mockedTexts.lifecycle_Processing()).thenReturn(MOCKED_LIFECYCLE_PROCESSING);
        when(mockedTexts.lifecycle_Delivering()).thenReturn(MOCKED_LIFECYCLE_DELIVERING);
        when(mockedTexts.lifecycle_Done()).thenReturn(MOCKED_LIFECYCLE_DONE);
        when(mockedTexts.lifecycle_Unknown()).thenReturn(MOCKED_LIFECYCLE_UNKNOWN);
        when(mockedTexts.button_Trace()).thenReturn(MOCKED_TRACE);
    }


    // Testing starts here...
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        setupView();
        view.setupColumns(mockedItemsList);
        view.setupColumns(mockedJobDiagnosticTabContent);

        // Verify invocations
        verify(mockedItemsTable, times(1)).addColumn(isA(Column.class), eq(MOCKED_COLUMN_ITEM));
        verify(mockedItemsTable, times(1)).addColumn(isA(Column.class), eq(MOCKED_COLUMN_STATUS));
        verify(mockedItemsTable, times(1)).addColumn(isA(Column.class), eq(MOCKED_COLUMN_RECORD_ID));
        verify(mockedItemsPager, times(1)).setDisplay(mockedItemsTable);
        verify(mockedJobDiagnosticTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_LEVEL));
        verify(mockedJobDiagnosticTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_MESSAGE));
    }


    class ConcreteView extends View {
        SelectionChangeHandlerClass selectionChangeHandler = new SelectionChangeHandlerClass();

        public ConcreteView() {
            super(false);
            this.viewInjector = mockedViewInjector;
            this.handlerRegistration = mockedHandlerRegistration;
            this.selectionModel = mockedSelectionModel;
        }

        @Override
        Texts getTexts() {
            return mockedTexts;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructItemColumn_call_correctlySetup() {
        // Test setup
        setupView();

        // Subject Under Test
        Column column = view.constructItemColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(itemModel), is(MOCKED_TEXT_ITEM + " " + itemModel.getItemNumber()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructStatusColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructStatusColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(itemModel), is(MOCKED_LIFECYCLE_DELIVERING));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructTrackingIdColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructTrackingIdColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(itemModel), is(MOCKED_TRACE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFixedColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructFixedColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(itemModel), is(itemModel.getWorkflowNoteModel().isProcessed()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDiagnosticLevelColumn_call_correctlySetup() {
        // Test setup
        setupView();

        // Subject Under Test
        Column column = view.constructDiagnosticLevelColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(diagnosticModel), is(diagnosticModel.getLevel()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDiagnosticMessageColumn_call_correctlySetup() {
        // Test setup
        setupView();

        // Subject Under Test
        Column column = view.constructDiagnosticMessageColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(diagnosticModel), is(diagnosticModel.getMessage()));
    }

    @Test
    public void selectionChangeHandlerClass_callEventHandler_verify() {
        // Test setup
        ConcreteView concreteView = setupViewConcrete();
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
        ConcreteView concreteView = setupViewConcrete();
        when(mockedSelectionModel.getSelectedObject()).thenReturn(null);
        concreteView.setPresenter(mockedPresenter);

        // Subject Under Test
        concreteView.selectionChangeHandler.onSelectionChange(mockedSelectionChangeEvent);

        // Verification
        verifyNoInteractions(mockedPresenter);
    }

    @Test
    public void trackingButtonCell_render_rendersCorrectly() {
        // Test setup
        ConcreteView view = setupViewConcrete();
        View.TrackingButtonCell trackingButtonCell = view.new TrackingButtonCell();
        Cell.Context mockedContext = mock(Cell.Context.class);
        SafeHtml mockedSafeHtml = mock(SafeHtml.class);
        SafeHtmlBuilder mockedSafeHtmlBuilder = mock(SafeHtmlBuilder.class);
        when(mockedContext.getKey()).thenReturn(mockedItemModel);
        when(mockedItemModel.getTrackingId()).thenReturn("This Is Tracking Id");

        // Subject Under Test
        trackingButtonCell.render(mockedContext, mockedSafeHtml, mockedSafeHtmlBuilder);

        // Verification
        verify(mockedContext).getKey();
        verify(mockedItemModel).getTrackingId();
        verify(mockedSafeHtmlBuilder).appendHtmlConstant("<span title='" + MOCKED_TEXT_TRACKING_ID + " This Is Tracking Id'>");
        verify(mockedSafeHtmlBuilder).appendHtmlConstant("<button type=\"button\" tabindex=\"-1\">");
        verify(mockedSafeHtmlBuilder).append(mockedSafeHtml);
        verify(mockedSafeHtmlBuilder).appendHtmlConstant("</button>");
        verify(mockedSafeHtmlBuilder).appendHtmlConstant("</span>");
        verifyNoMoreInteractions(mockedContext);
        verifyNoMoreInteractions(mockedSafeHtml);
        verifyNoMoreInteractions(mockedSafeHtmlBuilder);
        verifyNoMoreInteractions(mockedItemModel);
    }


    /*
     * Private methods
     */
    private ConcreteView setupViewConcrete() {
        return new ConcreteView();
    }

    private void setupView() {
        view = new ConcreteView();
        view.viewInjector = mockedViewInjector;
        view.itemsPager = mockedItemsPager;
    }
}
