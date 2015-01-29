package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest {

    @Mock ClientFactory mockedClientFactory;
    @Mock EventBus mockedEventBus;
    @Mock Widget mockedWidget;
    @Mock View mockedView;
    @Mock Texts mockedConstants;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock LogStoreProxyAsync mockedLogStoreProxy;
    @Mock JavaScriptLogPlace mockedJavaScriptLogPlace;

    private PresenterImpl presenterImpl;
    private PresenterImplConcrete presenterConcrete;
    private final static String NBSP = "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0";
    private final static String log = "A log with tab and new line is given as input";


    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(Place place, ClientFactory clientFactory, Texts texts) {
            super(place, clientFactory, texts);
            view = mockedView;
        }

        public GetJavaScriptLogFilteredAsyncCallback callback = new GetJavaScriptLogFilteredAsyncCallback();
    }

    @Before
    public void setupMockedObjects() {
        mockedView.htmlLabel = mock(HTML.class);
        when(mockedClientFactory.getJavaScriptLogView()).thenReturn(mockedView);
        when(mockedClientFactory.getLogStoreProxyAsync()).thenReturn(mockedLogStoreProxy);
        when(mockedView.asWidget()).thenReturn(mockedWidget);
    }

    @After
    public void tearDownMockedObjects() {
        reset(mockedClientFactory);
        reset(mockedEventBus);
        reset(mockedWidget);
        reset(mockedView.htmlLabel);
        reset(mockedView);
        reset(mockedConstants);
        reset(mockedContainerWidget);
        reset(mockedLogStoreProxy);
        reset(mockedJavaScriptLogPlace);
    }


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterImpl = new PresenterImpl(mockedJavaScriptLogPlace, mockedClientFactory, mockedConstants);
        assertThat(presenterImpl.texts, is(mockedConstants));
        assertThat(presenterImpl.clientFactory, is(mockedClientFactory));
    }

    @Test
    public void start_instantiateAndCallStart_javaScriptLogRetrievedFromLogStore() {
        presenterImpl = new PresenterImpl(mockedJavaScriptLogPlace, mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        verify(mockedJavaScriptLogPlace).getJobId();
        verify(mockedJavaScriptLogPlace).getChunkId();
        verify(mockedJavaScriptLogPlace).getFailedItemId();
        verify(mockedLogStoreProxy).getItemLog(
                eq(presenterImpl.jobId),
                eq(presenterImpl.chunkId),
                eq(presenterImpl.failedItemId),
                any(PresenterImpl.GetJavaScriptLogFilteredAsyncCallback.class));
    }

    @Test
    public void getJavaScriptLogFilteredAsyncCallback_successfulCallback_viewInitializedCorrectly() {
        presenterConcrete = new PresenterImplConcrete(mockedJavaScriptLogPlace, mockedClientFactory, mockedConstants);

        String logFile = "\t" + log + "\n";
        // Emulate a successful callback from flowstore
        presenterConcrete.callback.onSuccess(logFile);
        // Expect a the error text set in the label to be formatted correctly
        verify(mockedView.htmlLabel).setHTML(NBSP + log + "<br>");
    }

    @Test
    public void getJavaScriptLogFilteredAsyncCallback_unsuccessfulCallback_setErrorTextCalledInView() {
        presenterConcrete = new PresenterImplConcrete(mockedJavaScriptLogPlace, mockedClientFactory, mockedConstants);
        // Emulate an unsuccessful callback from flowstore
        presenterConcrete.callback.onFailure(new Throwable(mockedConstants.error_CannotFetchJavaScriptLog()));
        // Expect the error text to be set in View
        verify(mockedView).setErrorText(mockedConstants.error_CannotFetchJavaScriptLog());
    }
}
