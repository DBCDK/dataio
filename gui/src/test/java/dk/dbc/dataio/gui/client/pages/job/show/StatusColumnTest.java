package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Event;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;

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
    @Mock Resources mockedResources;
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
            "150014", "SubmitterName",
            "FlowBinderName", 6789L, "SinkName",
            true, 10, 10, 0, 0, 41, 42, 43, new ArrayList<DiagnosticModel>(),
            "packaging", "format", "charset", "destination", "mailNotification", "mailProcessing", "resultMailInitials");
    private JobModel doneWithErrorModel = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014", "SubmitterName",
            "FlowBinderName", 6789L, "SinkName",
            true, 10, 10, 5, 5, 44, 45, 46, new ArrayList<DiagnosticModel>(),
            "packaging", "format", "charset", "destination", "mailNotification", "mailProcessing", "resultMailInitials");
    private JobModel notDoneModel = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014", "SubmitterName",
            "FlowBinderName", 6789L, "SinkName",
            false, 10, 5, 0, 0, 47, 48, 49, new ArrayList<DiagnosticModel>(),// Job not done marks the model as Not Done
            "packaging", "format", "charset", "destination", "mailNotification", "mailProcessing", "resultMailInitials");

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

}
