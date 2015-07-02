package dk.dbc.dataio.gui.client.pages.flow.show;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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


    // Test Data
    private FlowComponentModel flowComponentModel1 = new FlowComponentModelBuilder().setName("FCnam1").setSvnRevision("FCsrv1").build();
    private FlowComponentModel flowComponentModel2 = new FlowComponentModelBuilder().setName("FCnam2").setSvnRevision("FCsrv2").build();
    private FlowComponentModel flowComponentModel3 = new FlowComponentModelBuilder().setName("FCnam3").setSvnRevision("FCsrv3").build();
    private FlowModel flowModel1 = new FlowModel(14L, 343L, "Fnam1", "Fdsc1", Collections.singletonList(flowComponentModel1));
    private FlowModel flowModel2 = new FlowModel(15L, 344L, "Fnam2", "Fdsc2", Arrays.asList(flowComponentModel2, flowComponentModel3));
    private FlowModel flowModel3 = new FlowModel(16L, 345L, "Fnam3", "Fdsc3", Collections.singletonList(flowComponentModel3));
    private List<FlowModel> flowModels = Arrays.asList(flowModel1, flowModel2);

    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_LABEL_FLOWS = "Mocked Text: label_Flows";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: columnHeader_Name";
    final static String MOCKED_COLUMNHEADER_DESCRIPTION = "Mocked Text: columnHeader_Description";
    final static String MOCKED_COLUMNHEADER_FLOWCOMPONENTS = "Mocked Text: columnHeader_FlowComponents";
    final static String MOCKED_COLUMNHEADER_ACTION_REFRESH = "Mocked Text: columnHeader_Action_Refresh";
    final static String MOCKED_COLUMNHEADER_ACTION_EDIT = "Mocked Text: columnHeader_Action_edit";
    final static String MOCKED_BUTTON_REFRESH = "Mocked Text: button_Refresh";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: button_Edit";
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedClientFactory.getFlowsShowTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Flows()).thenReturn("Header Text");

        when(mockedTexts.label_Flows()).thenReturn(MOCKED_LABEL_FLOWS);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Description()).thenReturn(MOCKED_COLUMNHEADER_DESCRIPTION);
        when(mockedTexts.columnHeader_FlowComponents()).thenReturn(MOCKED_COLUMNHEADER_FLOWCOMPONENTS);
        when(mockedTexts.columnHeader_Action_Refresh()).thenReturn(MOCKED_COLUMNHEADER_ACTION_REFRESH);
        when(mockedTexts.columnHeader_Action_Edit()).thenReturn(MOCKED_COLUMNHEADER_ACTION_EDIT);
        when(mockedTexts.button_Refresh()).thenReturn(MOCKED_BUTTON_REFRESH);
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
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESCRIPTION));
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FLOWCOMPONENTS));
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION_REFRESH));
        verify(view.flowsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION_EDIT));
    }


    @Test
    public void setFlows_callSetupFlows_dataSetupCorrect() {
        view = new View(mockedClientFactory);

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
        view = new View(mockedClientFactory);
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
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowModel1), is(flowModel1.getFlowName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDescriptionColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructDescriptionColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowModel1), is(flowModel1.getDescription()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowComponentsColumn_oneFlowComponent_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructFlowComponentsColumn();

        // Test that correct getValue handler has been setup
        String expected = flowComponentModel1.getName() + " (SVN Rev. " + flowComponentModel1.getSvnRevision() + ")";
        assertThat((String) column.getValue(flowModel1), is(expected));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowComponentsColumn_twoFlowComponents_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructFlowComponentsColumn();

        // Test that correct getValue handler has been setup
        String expected =
                flowComponentModel2.getName() + " (SVN Rev. " + flowComponentModel2.getSvnRevision() + "), " +
                flowComponentModel3.getName() + " (SVN Rev. " + flowComponentModel3.getSvnRevision() + ")";
        assertThat((String) column.getValue(flowModel2), is(expected));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructRefreshActionColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

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
        view = new View(mockedClientFactory);

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

}