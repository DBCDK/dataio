package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
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
    private SubmitterModel testModel1 = new SubmitterModel(12345L, 1111L, "564738", "Submitter Name 1", "Submitter Description 1");
    private SubmitterModel testModel2 = new SubmitterModel(12346L, 2222L, "564739", "Submitter Name 2", "Submitter Description 2");
    private List<SubmitterModel> testModels = new ArrayList<SubmitterModel>(Arrays.asList(testModel1, testModel2));

    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_LABEL_SUBMITTERS = "Mocked Text: Submittere";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: Rediger";
    final static String MOCKED_COLUMNHEADER_NUMBER = "Mocked Text: Nummer";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: Navn";
    final static String MOCKED_COLUMNHEADER_DESCRIPTION = "Mocked Text: Beskrivelse";
    final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: Handling";
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedClientFactory.getSubmittersShowTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Submitters()).thenReturn("Header Text");

        when(mockedTexts.label_Submitters()).thenReturn(MOCKED_LABEL_SUBMITTERS);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
        when(mockedTexts.columnHeader_Number()).thenReturn(MOCKED_COLUMNHEADER_NUMBER);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Description()).thenReturn(MOCKED_COLUMNHEADER_DESCRIPTION);
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
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NUMBER));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESCRIPTION));
        verify(view.submittersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION));
    }


    @Test
    public void constructor_setupData_dataSetupCorrect() {
        view = new View(mockedClientFactory);

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
    @SuppressWarnings("unchecked")
    public void constructSubmitterNumberColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructSubmitterNumberColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getNumber()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructNameColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDescriptionColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructDescriptionColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getDescription()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructActionColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructActionColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(mockedTexts.button_Edit()));

        // Test that the right action is activated upon click
        view.setPresenter(mockedPresenter);
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(3, testModel1, "Updated Button Text");  // Simulate a click on the column
        verify(mockedPresenter).editSubmitter(testModel1);
    }

}