package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.ListBoxHasValue;
import dk.dbc.dataio.gui.client.components.popup.PopupListBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowBinderModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
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
    private Presenter mockedPresenter;
    @Mock
    private dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock
    private ViewGinjector mockedViewInjector;
    @Mock
    private CommonGinjector mockedCommonInjector;
    @Mock
    private Texts mockedTexts;
    @Mock
    private PopupListBox mockedPopupList;
    @Mock
    private ListBoxHasValue mockedListBox;


    // Test Data
    private SubmitterModel testModel1 = new SubmitterModelBuilder().setEnabled(true).setNumber("564738").setName("Submitter Name 1").setDescription("Submitter Description 1").build();
    private SubmitterModel testModel2 = new SubmitterModelBuilder().setNumber("564739").setName("Submitter Name 2").setDescription("Submitter Description 2").setEnabled(false).build();
    private List<SubmitterModel> testModels = new ArrayList<>(Arrays.asList(testModel1, testModel2));

    // Subject Under Test
    private View view;

    // Mocked Texts
    private final static String MOCKED_LABEL_SUBMITTERS = "Mocked Text: Submittere";
    private final static String MOCKED_BUTTON_EDIT = "Mocked Text: Rediger";
    private final static String MOCKED_BUTTON_SHOWFLOWBINDERS = "Mocked Text: button_ShowFlowBinders";
    private final static String MOCKED_COLUMNHEADER_NUMBER = "Mocked Text: Nummer";
    private final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: Navn";
    private final static String MOCKED_COLUMNHEADER_DESCRIPTION = "Mocked Text: Beskrivelse";
    private final static String MOCKED_COLUMNHEADER_FLOWBINDERS = "Mocked Text: columnHeader_FlowBinders";
    private final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: Handling";
    private final static String MOCKED_COLUMNHEADER_STATUS = "Mocked Text: Tilstand";

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
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonInjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Submitters()).thenReturn("Header Text");
        when(mockedTexts.label_Submitters()).thenReturn(MOCKED_LABEL_SUBMITTERS);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
        when(mockedTexts.button_ShowFlowBinders()).thenReturn(MOCKED_BUTTON_SHOWFLOWBINDERS);
        when(mockedTexts.columnHeader_Number()).thenReturn(MOCKED_COLUMNHEADER_NUMBER);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Description()).thenReturn(MOCKED_COLUMNHEADER_DESCRIPTION);
        when(mockedTexts.columnHeader_FlowBinders()).thenReturn(MOCKED_COLUMNHEADER_FLOWBINDERS);
        when(mockedTexts.columnHeader_Action()).thenReturn(MOCKED_COLUMNHEADER_ACTION);
        when(mockedTexts.columnHeader_Status()).thenReturn(MOCKED_COLUMNHEADER_STATUS);
        when(mockedTexts.value_Disabled()).thenReturn("disabled");
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
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NUMBER));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESCRIPTION));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_STATUS));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FLOWBINDERS));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION));
    }


    @Test
    public void constructor_setupData_dataSetupCorrect() {
        setupView();

        List<SubmitterModel> models = view.dataProvider.getList();

        assertThat(models.isEmpty(), is(true));

        // Subject Under Test
        view.setSubmitters(testModels);

        assertThat(models.isEmpty(), is(false));
        assertThat(models.size(), is(2));
        assertThat(models.get(0).getName(), is(testModel1.getName()));
        assertThat(models.get(1).getName(), is(testModel2.getName()));
    }

    @Test
    public void showFlowBinders_nullFlowBinderList_noAction() {
        setupView();

        // Subject Under Test
        view.showFlowBinders(null);

        // Verify Test
        verifyNoInteractions(view.popupList);
    }

    @Test
    public void showFlowBinders_validFlowBinders_fillPopupList() {
        setupView();

        // Subject Under Test
        view.showFlowBinders(Arrays.asList(
                new FlowBinderModelBuilder().setId(111).setName("one").build(),
                new FlowBinderModelBuilder().setId(222).setName("two").build(),
                new FlowBinderModelBuilder().setId(333).setName("three").build()
        ));

        // Verify Test
        verify(view.popupList).clear();
        verify(view.popupList).addItem("one", "111");
        verify(view.popupList).addItem("three", "333");
        verify(view.popupList).addItem("two", "222");
        verify(view.popupList).show();
        verifyNoMoreInteractions(view.popupList);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSubmitterNumberColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructSubmitterNumberColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getNumber()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructNameColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDescriptionColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructDescriptionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getDescription()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructStatusColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructStatusColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(""));
        assertThat(column.getValue(testModel2), is("disabled"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowBindersColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructFlowBindersColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(mockedTexts.button_ShowFlowBinders()));

        // Test that the right action is activated upon click
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(37, testModel1, "Show FlowBinders Button Text");  // Simulate a click on the column
        verify(mockedPresenter).showFlowBinders(testModel1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructActionColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructActionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(mockedTexts.button_Edit()));

        // Test that the right action is activated upon click
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(3, testModel1, "Updated Button Text");  // Simulate a click on the column
        verify(mockedPresenter).editSubmitter(testModel1);
    }

    @Test
    public void setPopupListButtonPressed_nullEvent_noAction() {
        setupView();

        // Subject Under Test
        view.setPopupListButtonPressed(null);

        // Test that correct getValue handler has been setup
        verifyNoInteractions(mockedPresenter);
    }

    @Test
    public void setPopupListButtonPressed_extraButtonNotPressed_noAction() {
        setupView();

        // Subject Under Test
        DialogEvent okEvent = new DialogEvent() {
            @Override
            public DialogButton getDialogButton() {
                return DialogButton.OK_BUTTON;
            }
        };
        view.setPopupListButtonPressed(okEvent);

        // Test that correct getValue handler has been setup
        verifyNoInteractions(mockedPresenter);
    }

    @Test
    public void setPopupListButtonPressed_extraButtonPressed_noAction() {
        setupView();
        DialogEvent okEvent = new DialogEvent() {
            @Override
            public DialogButton getDialogButton() {
                return DialogButton.EXTRA_BUTTON;
            }
        };
        Map<String, String> testList = new HashMap<String, String>() {{
            put("1111", "The One And Only");
            put("2222", "The Second Round");
        }};
        when(mockedListBox.getItemCount()).thenReturn(2);
        when(mockedPopupList.getValue()).thenReturn(testList);

        // Subject Under Test
        view.setPopupListButtonPressed(okEvent);

        // Test that correct getValue handler has been setup
        verify(mockedPopupList).getValue();
        verify(mockedPopupList).getContentWidget();
        verifyNoMoreInteractions(mockedPopupList);
        verify(mockedPresenter).copyFlowBinderListToClipboard(testList);
        verifyNoMoreInteractions(mockedPresenter);
        verify(mockedListBox).setMultipleSelect(true);
        verify(mockedListBox).getItemCount();
        verify(mockedListBox).setItemSelected(0, true);
        verify(mockedListBox).setItemSelected(1, true);
        verifyNoMoreInteractions(mockedListBox);
    }


    /*
     * Private methods
     */

    private void setupView() {
        view = new ViewConcrete();
        view.commonInjector = mockedCommonInjector;
        view.viewInjector = mockedViewInjector;
        view.popupList = mockedPopupList;
        view.setPresenter(mockedPresenter);
        when(mockedPopupList.getContentWidget()).thenReturn(mockedListBox);
    }

}
