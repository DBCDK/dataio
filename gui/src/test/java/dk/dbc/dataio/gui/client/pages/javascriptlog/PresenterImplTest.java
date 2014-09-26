package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest {

    private ClientFactory mockedClientFactory;
    private EventBus mockedEventBus;
    private Widget mockedWidget;
    private View mockedView;
    private Texts mockedConstants;;
    private AcceptsOneWidget mockedContainerWidget;

    private PresenterImpl presenterImpl;
    private JavaScriptLogPlace mockedJavaScriptLogPlace;
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
        mockedClientFactory = mock(ClientFactory.class);
        mockedWidget = mock(Widget.class);
        mockedEventBus = mock(EventBus.class);
        mockedView = mock(View.class);
        mockedJavaScriptLogPlace = mock(JavaScriptLogPlace.class);
        mockedConstants = mock(Texts.class);
        mockedContainerWidget = mock(AcceptsOneWidget.class);
        mockedView.htmlLabel = mock(HTML.class);
        when(mockedClientFactory.getJavaScriptLogView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedWidget);
    }

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterImpl = new PresenterImpl(mockedJavaScriptLogPlace, mockedClientFactory, mockedConstants);
        assertThat(presenterImpl.texts, is(mockedConstants));
        assertThat(presenterImpl.clientFactory, is(mockedClientFactory));
    }

    @Test
    public void start_instantiateAndCallStart_viewInitializedCorrectly() {
        presenterImpl = new PresenterImpl(mockedJavaScriptLogPlace, mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        verify(mockedJavaScriptLogPlace).getFailedItemId();

        // TODO - no proxy call yet to test
        verify(mockedView.htmlLabel).setHTML(any(String.class));
    }

    @Test
    public void getJavaScriptLogFilteredAsyncCallback_successfulCallback_stringIsFormattedCorrectly() {
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
