package dk.dbc.dataio.gui.client.panels.statuspopup;

import com.google.gwt.dom.client.Element;
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
    @Mock private EventBus mockedEventBus;
    @Mock private Element mockedParent;
    JobModel model = new JobModel("2014-12-17 00:37:48", "1418773068083", "urn:dataio-fs:46551", "424242",
            true, JobErrorCode.NO_ERROR,
            6, 1, 2, 3,    // Chunkifying: total, success, failure, ignored
            9, 2, 3, 4,    // Processing:  total, success, failure, ignored
            12, 3, 4, 5);  // Delivering:  total, success, failure, ignored

    /*
     * Subject Under Test
     */
    private StatusPopup mockedStatusPopup;


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        mockedStatusPopup = new StatusPopup(mockedEventBus, mockedParent, model);

        verify(mockedStatusPopup.totalFailed).setText(" 6");
        verify(mockedStatusPopup.chunkifyingSuccess).setText("1");
        verify(mockedStatusPopup.chunkifyingFailed).setText("2");
        verify(mockedStatusPopup.chunkifyingIgnored).setText("3");
        verify(mockedStatusPopup.processingSuccess).setText("2");
        verify(mockedStatusPopup.processingFailed).setText("3");
        verify(mockedStatusPopup.processingIgnored).setText("4");
        verify(mockedStatusPopup.deliveringSuccess).setText("3");
        verify(mockedStatusPopup.deliveringFailed).setText("4");
        verify(mockedStatusPopup.deliveringIgnored).setText("5");
    }

}
