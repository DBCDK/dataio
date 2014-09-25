
package dk.dbc.dataio.gui.client.pages.faileditems;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest {
    private ClientFactory mockedClientFactory;
    private Texts mockedConstants;
    private AcceptsOneWidget mockedContainerWidget;
    private EventBus mockedEventBus;
    private View mockedView;
    private Widget mockedWidget;

    private PresenterImpl presenterImpl;

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        mockedClientFactory = mock(ClientFactory.class);
        mockedConstants = mock(Texts.class);
        mockedContainerWidget = mock(AcceptsOneWidget.class);
        mockedEventBus = mock(EventBus.class);
        mockedView = mock(View.class);
        mockedWidget = mock(Widget.class);
        when(mockedClientFactory.getFaileditemsView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedWidget);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        PresenterImpl presenterImpl = new PresenterImpl(mockedClientFactory, mockedConstants);

        assertThat(presenterImpl.texts, is(mockedConstants));
        assertThat(presenterImpl.clientFactory, is(mockedClientFactory));
    }

    @Test
    public void start_callStart_objectStartedInitialized() {
        PresenterImpl presenterImpl = new PresenterImpl(mockedClientFactory, mockedConstants);

        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedWidget);
        assertThat(presenterImpl.failedItemsDataProvider.getList().size(), is(3));  // Only until proxy implementation is done
        verify(mockedView).setFailedItemsDataProvider(presenterImpl.failedItemsDataProvider);
    }

    @Test
    public void failedItemSelected_callFailedItemSelected_showJavaScriptLog() {
        createAndInitializePresenter();

        presenterImpl.failedItemSelected("123");

        verify(mockedView).setErrorText("Selected item id: 123");
    }


    private void createAndInitializePresenter() {
        presenterImpl = new PresenterImpl(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }

}