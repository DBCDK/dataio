package dk.dbc.dataio.gui.client.pages;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.mockito.Mock;

/**
 * Created by ThomasBerg on 10/11/15.
 */
public abstract class PresenterImplTestBase {
    @Mock protected ClientFactory mockedClientFactory;
    @Mock protected FlowStoreProxyAsync mockedFlowStore;
    @Mock protected PlaceController mockedPlaceController;
    @Mock protected AcceptsOneWidget mockedContainerWidget;
    @Mock protected EventBus mockedEventBus;
    @Mock protected ProxyErrorTexts mockedProxyErrorTexts;
    @Mock protected dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock protected CommonGinjector mockedCommonGinjector;

    protected final String header = "Header Text";
}
