package dk.dbc.dataio.gui.client.pages.flow.show;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
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

    @Mock
    Presenter mockedPresenter;
    @Mock
    dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock
    CommonGinjector mockedCommonGinjector;
    @Mock
    ViewGinjector mockedViewGinjector;

    // Test Data
    private FlowComponentModel flowComponentModel1 = new FlowComponentModelBuilder().setName("FCnam1").setSvnRevision("FCsrv1").setSvnNext("FCsrv1").build();
    private FlowComponentModel flowComponentModel2 = new FlowComponentModelBuilder().setName("FCnam2").setSvnRevision("FCsrv2").setSvnNext("FCsrv4").build();
    private FlowComponentModel flowComponentModel3 = new FlowComponentModelBuilder().setName("FCnam3").setSvnRevision("FCsrv3").setSvnNext("").build();
    private FlowModel flowModel1 = new FlowModelBuilder().setName("Fnam1").setTimeOfFlowComponentUpdate("2016-11-18 15:24:40").setComponents(Collections.singletonList(flowComponentModel1)).build();
    private FlowModel flowModel2 = new FlowModelBuilder().setName("Fnam2").setComponents(Arrays.asList(flowComponentModel2, flowComponentModel3)).build();
    private FlowModel flowModel3 = new FlowModelBuilder().setName("Fnam3").setComponents(Collections.singletonList(flowComponentModel3)).build();
    private List<FlowModel> flowModels = Arrays.asList(flowModel1, flowModel2);

    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock
    static Texts mockedTexts;
    final static String MOCKED_LABEL_FLOWS = "Mocked Text: label_Flows";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: columnHeader_Name";
    final static String MOCKED_COLUMNHEADER_DESCRIPTION = "Mocked Text: columnHeader_Description";
    final static String MOCKED_COLUMNHEADER_FLOWCOMPONENTS = "Mocked Text: columnHeader_FlowComponents";
    final static String MOCKED_COLUMNHEADER_TIME_OF_FLOW_COMPONENT_UPDATE = "Mocked Text: columnHeader_TimeOfFlowComponentUpdate";
    final static String MOCKED_COLUMNHEADER_ACTION_REFRESH = "Mocked Text: columnHeader_Action_Refresh";
    final static String MOCKED_COLUMNHEADER_ACTION_EDIT = "Mocked Text: columnHeader_Action_edit";
    final static String MOCKED_BUTTON_REFRESH = "Mocked Text: button_Refresh";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: button_Edit";

    class ViewConcrete extends View {
        public ViewConcrete() {
            super();
            commonInjector = mockedCommonGinjector;
            viewInjector = mockedViewGinjector;
        }

        @Override
        public Texts getTexts() {
            return mockedTexts;
        }
    }

    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Flows()).thenReturn("Header Text");

        when(mockedTexts.label_Flows()).thenReturn(MOCKED_LABEL_FLOWS);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Description()).thenReturn(MOCKED_COLUMNHEADER_DESCRIPTION);
        when(mockedTexts.columnHeader_FlowComponents()).thenReturn(MOCKED_COLUMNHEADER_FLOWCOMPONENTS);
        when(mockedTexts.columnHeader_TimeOfFlowComponentUpdate()).thenReturn(MOCKED_COLUMNHEADER_TIME_OF_FLOW_COMPONENT_UPDATE);
        when(mockedTexts.columnHeader_Action_Refresh()).thenReturn(MOCKED_COLUMNHEADER_ACTION_REFRESH);
        when(mockedTexts.columnHeader_Action_Edit()).thenReturn(MOCKED_COLUMNHEADER_ACTION_EDIT);
        when(mockedTexts.button_Refresh()).thenReturn(MOCKED_BUTTON_REFRESH);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        setupView();

        // Verify invocations
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESCRIPTION));
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FLOWCOMPONENTS));
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_TIME_OF_FLOW_COMPONENT_UPDATE));
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION_REFRESH));
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION_EDIT));
    }

    @Test
    public void setFlows_callSetupFlows_dataSetupCorrect() {
        setupView();

        List<FlowModel> models = view.dataProvider.getList();
        assertThat(models.isEmpty(), is(true));

        // Subject Under Test
        view.setFlows(flowModels);

        assertThat(models.isEmpty(), is(false));
        assertThat(models.size(), is(2));
        assertThat(models.get(0).getFlowName(), is(flowModel1.getFlowName()));
        assertThat(models.get(1).getFlowName(), is(flowModel2.getFlowName()));
    }

    @Test
    public void setFlows_callSetupFlowsTwice_dataSetupCorrect() {
        setupView();
        List<FlowModel> models = view.dataProvider.getList();

        // Subject Under Test
        view.setFlows(flowModels);
        view.setFlows(Collections.singletonList(flowModel3));  // The second call clears the existing flowModels, and puts flowModel3 only

        assertThat(models.isEmpty(), is(false));
        assertThat(models.size(), is(1));
        assertThat(models.get(0).getFlowName(), is(flowModel3.getFlowName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructNameColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowModel1), is(flowModel1.getFlowName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDescriptionColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructDescriptionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowModel1), is(flowModel1.getDescription()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructTimeOfFlowComponentUpdate_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructTimeOfFlowComponentUpdateColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowModel1), is(flowModel1.getTimeOfFlowComponentUpdate()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowComponentsColumn_oneFlowComponentWithSvnNext_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructFlowComponentsColumn();

        // Test that correct getValue handler has been setup
        String expected = flowComponentModel1.getName() + " (SVN Rev. " + flowComponentModel1.getSvnRevision() + ", SVN Next. " + flowComponentModel1.getSvnNext() + ")";
        assertThat(column.getValue(flowModel1), is(expected));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowComponentsColumn_oneFlowComponentWithEmptySvnNext_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructFlowComponentsColumn();

        // Test that correct getValue handler has been setup
        String expected = flowComponentModel3.getName() + " (SVN Rev. " + flowComponentModel3.getSvnRevision() + ")";
        assertThat(column.getValue(flowModel3), is(expected));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowComponentsColumn_twoFlowComponents_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructFlowComponentsColumn();

        // Test that correct getValue handler has been setup
        String expected =
                flowComponentModel2.getName() + " (SVN Rev. " + flowComponentModel2.getSvnRevision() + ", SVN Next. " + flowComponentModel2.getSvnNext() + "), " +
                        flowComponentModel3.getName() + " (SVN Rev. " + flowComponentModel3.getSvnRevision() + ")";
        assertThat(column.getValue(flowModel2), is(expected));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructRefreshActionColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructRefreshActionColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowModel1), is(mockedTexts.button_Refresh()));

        // Test that the right action is activated upon click
        view.setPresenter(mockedPresenter);
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(334, flowModel1, "Updated Button Text");  // Simulate a click on the column
        verify(mockedPresenter).refreshFlowComponents(flowModel1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructEditActionColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructEditActionColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowModel1), is(mockedTexts.button_Edit()));

        // Test that the right action is activated upon click
        view.setPresenter(mockedPresenter);
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(334, flowModel1, "Updated Button Text");  // Simulate a click on the column
        verify(mockedPresenter).editFlow(flowModel1);
    }

    private void setupView() {
        view = new ViewConcrete();
    }
}
