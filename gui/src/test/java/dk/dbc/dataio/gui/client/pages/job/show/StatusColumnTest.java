package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Event;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
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
    @Mock EventBus mockedEventBus;
    @Mock
    Resources mockedResources;
    @Mock Cell<ImageResource> mockedCell;
    @Mock Cell.Context mockedContext;
    @Mock Element mockedElement;
    @Mock static Event mockedBrowserClickEvent;
    @Before
    public void setupMockedEvents() {
        when(mockedBrowserClickEvent.getType()).thenReturn("click");
    }


    // Test data
    private JobModel doneWithoutErrorModel = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014.5000_records.xml3473603508877630498.tmp", "150014",
            true, JobErrorCode.NO_ERROR,
            4, 4, 0, 0,   // Chunkifying: total, success, failure, ignored
            5, 5, 0, 0,   // Processing:  total, success, failure, ignored
            6, 6, 0, 0);  // Delivering:  total, success, failure, ignored
    private JobModel doneWithErrorCodeModel = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014.5000_records.xml3473603508877630498.tmp", "150014",
            true, JobErrorCode.DATA_FILE_INVALID,  // Marks the model with an error
            4, 4, 0, 0,   // Chunkifying: total, success, failure, ignored
            5, 5, 0, 0,   // Processing:  total, success, failure, ignored
            6, 6, 0, 0);  // Delivering:  total, success, failure, ignored
    private JobModel doneWithErrorCountModel = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014.5000_records.xml3473603508877630498.tmp", "150014",
            true, JobErrorCode.NO_ERROR,
            4, 3, 1, 0,   // Chunkifying: total, success, failure, ignored  // One chunkify failure marks the model with an error
            5, 5, 0, 0,   // Processing:  total, success, failure, ignored
            6, 6, 0, 0);  // Delivering:  total, success, failure, ignored
    private JobModel notDoneModel = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014.5000_records.xml3473603508877630498.tmp", "150014",
            false, JobErrorCode.NO_ERROR,  // Job not done marks the model as Not Done
            4, 4, 0, 0,   // Chunkifying: total, success, failure, ignored
            5, 5, 0, 0,   // Processing:  total, success, failure, ignored
            6, 6, 0, 0);  // Delivering:  total, success, failure, ignored

    // Subject Under Test
    StatusColumn statusColumn;


    // Test Constructor
    @Test
    public void statusColumn_constructor_correctlySetup() {
        assertThat(true, is(true));

        // Test Subject Under Test
        statusColumn = new StatusColumn(mockedEventBus, mockedResources, mockedCell);
    }

    // Test onBrowserEvent(...)
    @Test
    public void onBrowserEvent_callOnBrowserEvent_statusPopupPanelCreated() {
        statusColumn = new StatusColumn(mockedEventBus, mockedResources, mockedCell);

        // Test Subject Under Test
        statusColumn.onBrowserEvent(mockedContext, mockedElement, doneWithoutErrorModel, mockedBrowserClickEvent);

        // Here we know, that the StatusPopupPanel is opened, but there is currently no way that we can detect that
        // However, (at least) we do test, that an exception is not thrown
    }

    // Test getValue(...)
    @Test
    public void getValueAndGetJobStatus_doneWithoutErrorModel_returnGreen() {
        statusColumn = new StatusColumn(mockedEventBus, mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(doneWithoutErrorModel), is(mockedResources.green()));
    }

    @Test
    public void getValueAndGetJobStatus_doneWithErrorCodeModel_returnRed() {
        statusColumn = new StatusColumn(mockedEventBus, mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(doneWithErrorCodeModel), is(mockedResources.red()));
    }

    @Test
    public void getValueAndGetJobStatus_doneWithErrorCountModel_returnRed() {
        statusColumn = new StatusColumn(mockedEventBus, mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(doneWithErrorCountModel), is(mockedResources.red()));
    }

    @Test
    public void getValueAndGetJobStatus_notDoneModel_returnGray() {
        statusColumn = new StatusColumn(mockedEventBus, mockedResources, mockedCell);

        // Test Subject Under Test
        assertThat(statusColumn.getValue(notDoneModel), is(mockedResources.gray()));
    }

}
