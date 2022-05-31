package dk.dbc.dataio.gui.client.pages.sink.status;


import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {
    @Mock
    PlaceController mockedPlaceController;
    @Mock
    Texts mockedTexts;
    @Mock
    View mockedView;
    @Mock
    Widget mockedViewWidget;
    @Mock
    ViewGinjector mockedViewGinjector;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedViewGinjector.getView()).thenReturn(mockedView);
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedCommonGinjector.getJobStoreProxyAsync()).thenReturn(mockedJobStore);
    }

    @Before
    public void setupTexts() {
        when(mockedMenuTexts.menu_SinkStatus()).thenReturn("UshHarvestersMenu");
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;


    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {
        setupPresenterImpl();

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedViewGinjector, times(3)).getView();
        verify(mockedView).setHeader("Sink Status");
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedJobStore).getSinkStatusModels(any(AsyncCallback.class));
        verifyNoMoreInteractionsOnMocks();
    }


    // Private methods

    private void verifyNoMoreInteractionsOnMocks() {
        verifyNoMoreInteractions(mockedViewGinjector);
        verifyNoMoreInteractions(mockedView);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedFlowStore);
        verifyNoMoreInteractions(mockedMenuTexts);
        verifyNoMoreInteractions(mockedTexts);
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImpl(mockedPlaceController, "Sink Status");
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
    }
}
