package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.gui.client.components.MultiProgressBar;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
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
@SuppressWarnings("deprecation")  // Due to the fact, that com.google.gwt.user.client.Element is deprecated, and MultiProgressBar.getElement() returns this element, so I am forced to use it!
@RunWith(GwtMockitoTestRunner.class)
public class ProgressColumnTest {
    // Mocked data
    @Mock EventBus mockedEventBus;
    @Mock Resources mockedResources;
    @Mock Cell<ImageResource> mockedCell;
    @Mock Cell.Context mockedContext;
    @Mock Element mockedElement;
    @Mock static Event mockedBrowserClickEvent;
    @Mock MultiProgressBar mockedMultiProgressBar;
    @Mock SafeHtmlBuilder mockedHtmlBuilder;

    @Before
    public void setupMockedEvents() {
        when(mockedBrowserClickEvent.getType()).thenReturn("click");
    }


    // Test data
    private JobModel legalTestModel = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014.5000_records.xml3473603508877630498.tmp", "150014", "SubmitterName",
            "FlowBinderName", "SinkName",
            true, 100, 10, 0, 0, 41, 23, 12,
            "packaging", "format", "charset", "destination", "mailNotification", "mailProcessing", "resultMailInitials");
    private JobModel illegalTestModel1 = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014.5000_records.xml3473603508877630498.tmp", "150014", "SubmitterName",
            "FlowBinderName", "SinkName",
            true, 100, 10, 0, 0, 41, 23, 43,
            "packaging", "format", "charset", "destination", "mailNotification", "mailProcessing", "resultMailInitials");
    private JobModel illegalTestModel2 = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014.5000_records.xml3473603508877630498.tmp", "150014", "SubmitterName",
            "FlowBinderName", "SinkName",
            true, 20, 10, 0, 0, 41, 23, 12,
            "packaging", "format", "charset", "destination", "mailNotification", "mailProcessing", "resultMailInitials");

    // Subject Under Test
    ProgressColumn progressColumn;


    // Test Constructor
    @Test
    public void progressColumn_constructor_correctlySetup() {

        // Test Subject Under Test
        progressColumn = new ProgressColumn();
        assertThat(progressColumn.getCell() instanceof ProgressColumn.ProgressCell, is(true));
    }

    @Test
    public void progressCell_constructor_correctlySetup() {
        ProgressColumn.ProgressCell progressCell = new ProgressColumn.ProgressCell();
    }

    @Test
    public void progressCell_renderWithNullProgressBar_noAction() {
        ProgressColumn.ProgressCell progressCell = new ProgressColumn.ProgressCell();

        // Test Subject Under Test
        progressCell.render(mockedContext, null, mockedHtmlBuilder);

        // Verification
        verifyNoMoreInteractions(mockedHtmlBuilder);
    }

    @Test
    public void progressCell_renderWithProgressBar_renderHtml() {
        // Setup Test
        final String INNERHTML = "<inner html>";
        ProgressColumn.ProgressCell progressCell = new ProgressColumn.ProgressCell();
        Element mockedElement = mock(Element.class);
        when(mockedMultiProgressBar.getElement()).thenReturn(mockedElement);
        when(mockedElement.getInnerHTML()).thenReturn(INNERHTML);

        // Test Subject Under Test
        progressCell.render(mockedContext, mockedMultiProgressBar, mockedHtmlBuilder);
        // Verification
        verify(mockedHtmlBuilder).appendHtmlConstant(INNERHTML);
    }

    // Test getValue(...)
    @Test
    public void getValue_doneWithoutErrorModel_returnOK() {
        // Setup Test
        progressColumn = new ProgressColumn();

        // Test Subject Under Test
        MultiProgressBar progressBar = progressColumn.getValue(legalTestModel);

        // Verification
        verify(progressBar.textProgress).setText("12/11/77"); // 12=12, 11=max(0,23-12), 77=100-23
        verify(progressBar.firstProgress).setAttribute("value", "12");
        verify(progressBar.secondProgress).setAttribute("value", "23");
    }

    @Test
    public void getValue_doneWithErrorModel1_returnOK() {
        // Setup Test
        progressColumn = new ProgressColumn();

        // Test Subject Under Test
        MultiProgressBar progressBar = progressColumn.getValue(illegalTestModel1);

        // Verification
        verify(progressBar.textProgress).setText("43/0/77"); // 43=43, 0=max(0,12-43), 77=100-23
        verify(progressBar.firstProgress).setAttribute("value", "43");
        verify(progressBar.secondProgress).setAttribute("value", "23");
    }

    @Test
    public void getValue_doneWithErrorModel2_returnOK() {
        // Setup Test
        progressColumn = new ProgressColumn();

        // Test Subject Under Test
        MultiProgressBar progressBar = progressColumn.getValue(illegalTestModel2);

        // Verification
        verify(progressBar.textProgress).setText("12/11/0"); // 12=12, 11=max(0,23-12), 77=max(0,20-23)
        verify(progressBar.firstProgress).setAttribute("value", "12");
        verify(progressBar.secondProgress).setAttribute("value", "23");
    }

}
