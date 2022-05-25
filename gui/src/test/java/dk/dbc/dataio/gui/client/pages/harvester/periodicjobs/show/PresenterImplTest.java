package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
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

    @Before
    public void setupExpectations() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync())
                .thenReturn(mockedFlowStore);
        when(mockedCommonGinjector.getMenuTexts())
                .thenReturn(mockedMenuTexts);
        when(mockedCommonGinjector.getProxyErrorTexts())
                .thenReturn(mockedProxyErrorTexts);
        when(mockedViewGinjector.getView())
                .thenReturn(mockedView);
        when(mockedView.asWidget())
                .thenReturn(mockedViewWidget);
    }

    @Before
    public void createPresenterImpl() {
        presenterImpl = new dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show.PresenterImplTest.TestablePresenterImpl();
    }

    private dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show.PresenterImplTest.TestablePresenterImpl presenterImpl;

    // Test specialization of presenter to enable test of callbacks
    class TestablePresenterImpl extends PresenterImpl {
        public TestablePresenterImpl() {
            super(mockedPlaceController);
            commonInjector = mockedCommonGinjector;
            viewInjector = mockedViewGinjector;
        }

        public FetchHarvesterConfigsCallback fetchHarvesterConfigsCallback =
                new FetchHarvesterConfigsCallback();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void start() {
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(mockedViewGinjector, times(3)).getView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(isNull());
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllPeriodicJobsHarvesterConfigs(any(AsyncCallback.class));
        verifyNoMoreInteractions(mockedViewGinjector);
        verifyNoMoreInteractions(mockedView);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedFlowStore);
    }

    @Test
    public void onErrorWhenFetchingHarvesterConfigs() {
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.fetchHarvesterConfigsCallback.onFilteredFailure(new Exception());

        verify(mockedView).setErrorText(isNull());
    }

    @Test
    public void onSuccessWhenFetchingHarvesterConfigs() {
        final List<PeriodicJobsHarvesterConfig> configs = new ArrayList<>();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.fetchHarvesterConfigsCallback.onSuccess(configs);

        verify(mockedView).setHarvesters(configs);
    }
}
