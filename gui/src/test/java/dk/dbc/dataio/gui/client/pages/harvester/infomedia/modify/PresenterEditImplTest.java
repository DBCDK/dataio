package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest {
    @Mock
    EditPlace editPlace;
    @Mock
    CommonGinjector commonInjector;
    @Mock
    FlowStoreProxyAsync flowStoreProxyAsync;

    private PresenterEditImpl presenter;

    @Before
    public void setupMocks() {
        when(commonInjector.getFlowStoreProxyAsync()).thenReturn(flowStoreProxyAsync);
    }

    @Before
    public void createPresenter() {
        when(editPlace.getHarvesterId()).thenReturn(1L);
        presenter = new PresenterEditImpl(editPlace, "test");
        presenter.commonInjector = commonInjector;
        presenter.config = new InfomediaHarvesterConfig(1, 2,
                new InfomediaHarvesterConfig.Content()
                        .withId("-id-")
                        .withSchedule("-schedule-")
                        .withDescription("-description-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withEnabled(false));
    }

    @Test
    public void initializeModel() {
        presenter.initializeModel();
        verify(flowStoreProxyAsync).getInfomediaHarvesterConfig(
                eq(1L), any(PresenterEditImpl.GetInfomediaHarvesterConfigAsyncCallback.class));
    }

    @Test
    public void saveModel() {
        presenter.saveModel();
        verify(flowStoreProxyAsync).updateHarvesterConfig(
                eq(presenter.config),
                any(PresenterEditImpl.UpdateInfomediaHarvesterConfigAsyncCallback.class));
    }

    @Test
    public void deleteButtonPressed() {
        presenter.deleteButtonPressed();
        verify(flowStoreProxyAsync).deleteHarvesterConfig(
                eq(1L), eq(2L),
                any(PresenterEditImpl.DeleteInfomediaHarvesterConfigAsyncCallback.class));
    }
}
