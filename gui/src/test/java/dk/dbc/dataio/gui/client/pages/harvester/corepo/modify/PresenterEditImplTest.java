package dk.dbc.dataio.gui.client.pages.harvester.corepo.modify;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
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
public class PresenterEditImplTest extends PresenterImplTestBase {
    @Mock
    private EditPlace mockedPlace;
    @Mock
    private Texts mockedTexts;
    @Mock
    private View mockedView;
    @Mock
    private PromptedTextBox mockedName;
    @Mock
    private PromptedTextArea mockedDescription;
    @Mock
    private PromptedTextBox mockedResource;
    @Mock
    private PromptedList mockedRrHarvester;
    @Mock
    private PromptedCheckBox mockedEnabled;
    @Mock
    private Label mockedStatus;
    @Mock
    private Button mockedDeleteButton;

    @Mock
    private Widget mockedWidget;


    private PresenterEditImpl presenter;


    /*
     * Test data
     */
    private final CoRepoHarvesterConfig.Content content =
            new CoRepoHarvesterConfig.Content().
                    withName("Name123").
                    withDescription("Description123").
                    withResource("Resource123").
                    withRrHarvester(22).
                    withEnabled(true);
    private final CoRepoHarvesterConfig coHarvesterConfig = new CoRepoHarvesterConfig(123L, 234L, content);


    /**
     * Test Preparation
     */
    @Before
    public void commonTestPreparation() {
        when(mockedPlace.getHarvesterId()).thenReturn(123L);
        presenter = new PresenterEditImpl(mockedPlace, "Header Text");
        when(presenter.viewInjector.getView()).thenReturn(mockedView);
        when(presenter.viewInjector.getTexts()).thenReturn(mockedTexts);
        mockedView.name = mockedName;
        mockedView.description = mockedDescription;
        mockedView.resource = mockedResource;
        mockedView.rrHarvester = mockedRrHarvester;
        mockedView.enabled = mockedEnabled;
        mockedView.status = mockedStatus;
        mockedView.deleteButton = mockedDeleteButton;
        when(mockedView.asWidget()).thenReturn(mockedWidget);
        when(presenter.commonInjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(presenter.commonInjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        List<RRHarvesterConfig> rrConfigs = new ArrayList<>();
        rrConfigs.add(new RRHarvesterConfig(11L, 1L, new RRHarvesterConfig.Content().withId("RR1")));
        rrConfigs.add(new RRHarvesterConfig(22L, 2L, new RRHarvesterConfig.Content().withId("RR2")));
        rrConfigs.add(new RRHarvesterConfig(33L, 3L, new RRHarvesterConfig.Content().withId("RR3")));
        presenter.setAvailableRrHarvesterConfigs(rrConfigs);
    }

    @Before
    public void prepareTexts() {
        when(mockedTexts.status_ConfigSuccessfullySaved()).thenReturn("status_ConfigSuccessfullySaved");
    }


    /**
     * Run tests
     */
    @Test
    public void constructor_valid_ok() {
        commonPostVerification();
    }

    @Test
    public void start_valid_ok() {  // Includes call to (and test of) initializeModel()
        // Test
        presenter.start(mockedContainerWidget, mockedEventBus);

        // Test validation
        verifyStart();
        verify(presenter.commonInjector, times(2)).getFlowStoreProxyAsync();
        verify(mockedFlowStore).getCoRepoHarvesterConfig(any(Long.class), any(PresenterEditImpl.GetCoRepoHarvesterConfigAsyncCallback.class));
        commonPostVerification();
    }

    @Test
    public void saveModel_ok_modelSaved() {
        // Test preparation
        presenter.start(mockedContainerWidget, mockedEventBus);

        // Test
        presenter.saveModel();

        // Test validation
        verifyStart();
        verify(presenter.commonInjector, times(3)).getFlowStoreProxyAsync();
        verify(mockedFlowStore).getCoRepoHarvesterConfig(any(Long.class), any(PresenterEditImpl.GetCoRepoHarvesterConfigAsyncCallback.class));
        verify(mockedFlowStore).updateHarvesterConfig(isNull(), any(PresenterEditImpl.UpdateCoRepoHarvesterConfigAsyncCallback.class));
        commonPostVerification();
    }

    @Test
    public void GetCoRepoHarvesterConfigsAsyncCallback_onFailure_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetCoRepoHarvesterConfigAsyncCallback callback = presenter.new GetCoRepoHarvesterConfigAsyncCallback();
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.PRECONDITION_FAILED);

        // Test
        callback.onFailure(mockedProxyException);

        // Test validation
        verify(presenter.commonInjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_preconditionFailedError();
        commonPostVerification();
    }

    @Test
    public void GetCoRepoHarvesterConfigsAsyncCallback_onSuccessNotFound_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetCoRepoHarvesterConfigAsyncCallback callback = presenter.new GetCoRepoHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(null);

        // Test validation
        verify(mockedTexts).error_HarvesterNotFound();
        commonPostVerification();
    }

