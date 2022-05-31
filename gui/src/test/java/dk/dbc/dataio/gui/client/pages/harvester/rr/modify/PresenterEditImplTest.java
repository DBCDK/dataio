package dk.dbc.dataio.gui.client.pages.harvester.rr.modify;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.popup.PopupMapEntry;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedPasswordTextBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private PromptedTextBox mockedDescription;
    @Mock
    private PromptedTextBox mockedResource;
    @Mock
    private PromptedTextBox mockedTargetUrl;
    @Mock
    private PromptedTextBox mockedTargetGroup;
    @Mock
    private PromptedTextBox mockedTargetUser;
    @Mock
    private PromptedPasswordTextBox mockedTargetPassword;
    @Mock
    private PromptedTextBox mockedConsumerId;
    @Mock
    private PromptedTextBox mockedSize;
    @Mock
    private PromptedMultiList mockedFormatOverrides;
    @Mock
    private PromptedCheckBox mockedRelations;
    @Mock
    private PromptedCheckBox mockedExpand;
    @Mock
    private PromptedCheckBox mockedLibraryRules;
    @Mock
    private PromptedList mockedHarvesterType;
    @Mock
    private PromptedTextBox mockedHoldingsTarget;
    @Mock
    private PromptedTextBox mockedDestination;
    @Mock
    private PromptedTextBox mockedFormat;
    @Mock
    private PromptedList mockedType;
    @Mock
    private PromptedTextArea mockedNote;
    @Mock
    private PromptedCheckBox mockedEnabled;
    @Mock
    private Button mockedUpdateButton;
    @Mock
    private Label mockedStatus;
    @Mock
    private PopupMapEntry mockedPopupFormatOverrideEntry;
    @Mock
    private Widget mockedWidget;
    @Mock
    private Map mockedMap;
    @Mock
    private RRHarvesterConfig mockedConfig;
    @Mock
    private RRHarvesterConfig.Content mockedContent;
    @Mock
    private Button mockedDeleteButton;


    private PresenterEditImpl presenter;


    /**
     * Test data
     */
    private final HashMap<Integer, String> formatOverrides = new HashMap<>();
    private final RRHarvesterConfig.Content content =
            new RRHarvesterConfig.Content()
                    .withId("id123")
                    .withDescription("description123")
                    .withResource("resource123")
                    .withConsumerId("ConsumerId123")
                    .withBatchSize(1)
                    .withFormatOverrides(formatOverrides)
                    .withIncludeRelations(true)
                    .withHarvesterType(RRHarvesterConfig.HarvesterType.IMS)
                    .withImsHoldingsTarget("HoldingsTarget321")
                    .withDestination("Destination123")
                    .withFormat("Format123")
                    .withType(JobSpecification.Type.TEST)
                    .withNote("Note123")
                    .withEnabled(true);
    private final RRHarvesterConfig rrHarvesterConfig = new RRHarvesterConfig(123L, 234L, content);

    @Before
    public void prepareTestData() {
        formatOverrides.put(123, "value123");
        formatOverrides.put(1234, "value1234");
    }


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
        mockedView.consumerId = mockedConsumerId;
        mockedView.size = mockedSize;
        mockedView.formatOverrides = mockedFormatOverrides;
        mockedView.relations = mockedRelations;
        mockedView.expand = mockedExpand;
        mockedView.libraryRules = mockedLibraryRules;
        mockedView.harvesterType = mockedHarvesterType;
        mockedView.holdingsTarget = mockedHoldingsTarget;
        mockedView.destination = mockedDestination;
        mockedView.format = mockedFormat;
        mockedView.type = mockedType;
        mockedView.note = mockedNote;
        mockedView.enabled = mockedEnabled;
        mockedView.updateButton = mockedUpdateButton;
        mockedView.status = mockedStatus;
        mockedView.popupFormatOverrideEntry = mockedPopupFormatOverrideEntry;
        mockedView.deleteButton = mockedDeleteButton;
        when(mockedView.asWidget()).thenReturn(mockedWidget);
        when(mockedConfig.getContent()).thenReturn(mockedContent);
        when(presenter.commonInjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(presenter.commonInjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
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
        verify(presenter.commonInjector).getFlowStoreProxyAsync();
        verify(mockedFlowStore).getRRHarvesterConfig(any(Long.class), any(PresenterEditImpl.GetRRHarvesterConfigAsyncCallback.class));
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
        verify(presenter.commonInjector, times(2)).getFlowStoreProxyAsync();
        verify(mockedFlowStore).getRRHarvesterConfig(any(Long.class), any(PresenterEditImpl.GetRRHarvesterConfigAsyncCallback.class));
        verify(mockedFlowStore).updateHarvesterConfig(eq(presenter.model), any(PresenterEditImpl.UpdateHarvesterConfigAsyncCallback.class));
        commonPostVerification();
    }

    @Test
    public void GetHarvesterRrConfigsAsyncCallback_onFailure_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetRRHarvesterConfigAsyncCallback callback = presenter.new GetRRHarvesterConfigAsyncCallback();
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
    public void GetHarvesterRrConfigsAsyncCallback_onSuccessNotFound_errorMessage() {
        // Test preparation
        PresenterEditImpl.GetRRHarvesterConfigAsyncCallback callback = presenter.new GetRRHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(null);

        // Test validation
        verify(mockedTexts).error_HarvesterNotFound();
        commonPostVerification();
    }

    @Test
    public void GetHarvesterRrConfigsAsyncCallback_onSuccessFound_ok() {
        // Test preparation
        PresenterEditImpl.GetRRHarvesterConfigAsyncCallback callback = presenter.new GetRRHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(rrHarvesterConfig);

        // Test validation
        verify(mockedName).setText("id123");
        verify(mockedName).setEnabled(true);
        verify(mockedDescription).setText("description123");
        verify(mockedDescription).setEnabled(true);
        verify(mockedResource).setText("resource123");
        verify(mockedResource).setEnabled(true);
        verify(mockedConsumerId).setText("ConsumerId123");
        verify(mockedConsumerId).setEnabled(true);
        verify(mockedSize).setText("1");
        verify(mockedSize).setEnabled(true);
        verify(mockedView).setFormatOverrides(new HashMap<>());
        verify(mockedFormatOverrides).setEnabled(true);
        verify(mockedRelations).setValue(true);
        verify(mockedRelations).setEnabled(true);
        verify(mockedLibraryRules).setValue(false);
        verify(mockedLibraryRules).setEnabled(true);
        verify(mockedHarvesterType).clear();
        verify(mockedHarvesterType).addAvailableItem(RRHarvesterConfig.HarvesterType.STANDARD.toString());
        verify(mockedHarvesterType).addAvailableItem(RRHarvesterConfig.HarvesterType.IMS.toString());
        verify(mockedHarvesterType).addAvailableItem(RRHarvesterConfig.HarvesterType.WORLDCAT.toString());
        verify(mockedHarvesterType).setSelectedValue(RRHarvesterConfig.HarvesterType.IMS.toString());
        verify(mockedHarvesterType).setEnabled(true);
        verify(mockedHoldingsTarget).setText("HoldingsTarget321");
        verify(mockedHoldingsTarget).setEnabled(true);
        verify(mockedDestination).setText("Destination123");
        verify(mockedDestination).setEnabled(true);
        verify(mockedFormat).setText("Format123");
        verify(mockedFormat).setEnabled(true);
        verifyType(mockedType, "TEST", true);
        verify(mockedType).setEnabled(true);
        verify(mockedNote).setText("Note123");
        verify(mockedNote).setEnabled(true);
        verify(mockedEnabled).setValue(true);
        verify(mockedEnabled).setEnabled(true);
        verify(mockedStatus, times(1)).setText("");
        verify(mockedUpdateButton).setVisible(false);
        verify(mockedPopupFormatOverrideEntry).hide();
        commonPostVerification();
    }

    @Test
    public void UpdateHarvesterRrConfigAsyncCallback_onFailure_errorMessage() {
        // Test preparation
        PresenterEditImpl.UpdateHarvesterConfigAsyncCallback callback = presenter.new UpdateHarvesterConfigAsyncCallback();
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
    public void UpdateHarvesterRrConfigAsyncCallback_onSuccess_errorMessage() {
        // Test preparation
        PresenterEditImpl.UpdateHarvesterConfigAsyncCallback callback = presenter.new UpdateHarvesterConfigAsyncCallback();

        // Test
        callback.onSuccess(rrHarvesterConfig);

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
        verify(mockedConsumerId).setText("");
        verify(mockedConsumerId).setEnabled(false);
        verify(mockedSize).setText("");
        verify(mockedSize).setEnabled(false);
        verify(mockedView).setFormatOverrides(new HashMap<>());
        verify(mockedFormatOverrides).setEnabled(false);
        verify(mockedRelations).setValue(false);
        verify(mockedRelations).setEnabled(false);
        verify(mockedLibraryRules).setValue(false);
        verify(mockedLibraryRules).setEnabled(false);
        verify(mockedHarvesterType).clear();
        verify(mockedHarvesterType).addAvailableItem(RRHarvesterConfig.HarvesterType.STANDARD.toString());
        verify(mockedHarvesterType).addAvailableItem(RRHarvesterConfig.HarvesterType.IMS.toString());
        verify(mockedHarvesterType).addAvailableItem(RRHarvesterConfig.HarvesterType.WORLDCAT.toString());
        verify(mockedHarvesterType).setSelectedValue(RRHarvesterConfig.HarvesterType.STANDARD.toString());
        verify(mockedHarvesterType).setEnabled(false);
        verify(mockedHoldingsTarget).setText("");
        verify(mockedHoldingsTarget).setEnabled(false);
        verify(mockedDestination).setText("");
        verify(mockedDestination).setEnabled(false);
        verify(mockedFormat).setText("");
        verify(mockedFormat).setEnabled(false);
        verifyType(mockedType, "", false);
        verify(mockedType).setEnabled(false);
        verify(mockedNote).setText("");
        verify(mockedNote).setEnabled(false);
        verify(mockedEnabled).setValue(false);
        verify(mockedEnabled).setEnabled(false);
        verify(mockedStatus).setText("");
        verify(mockedUpdateButton).setVisible(false);
        verify(mockedPopupFormatOverrideEntry).hide();
    }

    private void verifyType(PromptedList list, String value, boolean enabled) {
        verify(list).clear();
        verify(list).addAvailableItem("TRANSIENT");
        verify(list).addAvailableItem("PERSISTENT");
        verify(list).addAvailableItem("TEST");
        verify(list).addAvailableItem("ACCTEST");
        verify(list).addAvailableItem("INFOMEDIA");
        verify(list).addAvailableItem("PERIODIC");
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
        verifyNoMoreInteractions(mockedTargetUrl);
        verifyNoMoreInteractions(mockedTargetGroup);
        verifyNoMoreInteractions(mockedTargetUser);
        verifyNoMoreInteractions(mockedTargetPassword);
        verifyNoMoreInteractions(mockedConsumerId);
        verifyNoMoreInteractions(mockedSize);
        verifyNoMoreInteractions(mockedFormatOverrides);
        verifyNoMoreInteractions(mockedRelations);
        verifyNoMoreInteractions(mockedLibraryRules);
        verifyNoMoreInteractions(mockedHarvesterType);
        verifyNoMoreInteractions(mockedHoldingsTarget);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedType);
        verifyNoMoreInteractions(mockedNote);
        verifyNoMoreInteractions(mockedEnabled);
        verifyNoMoreInteractions(mockedUpdateButton);
        verifyNoMoreInteractions(mockedStatus);
        verifyNoMoreInteractions(mockedPopupFormatOverrideEntry);
        verifyNoMoreInteractions(mockedMap);
        verifyNoMoreInteractions(mockedConfig);
        verifyNoMoreInteractions(mockedContent);
        verifyNoMoreInteractions(mockedFlowStore);
        verifyNoMoreInteractions(mockedProxyErrorTexts);
    }

}
