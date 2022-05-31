package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
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
    private ViewGinjector mockedViewInjector;
    @Mock
    private CommonGinjector mockedCommonInjector;


    // Test Data
    private FlowComponentModel flowComponentModel1 = new FlowComponentModelBuilder().setName("FCnam1").setJavascriptModules(Collections.singletonList("Java Script 1")).build();
    private FlowComponentModel flowComponentModel2 = new FlowComponentModelBuilder().setName("FCnam2").setJavascriptModules(Arrays.asList("Java Script 2", "Java Script 3")).build();
    private FlowComponentModel flowComponentModel3 = new FlowComponentModelBuilder().setName("FCnam3").setJavascriptModules(Arrays.asList("Java Script 4", "Java Script 5", "Java Script 6")).build();
    private FlowComponentModel flowComponentModelNext = new FlowComponentModelBuilder().
            setSvnRevision("123").
            setJavascriptModules(Arrays.asList("Module 1", "Module 2", "Module 3")).
            setSvnNext("456").
            setNextJavascriptModules(Arrays.asList("Modulus 4", "Modulus 5")).
            build();

    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock
    static Texts mockedTexts;
    final static String MOCKED_LABEL_FLOWCOMPONENTS = "Mocked Text: label_FlowComponents";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: button_Edit";
    final static String MOCKED_BUTTON_CREATE = "Mocked Text: button_Create";
    final static String MOCKED_SHOW_JS_MODULES = "Mocked Text: button_ShowJsModules";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: columnHeader_Name";
    final static String MOCKED_COLUMNHEADER_DESCRIPTION = "Mocked Text: columnHeader_Description";
    final static String MOCKED_COLUMNHEADER_SCRIPTNAME = "Mocked Text: columnHeader_ScriptName";
    final static String MOCKED_COLUMNHEADER_INVOCATIONMETHOD = "Mocked Text: columnHeader_InvocationMethod";
    final static String MOCKED_COLUMNHEADER_PROJECT = "Mocked Text: columnHeader_Project";
    final static String MOCKED_COLUMNHEADER_REVISION = "Mocked Text: columnHeader_Revision";
    final static String MOCKED_COLUMNHEADER_NEXT = "Mocked Text: columnHeader_Next";
    final static String MOCKED_COLUMNHEADER_JAVASCRIPTMODULES = "Mocked Text: columnHeader_JavaScript";
    final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: columnHeader_Action";
    final static String MOCKED_HEADER_SVNREVISION = "Mocked Text: header_SVNRevision: <@REVISION@>";
    final static String MOCKED_HEADER_SVNNEXTREVISION = "Mocked Text: header_SVNNextRevision: <@REVISION@>";
    final static String MOCKED_HEADER_JSMODULESLISTPOPUP = "Mocked Text: header_JSModulesListPopup";

    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedViewInjector.getView()).thenReturn(view);
        when(mockedCommonInjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowComponents()).thenReturn("Header Text");

        when(mockedTexts.label_FlowComponents()).thenReturn(MOCKED_LABEL_FLOWCOMPONENTS);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
        when(mockedTexts.button_Create()).thenReturn(MOCKED_BUTTON_CREATE);
        when(mockedTexts.button_ShowJSModules()).thenReturn(MOCKED_SHOW_JS_MODULES);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Description()).thenReturn(MOCKED_COLUMNHEADER_DESCRIPTION);
        when(mockedTexts.columnHeader_ScriptName()).thenReturn(MOCKED_COLUMNHEADER_SCRIPTNAME);
        when(mockedTexts.columnHeader_InvocationMethod()).thenReturn(MOCKED_COLUMNHEADER_INVOCATIONMETHOD);
        when(mockedTexts.columnHeader_Project()).thenReturn(MOCKED_COLUMNHEADER_PROJECT);
        when(mockedTexts.columnHeader_Revision()).thenReturn(MOCKED_COLUMNHEADER_REVISION);
        when(mockedTexts.columnHeader_Next()).thenReturn(MOCKED_COLUMNHEADER_NEXT);
        when(mockedTexts.columnHeader_JavaScriptModules()).thenReturn(MOCKED_COLUMNHEADER_JAVASCRIPTMODULES);
        when(mockedTexts.columnHeader_Action()).thenReturn(MOCKED_COLUMNHEADER_ACTION);
        when(mockedTexts.header_SVNRevision()).thenReturn(MOCKED_HEADER_SVNREVISION);
        when(mockedTexts.header_SVNNextRevision()).thenReturn(MOCKED_HEADER_SVNNEXTREVISION);
        when(mockedTexts.header_JSModulesListPopup()).thenReturn(MOCKED_HEADER_JSMODULESLISTPOPUP);
    }

    public class ViewConcrete extends View {
        public ViewConcrete() {
            super();
            this.viewInjector = mockedViewInjector;
        }

        @Override
        public View getView() {
            return view;
        }

        @Override
        public Texts getTexts() {
            return mockedTexts;
        }
    }

    /*
     * Testing starts here...
     */
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        setupView();

        // Verify invocations
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESCRIPTION));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_SCRIPTNAME));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_INVOCATIONMETHOD));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_PROJECT));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_REVISION));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NEXT));
        verify(view.flowComponentsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION));
        verify(view.flowComponentsTable).setSelectionModel(view.selectionModel);
    }


    @Test
    public void constructor_setupData_dataSetupCorrect() {
        setupView();

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
        setupView();

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowComponentModel1), is(flowComponentModel1.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowComponentJavaScriptNameColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructJavaScriptNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowComponentModel1), is(flowComponentModel1.getInvocationJavascript()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructInvocationMethodColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructInvocationMethodColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowComponentModel1), is(flowComponentModel1.getInvocationMethod()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSvnProjectColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructSvnProjectColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowComponentModel1), is(flowComponentModel1.getSvnProject()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSvnRevisionColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructSvnRevisionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowComponentModel1), is(flowComponentModel1.getSvnRevision()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructActionColumn_getValue_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructActionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowComponentModel1), is(flowComponentModel1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructActionColumn_getCellStyleNames_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructActionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getCellStyleNames(null, null), is("button-cell"));
    }

    @Test
    public void showJsModules_call_correctlySetup() {
        setupView();

        // Subject Under Test
        view.showJsModules(flowComponentModelNext);

        // Test Verification
        verify(view.jsModulesPopup).setValue(
                view.jsModulesPopup.new DoubleListData(
                        "Mocked Text: header_SVNRevision: <123>",
                        Arrays.asList("Module 1", "Module 2", "Module 3"),
                        "Mocked Text: header_SVNNextRevision: <456>",
                        Arrays.asList("Modulus 4", "Modulus 5"))
        );
        verify(view.jsModulesPopup).setDialogTitle("Mocked Text: header_JSModulesListPopup");
        verify(view.jsModulesPopup).show();
        verifyNoMoreInteractions(view.jsModulesPopup);
    }

    private void setupView() {
        view = new ViewConcrete();
        view.viewInjector = mockedViewInjector;
    }
}
