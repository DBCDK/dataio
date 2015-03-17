package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
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
    @Mock ColumnSortList mockedColumnSortList;


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


    /*
     * Testing starts here...
     */
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        view = new View("Header Text", mockedTexts);

        // Verify invocations
        verify(view.itemsTable).addColumnSortHandler(view.columnSortHandler);
        verify(view.itemsTable).addColumn(view.itemNumberColumn, MOCKED_COLUMN_ITEM);
        verify(view.itemsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_STATUS));
        verify(view.itemsTable).setSelectionModel(isA(SelectionModel.class));
        verify(view.pager).setDisplay(view.itemsTable);
    }


    @Test
    public void constructor_setupData_dataSetupCorrect() {
        view = new View("Header Text", mockedTexts);
        when(view.itemsTable.getColumnSortList()).thenReturn(mockedColumnSortList);

        // Subject Under Test
        view.setItems(testModels);

        verify(view.itemsTable).getColumnSortList();
        verify(mockedColumnSortList).clear();
        verify(mockedColumnSortList).push(view.itemNumberColumn);
        verify(view.itemsTable).setPageSize(View.PAGE_SIZE);
        verify(view.itemsTable).setRowCount(4);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructItemColumn_call_correctlySetup() {
        view = new View("Header Text", mockedTexts);

        // Subject Under Test
        Column column = view.constructItemColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(MOCKED_TEXT_ITEM + " " + testModel1.getItemNumber()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(true));

        // Test that correct comparator has been setup
        // Item Number for testModel1 is lower than for testModel2
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel1), is(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel2), lessThan(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel2, testModel1), greaterThan(0));

        // Test that column is set to descending sorting
        assertThat(column.isDefaultSortAscending(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructStatusColumn_call_correctlySetup() {
        view = new View("Header Text", mockedTexts);

        // Subject Under Test
        Column column = view.constructStatusColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(MOCKED_LIFECYCLE_DELIVERING));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(true));

        // Test that correct comparator has been setup
        // JobId for testModel1 is less than testModel2
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel1), is(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel2), lessThan(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel2, testModel1), greaterThan(0));

        // Test that column is set to ascending sorting
        assertThat(column.isDefaultSortAscending(), is(false));
    }

}