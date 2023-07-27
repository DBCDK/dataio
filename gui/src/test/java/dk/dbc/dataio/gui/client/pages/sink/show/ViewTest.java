package dk.dbc.dataio.gui.client.pages.sink.show;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
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
    Presenter mockedPresenter;
    @Mock
    dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock
    static ClickEvent mockedClickEvent;
    @Mock
    private ViewGinjector mockedViewGinjector;


    // Test Data
    private SinkModel testModel1 = new SinkModelBuilder().setName("SinkNam1").setQueue("SinkQueue1").setDescription("SinkDescription1").setSinkType(SinkContent.SinkType.DUMMY).build();
    private SinkModel testModel2 = new SinkModelBuilder().setName("SinkNam2").setQueue("SinkQueue2").setDescription("SinkDescription2").build();
    private List<SinkModel> testModels = Arrays.asList(testModel1, testModel2);


    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock
    static Texts mockedTexts;
    final static String MOCKED_LABEL_SINKS = "Mocked Text: label_Sinks";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: button_Edit";
    final static String MOCKED_COLUMNHEADER_TYPE = "Mocked Text: columnHeader_Type";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: columnHeader_Name";
    final static String MOCKED_COLUMNHEADER_DESCRIPTION = "Mocked Text: columnHeader_Description";
    final static String MOCKED_COLUMNHEADER_RESOURCENAME = "Mocked Text: columnHeader_ResourceName";
    final static String MOCKED_COLUMNHEADER_QUEUENAME = "Mocked Text: columnHeader_QueueName";
    final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: columnHeader_Action";
    final static String MOCKED_ESSINK = "Mocked Text: MOCKED_ESSINK";
    final static String MOCKED_UPDATESINK = "Mocked Text: MOCKED_UPDATESINK";
    final static String MOCKED_DUMMYSINK = "Mocked Text: MOCKED_DUMMYSINK";

    class ViewConcrete extends View {
        public ViewConcrete() {
            super();
        }

        @Override
        public Texts getTexts() {
            return mockedTexts;
        }
    }

    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedMenuTexts.menu_Sinks()).thenReturn("Header Text");
        when(mockedTexts.label_Sinks()).thenReturn(MOCKED_LABEL_SINKS);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
        when(mockedTexts.columnHeader_Type()).thenReturn(MOCKED_COLUMNHEADER_TYPE);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Description()).thenReturn(MOCKED_COLUMNHEADER_DESCRIPTION);
        when(mockedTexts.columnHeader_ResourceName()).thenReturn(MOCKED_COLUMNHEADER_RESOURCENAME);
        when(mockedTexts.columnHeader_QueueName()).thenReturn(MOCKED_COLUMNHEADER_QUEUENAME);
        when(mockedTexts.columnHeader_Action()).thenReturn(MOCKED_COLUMNHEADER_ACTION);
        when(mockedTexts.selection_ESSink()).thenReturn(MOCKED_ESSINK);
        when(mockedTexts.selection_UpdateSink()).thenReturn(MOCKED_UPDATESINK);
        when(mockedTexts.selection_DummySink()).thenReturn(MOCKED_DUMMYSINK);
    }


    /*
     * Testing starts here...
     */
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        view = new ViewConcrete();
        view.injector = mockedViewGinjector;

        // Verify invocations
        verify(view.sinksTable).addRangeChangeHandler(any(RangeChangeEvent.Handler.class));
        verify(view.sinksTable).setRowCount(0, true);
        verify(view.sinksTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_TYPE));
        verify(view.sinksTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.sinksTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESCRIPTION));
        verify(view.sinksTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_QUEUENAME));
        verify(view.sinksTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION));
        verify(view.sinksTable).setSelectionModel(any(SelectionModel.class));
        verify(view.sinksTable).addDomHandler(any(DoubleClickHandler.class), any(DomEvent.Type.class));
        verifyNoMoreInteractions(view.sinksTable);
    }

    @Test
    public void constructor_setupData_dataSetupCorrect() {

        // Setup
        view = new ViewConcrete();
        List<SinkModel> models = view.dataProvider.getList();

        assertThat(models.isEmpty(), is(true));

        // Subject Under Test
        view.setSinks(testModels);

        assertThat(models.isEmpty(), is(false));
        assertThat(models.size(), is(2));
        assertThat(models.get(0).getSinkName(), is(testModel1.getSinkName()));
        assertThat(models.get(1).getSinkName(), is(testModel2.getSinkName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSinkTypeColumn_call_correctlySetup() {

        // Setup
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructTypeColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(MOCKED_DUMMYSINK));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSinkNameColumn_call_correctlySetup() {

        // Setup
        view = new View();

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getSinkName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDescriptionColumn_call_correctlySetup() {
        // Setup
        view = new View();

        // Subject Under Test
        Column column = view.constructDescriptionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getDescription()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructActionColumn_call_correctlySetup() {

        // Setup
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructActionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(mockedTexts.button_Edit()));

        // Test that the right action is activated upon click
        view.setPresenter(mockedPresenter);
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(3, testModel1, "Updated Button Text");  // Simulate a click on the column
        verify(mockedPresenter).editSink(testModel1);
    }

}
