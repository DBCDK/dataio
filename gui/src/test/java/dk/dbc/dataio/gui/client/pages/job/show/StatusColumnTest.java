package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Event;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.StateModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.StateElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;


/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class StatusColumnTest {
    // Mocked data
    @Mock
    Resources mockedResources;
    @Mock
    Cell<ImageResource> mockedCell;
    @Mock
    static Event mockedBrowserClickEvent;
    @Mock
    ImageResource gray;
    @Mock
    ImageResource green;
    @Mock
    ImageResource red;
    @Mock
    ImageResource yellow;

    @Before
    public void setupMockedEvents() {
        when(mockedBrowserClickEvent.getType()).thenReturn("click");
        when(mockedResources.gray()).thenReturn(gray);
        when(mockedResources.green()).thenReturn(green);
        when(mockedResources.red()).thenReturn(red);
        when(mockedResources.yellow()).thenReturn(yellow);
    }


    // Test data
    private JobModel doneWithoutErrorModel = new JobModel()
            .withNumberOfItems(10)
            .withNumberOfChunks(1)
            .withJobCompletionTime(new Date().toString());

    private JobModel doneWithErrorModel = new JobModel()
            .withNumberOfItems(10)
            .withNumberOfChunks(1)
            .withJobCompletionTime(new Date().toString())
            .withStateModel(new StateModel().withPartitioning(new StateElement().withFailed(1).withSucceeded(9)));

    private JobModel notDoneModel = new JobModel().withStateModel(new StateModel());

    private JobModel previewModel = new JobModel()
            .withNumberOfItems(10)
            .withNumberOfChunks(0)
            .withJobCompletionTime(new Date().toString());

    // Subject Under Test
    StatusColumn statusColumn;


    // Test Constructor
    @Test
    public void statusColumn_constructor_correctlySetup() {
        assertThat(true, is(true));

        // Test Subject Under Test
        statusColumn = new StatusColumn(mockedResources, mockedCell);
    }

    // Test getValue(...)
    @Test
    public void getValueAndGetJobStatus_doneWithoutErrorModel_returnGreen() {
        statusColumn = new StatusColumn(mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(doneWithoutErrorModel), is(mockedResources.green()));
    }

    @Test
    public void getValueAndGetJobStatus_doneWithErrorModel_returnRed() {
        statusColumn = new StatusColumn(mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(doneWithErrorModel), is(mockedResources.red()));
    }

    @Test
    public void getValueAndGetJobStatus_notDoneModel_returnGray() {
        statusColumn = new StatusColumn(mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(notDoneModel), is(mockedResources.gray()));
    }

    @Test
    public void getValueAndGetJobStatus_previewModel_returnYellow() {
        statusColumn = new StatusColumn(mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(previewModel), is(mockedResources.yellow()));
    }

}
