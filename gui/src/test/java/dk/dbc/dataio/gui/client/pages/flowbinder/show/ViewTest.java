package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
    @Mock ClientFactory mockedClientFactory;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock static ClickEvent mockedClickEvent;


    // Test Data
//    private FlowComponentModel flowComponentModel1 = new FlowComponentModel(58L, 485L, "FCnam1", "FCspr1", "FCsrv1", "FCijs1", "FCmet1", Collections.singletonList("Java Script 1"), "description");
    private FlowComponentModel flowComponentModel1 = new FlowComponentModelBuilder().build();

    private FlowModel flowModel1 = new FlowModel(14L, 343L, "Fnam1", "Fdsc1", Arrays.asList(flowComponentModel1));
    private SinkModel sinkModel1 = new SinkModelBuilder().setName("SInam1").build();
    private FlowBinderModel flowBinderModel1 = new FlowBinderModel(123L, 111L, "FBnam1", "FBdsc1", "FBpac1", "FBfor1", "FBchr1", "FBdes1", "FBrec1", true, flowModel1, Collections.singletonList(new SubmitterModelBuilder().build()), sinkModel1);
    private FlowBinderModel flowBinderModel2 = new FlowBinderModel(124L, 112L, "FBnam2", "FBdsc2", "FBpac2", "FBfor2", "FBchr2", "FBdes2", "FBrec2", true, flowModel1, Collections.singletonList(new SubmitterModelBuilder().build()), sinkModel1);
    private List<FlowBinderModel> flowBinderModels = Arrays.asList(flowBinderModel1, flowBinderModel2);

    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_LABEL_FLOWBINDERS = "Mocked Text: Flowbinders";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: Navn";
    final static String MOCKED_COLUMNHEADER_DESCRIPTION = "Mocked Text: Beskrivelse";
    final static String MOCKED_COLUMNHEADER_PACKAGING = "Mocked Text: Rammeformat";
    final static String MOCKED_COLUMNHEADER_FORMAT = "Mocked Text: Indholdsformat";
    final static String MOCKED_COLUMNHEADER_CHARSET = "Mocked Text: Tegnsæt";
    final static String MOCKED_COLUMNHEADER_DESTINATION = "Mocked Text: Destination";
    final static String MOCKED_COLUMNHEADER_RECORDSPLITTER = "Mocked Text: Recordssplitter";
    final static String MOCKED_COLUMNHEADER_SUBMITTERS = "Mocked Text: Submittere";
    final static String MOCKED_COLUMNHEADER_FLOW = "Mocked Text: Flow";
    final static String MOCKED_COLUMNHEADER_SINK = "Mocked Text: Sink";
    final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: Handling";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: Rediger";
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedClientFactory.getFlowBindersShowTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowBinders()).thenReturn("Header Text");

        when(mockedTexts.label_FlowBinders()).thenReturn(MOCKED_LABEL_FLOWBINDERS);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Description()).thenReturn(MOCKED_COLUMNHEADER_DESCRIPTION);
        when(mockedTexts.columnHeader_Packaging()).thenReturn(MOCKED_COLUMNHEADER_PACKAGING);
        when(mockedTexts.columnHeader_Format()).thenReturn(MOCKED_COLUMNHEADER_FORMAT);
        when(mockedTexts.columnHeader_Charset()).thenReturn(MOCKED_COLUMNHEADER_CHARSET);
        when(mockedTexts.columnHeader_Destination()).thenReturn(MOCKED_COLUMNHEADER_DESTINATION);
        when(mockedTexts.columnHeader_RecordSplitter()).thenReturn(MOCKED_COLUMNHEADER_RECORDSPLITTER);
        when(mockedTexts.columnHeader_Submitters()).thenReturn(MOCKED_COLUMNHEADER_SUBMITTERS);
        when(mockedTexts.columnHeader_Flow()).thenReturn(MOCKED_COLUMNHEADER_FLOW);
        when(mockedTexts.columnHeader_Sink()).thenReturn(MOCKED_COLUMNHEADER_SINK);
        when(mockedTexts.columnHeader_Action()).thenReturn(MOCKED_COLUMNHEADER_ACTION);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
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
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESCRIPTION));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_PACKAGING));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FORMAT));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_CHARSET));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESTINATION));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_RECORDSPLITTER));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_SUBMITTERS));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FLOW));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_SINK));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION));
    }


    @Test
    public void constructor_setupData_dataSetupCorrect() {
        view = new View(mockedClientFactory);

        List<FlowBinderModel> models = view.dataProvider.getList();

        assertThat(models.isEmpty(), is(true));

        // Subject Under Test
        view.setFlowBinders(flowBinderModels);

        assertThat(models.isEmpty(), is(false));
        assertThat(models.size(), is(2));
        assertThat(models.get(0).getName(), is(flowBinderModel1.getName()));
        assertThat(models.get(1).getName(), is(flowBinderModel2.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructNameColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(flowBinderModel1.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDescriptionColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructDescriptionColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(flowBinderModel1.getDescription()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructPackagingColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructPackagingColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(flowBinderModel1.getPackaging()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFormatColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructFormatColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(flowBinderModel1.getFormat()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructCharsetColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructCharsetColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(flowBinderModel1.getCharset()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDestinationColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructDestinationColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(flowBinderModel1.getDestination()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructRecordSplitterColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructRecordSplitterColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(flowBinderModel1.getRecordSplitter()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSubmittersColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructSubmittersColumn();

        // Test that correct getValue handler has been setup - remember format: "name (number)"
        assertThat((String) column.getValue(flowBinderModel1),
                is(flowBinderModel1.getSubmitterModels().get(0).getNumber() + " ("
                        + flowBinderModel1.getSubmitterModels().get(0).getName() + ")"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructFlowColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(flowBinderModel1.getFlowModel().getFlowName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSinkColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructSinkColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(flowBinderModel1.getSinkModel().getSinkName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructActionColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructActionColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowBinderModel1), is(mockedTexts.button_Edit()));

        // Test that the right action is activated upon click
        view.setPresenter(mockedPresenter);
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(334, flowBinderModel1, "Updated Button Text");  // Simulate a click on the column
        verify(mockedPresenter).editFlowBinder(flowBinderModel1);
    }

}