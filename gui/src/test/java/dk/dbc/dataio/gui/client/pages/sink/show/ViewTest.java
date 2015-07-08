package dk.dbc.dataio.gui.client.pages.sink.show;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
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
    @Mock static ClickEvent mockedClickEvent;


    // Test Data
    private SinkModel testModel1 = new SinkModelBuilder().setName("SinkNam1").setResource("SinkRes1").setDescription("SinkDescription1").build();
    private SinkModel testModel2 = new SinkModelBuilder().setName("SinkNam2").setResource("SinkRes2").setDescription("SinkDescription2").build();
    private List<SinkModel> testModels = Arrays.asList(testModel1, testModel2);


    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_LABEL_SINKS = "Mocked Text: label_Sinks";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: button_Edit";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: columnHeader_Name";
    final static String MOCKED_COLUMNHEADER_RESOURCENAME = "Mocked Text: columnHeader_ResourceName";
    final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: columnHeader_Action";
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedClientFactory.getSinksShowTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Sinks()).thenReturn("Header Text");

        when(mockedTexts.label_Sinks()).thenReturn(MOCKED_LABEL_SINKS);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_ResourceName()).thenReturn(MOCKED_COLUMNHEADER_RESOURCENAME);
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
        verify(view.sinksTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.sinksTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_RESOURCENAME));
        verify(view.sinksTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION));
    }

    @Test
    public void constructor_setupData_dataSetupCorrect() {
        view = new View(mockedClientFactory);

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
    public void constructSinkNameColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getSinkName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructResourceNameColumn_call_correctlySetup() {
        view = new View(mockedClientFactory);

        // Subject Under Test
        Column column = view.constructResourceNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getResourceName()));
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
        verify(mockedPresenter).editSink(testModel1);
    }

}