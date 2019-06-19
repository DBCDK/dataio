/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterCreateImplTest {
    @Mock CommonGinjector commonInjector;
    @Mock FlowStoreProxyAsync flowStoreProxyAsync;

    private PresenterCreateImpl presenter;

    @Before
    public void setupMocks() {
        when(commonInjector.getFlowStoreProxyAsync()).thenReturn(flowStoreProxyAsync);
    }

    @Before
    public void createPresenter() {
        presenter = new PresenterCreateImpl("test");
        presenter.commonInjector = commonInjector;
        presenter.config = new InfomediaHarvesterConfig(1, 1,
                new InfomediaHarvesterConfig.Content()
                        .withId("-id-")
                        .withSchedule("-schedule-")
                        .withDescription("-description-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withEnabled(false));
    }

    @Test
    public void saveButtonPressed() {
        presenter.saveButtonPressed();
        verify(flowStoreProxyAsync).createInfomediaHarvesterConfig(
                eq(presenter.config), any(AsyncCallback.class));
    }
}