    @Test
    public void GetCoRepoHarvesterConfigsAsyncCallback_onSuccessFound_ok() {
        // Test preparation
        PresenterEditImpl.GetCoRepoHarvesterConfigAsyncCallback callback = presenter.new GetCoRepoHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(coHarvesterConfig);

        // Test validation
        verify(mockedName).setText("Name123");
        verify(mockedName).setEnabled(true);
        verify(mockedDescription).setText("Description123");
        verify(mockedDescription).setEnabled(true);
        verify(mockedResource).setText("Resource123");
        verify(mockedResource).setEnabled(true);
        verifyRrHarvester(mockedRrHarvester, "22", true);
        verify(mockedEnabled).setValue(true);
        verify(mockedEnabled).setEnabled(true);
        verify(mockedStatus, times(1)).setText("");
        commonPostVerification();
    }

    @Test
    public void UpdateCoRepoHarvesterConfigAsyncCallback_onFailure_errorMessage() {
        // Test preparation
        PresenterEditImpl.UpdateCoRepoHarvesterConfigAsyncCallback callback = presenter.new UpdateCoRepoHarvesterConfigAsyncCallback();
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.PRECONDITION_FAILED);

        // Test
        callback.onFailure(mockedProxyException);

        // Test validation
        verify(presenter.commonInjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_preconditionFailedError();
        commonPostVerification();
    }

    @Test
    public void UpdateCoRepoHarvesterConfigAsyncCallback_onSuccess_errorMessage() {
        // Test preparation
        PresenterEditImpl.UpdateCoRepoHarvesterConfigAsyncCallback callback = presenter.new UpdateCoRepoHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(coHarvesterConfig);

        // Test validation
        verify(mockedTexts).status_ConfigSuccessfullySaved();
        verify(mockedStatus).setText("status_ConfigSuccessfullySaved");
        commonPostVerification();
    }


    /*
     * Private methods
     */

    private void verifyStart() {
        verifyInitializeViewMocks();
        verifyInitializeViewFields();
        verify(mockedContainerWidget).setWidget(mockedWidget);
        verify(mockedFlowStore).findAllRRHarvesterConfigs(any(PresenterImpl.FetchAvailableRRHarvesterConfigsCallback.class));
    }

    private void verifyInitializeViewMocks() {
        verify(mockedView).setHeader("Header Text");
        verify(mockedView).setPresenter(presenter);
    }

    private void verifyInitializeViewFields() {
        verify(mockedName).setText("");
        verify(mockedName).setEnabled(false);
        verify(mockedDescription).setText("");
        verify(mockedDescription).setEnabled(false);
        verify(mockedResource).setText("");
        verify(mockedResource).setEnabled(false);
        verifyRrHarvester(mockedRrHarvester, "", false);
        verify(mockedRrHarvester).setEnabled(false);
        verify(mockedEnabled).setValue(false);
        verify(mockedEnabled).setEnabled(false);
        verify(mockedStatus).setText("");
    }

    private void verifyRrHarvester(PromptedList list, String value, boolean enabled) {
        verify(list).clear();
        verify(list).addAvailableItem("RR1", "11");
        verify(list).addAvailableItem("RR2", "22");
        verify(list).addAvailableItem("RR3", "33");
        verify(list).setSelectedValue(value);
        verify(list).setEnabled(enabled);
    }

    private void commonPostVerification() {
        verifyNoMoreInteractions(mockedTexts);
        verifyNoMoreInteractions(presenter.commonInjector);
        verifyNoMoreInteractions(mockedContainerWidget);
        verifyNoMoreInteractions(mockedEventBus);
        verifyNoMoreInteractions(mockedName);
        verifyNoMoreInteractions(mockedDescription);
        verifyNoMoreInteractions(mockedResource);
        verifyNoMoreInteractions(mockedRrHarvester);
        verifyNoMoreInteractions(mockedEnabled);
        verifyNoMoreInteractions(mockedStatus);
        verifyNoMoreInteractions(mockedFlowStore);
        verifyNoMoreInteractions(mockedProxyErrorTexts);
    }

}
