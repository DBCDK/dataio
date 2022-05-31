package dk.dbc.dataio.gui.client.pages.harvester.corepo.show;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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
    View mockedView;
    @Mock
    Widget mockedViewWidget;
    @Mock
    ViewGinjector mockedViewGinjector;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getView()).thenReturn(mockedView);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    private List<CoRepoHarvesterConfig> testHarvesterConfig = new ArrayList<>();


    // Subject Under Test
    private PresenterImplConcrete presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete() {
            super(mockedPlaceController);
            viewInjector = mockedViewGinjector;
            commonInjector = mockedCommonGinjector;
        }

        GetCoRepoHarvestersCallback getHarvestersCallback = new GetCoRepoHarvestersCallback();
    }


    //------------------------------------------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {
        presenterImpl = new PresenterImplConcrete();

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedViewGinjector, times(3)).getView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(isNull());
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllCoRepoHarvesterConfigs(any(AsyncCallback.class));
        verifyNoMoreInteractions(mockedViewGinjector);
        verifyNoMoreInteractions(mockedView);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedFlowStore);
    }

    @Test
    public void getHarvesterRrConfigs_callbackWithError_errorMessageInView() {
        presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getHarvestersCallback.onFilteredFailure(new Exception());

        // Verify Test
        // The following is called from start()
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(isNull());
        verify(mockedView).asWidget();
        // The following is called from GetHarvestersCallback
        verify(mockedView).setErrorText(isNull());
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void getHarvesterRrConfigs_callbackWithSuccess_fetchHarvesterConfigs() {
        presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getHarvestersCallback.onSuccess(testHarvesterConfig);

        // Verify Test
        // The following is called from start()
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(isNull());
        verify(mockedView).asWidget();
        // The following is called from GetHarvestersCallback
        verify(mockedView).setHarvesters(testHarvesterConfig);
        verifyNoMoreInteractions(mockedView);
    }

}
