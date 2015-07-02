package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
    private FlowComponentModel flowComponentModel1 = new FlowComponentModelBuilder().setName("FCnam1").setJavascriptModules(Arrays.asList("Java Script 1")).build();
    private FlowComponentModel flowComponentModel2 = new FlowComponentModelBuilder().setName("FCnam2").setJavascriptModules(Arrays.asList("Java Script 2", "Java Script 3")).build();
    private FlowComponentModel flowComponentModel3 = new FlowComponentModelBuilder().setName("FCnam3").setJavascriptModules(Arrays.asList("Java Script 4", "Java Script 5", "Java Script 6")).build();

    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_LABEL_FLOWCOMPONENTS = "Mocked Text: label_FlowComponents";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: button_Edit";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: columnHeader_Name";
    final static String MOCKED_COLUMNHEADER_SCRIPTNAME = "Mocked Text: columnHeader_ScriptName";
    final static String MOCKED_COLUMNHEADER_INVOCATIONMETHOD = "Mocked Text: columnHeader_InvocationMethod";
    final static String MOCKED_COLUMNHEADER_PROJECT = "Mocked Text: columnHeader_Project";
    final static String MOCKED_COLUMNHEADER_REVISION = "Mocked Text: columnHeader_Revision";
    final static String MOCKED_COLUMNHEADER_JAVASCRIPTMODULES = "Mocked Text: columnHeader_JavaScript";
    final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: columnHeader_Action";
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedClientFactory.getFlowComponentsShowTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowComponents()).thenReturn("Header Text");

        when(mockedTexts.label_FlowComponents()).thenReturn(MOCKED_LABEL_FLOWCOMPONENTS);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_ScriptName()).thenReturn(MOCKED_COLUMNHEADER_SCRIPTNAME);
        when(mockedTexts.columnHeader_InvocationMethod()).thenReturn(MOCKED_COLUMNHEADER_INVOCATIONMETHOD);
        when(mockedTexts.columnHeader_Project()).thenReturn(MOCKED_COLUMNHEADER_PROJECT);
        when(mockedTexts.columnHeader_Revision()).thenReturn(MOCKED_COLUMNHEADER_REVISION);
        when(mockedTexts.columnHeader_JavaScriptModules()).thenReturn(MOCKED_COLUMNHEADER_JAVASCRIPTMODULES);
        when(mockedTexts.columnHeader_Action()).thenReturn(MOCKED_COLUMNHEADER_ACTION);
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
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_SCRIPTNAME));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_INVOCATIONMETHOD));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_PROJECT));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_REVISION));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_JAVASCRIPTMODULES));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION));
    }

    @Test
    public void constructor_setupData_dataSetupCorrect() {
        view = new View(mockedClientFactory);

        List<FlowComponentModel> models = view.dataProvider.getList();

        assertThat(models.isEmpty(), is(true));

        // Subject Under Test
        view.setFlowComponents(Arrays.asList(flowComponentModel1, flowComponentModel2, flowComponentModel3));

        assertThat(models.isEmpty(), is(false));
        assertThat(models.size(), is(3));
        assertThat(models.get(0).getName(), is(flowComponentModel1.getName()));
        assertThat(models.get(1).getName(), is(flowComponentModel2.getName()));
        assertThat(models.get(2).getName(), is(flowComponentModel3.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowComponentNameColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowComponentModel1), is(flowComponentModel1.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowComponentJavaScriptNameColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructJavaScriptNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowComponentModel1), is(flowComponentModel1.getInvocationJavascript()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructInvocationMethodColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructInvocationMethodColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowComponentModel1), is(flowComponentModel1.getInvocationMethod()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSvnProjectColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructSvnProjectColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowComponentModel1), is(flowComponentModel1.getSvnProject()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSvnRevisionColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructSvnRevisionColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowComponentModel1), is(flowComponentModel1.getSvnRevision()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructJavaScriptModulesColumn_callWithOneJavaScript_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructJavaScriptModulesColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowComponentModel1), is(flowComponentModel1.getJavascriptModules().get(0)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructJavaScriptModulesColumn_callWithThreeJavaScript_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructJavaScriptModulesColumn();

        // Test that correct getValue handler has been setup
        String expectedValue = flowComponentModel3.getJavascriptModules().get(0) + ", "
                + flowComponentModel3.getJavascriptModules().get(1) + ", "
                + flowComponentModel3.getJavascriptModules().get(2);
        assertThat((String) column.getValue(flowComponentModel3), is(expectedValue));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructActionColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructActionColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(flowComponentModel1), is(mockedTexts.button_Edit()));

        // Test that the right action is activated upon click
        view.setPresenter(mockedPresenter);
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(3, flowComponentModel1, "Updated Button Text");  // Simulate a click on the column
        verify(mockedPresenter).editFlowComponent(flowComponentModel1);
    }

}