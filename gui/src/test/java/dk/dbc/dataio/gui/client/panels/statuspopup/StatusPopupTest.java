package dk.dbc.dataio.gui.client.panels.statuspopup;

import com.google.gwt.dom.client.Element;
import com.google.gwtmockito.GwtMock;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class StatusPopupTest {
    @Mock private ClientFactory mockedClientFactory;
    @Mock private EventBus eventBus;
    @Mock private Element parent;
    JobModel model = new JobModel("2014-12-17 00:37:48", "1418773068083", "urn:dataio-fs:46551", "424242",
            true, JobErrorCode.NO_ERROR,
            6, 1, 2, 3,    // Chunkifying: total, success, failure, ignored
            9, 2, 3, 4,    // Processing:  total, success, failure, ignored
            12, 3, 4, 5);  // Delivering:  total, success, failure, ignored

    /*
     * Subject Under Test
     */
    @GwtMock private StatusPopup statusPopup;


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        statusPopup = new StatusPopup(eventBus, parent, model);

        verify(statusPopup.totalFailed).setText(" 6");
        verify(statusPopup.chunkifyingSuccess).setText("1");
        verify(statusPopup.chunkifyingFailed).setText("2");
        verify(statusPopup.chunkifyingIgnored).setText("3");
        verify(statusPopup.processingSuccess).setText("2");
        verify(statusPopup.processingFailed).setText("3");
        verify(statusPopup.processingIgnored).setText("4");
        verify(statusPopup.deliveringSuccess).setText("3");
        verify(statusPopup.deliveringFailed).setText("4");
        verify(statusPopup.deliveringIgnored).setText("5");
    }

}
