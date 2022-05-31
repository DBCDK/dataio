package dk.dbc.dataio.gui.client.pages;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import org.mockito.Mock;

/**
 * Created by ThomasBerg on 10/11/15.
 */
public abstract class PresenterImplTestBase {
    @Mock
    protected FlowStoreProxyAsync mockedFlowStore;
    @Mock
    protected JobStoreProxyAsync mockedJobStore;
    @Mock
    protected PlaceController mockedPlaceController;
    @Mock
    protected AcceptsOneWidget mockedContainerWidget;
    @Mock
    protected EventBus mockedEventBus;
    @Mock
    protected ProxyErrorTexts mockedProxyErrorTexts;
    @Mock
    protected dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock
    protected CommonGinjector mockedCommonGinjector;
    @Mock
    protected Exception mockedException;
    @Mock
    protected ProxyException mockedProxyException;

    protected final String header = "Header Text";
}
