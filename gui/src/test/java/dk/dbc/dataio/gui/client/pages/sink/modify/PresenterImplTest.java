package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.EsSinkConfig;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    @Mock
    private Texts mockedTexts;
    @Mock
    private ViewGinjector mockedViewGinjector;

    private View view;

    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;
    private static boolean initializeModelHasBeenCalled;
    private static boolean handleSinkConfigHasBeenCalled;

    private final SinkModel sinkModel = new SinkModelBuilder().build();

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(String header) {
            super(header);
            view = PresenterImplTest.this.view;
            model = sinkModel;
            saveModelHasBeenCalled = false;
            initializeModelHasBeenCalled = false;
            handleSinkConfigHasBeenCalled = false;
        }

        @Override
        void handleSinkConfig(SinkContent.SinkType sinkType) {
            handleSinkConfigHasBeenCalled = true;
        }

        @Override
        void initializeModel() {
            initializeModelHasBeenCalled = true;
        }

        @Override
        void saveModel() {
            saveModelHasBeenCalled = true;
        }

        public SaveSinkModelFilteredAsyncCallback saveSinkModelFilteredAsyncCallback = new SaveSinkModelFilteredAsyncCallback();

        // Test method for reading flowStoreProxy
        public FlowStoreProxyAsync getFlowStoreProxy() {
            return mockedFlowStore;
        }

        // Test method for reading constants
        public Texts getSinkModifyConstants() {
            return mockedTexts;
        }

        @Override
        public Texts getTexts() {
            return mockedTexts;
        }

        @Override
        public void deleteButtonPressed() {
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        view = new View();
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedViewGinjector.getView()).thenReturn(view);
        mock(ContentPanel.class);
    }


    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        setupPresenterImpl();

        // Verifications
        assertThat(presenterImpl.getFlowStoreProxy(), is(mockedFlowStore));
        assertThat(presenterImpl.getSinkModifyConstants(), is(mockedTexts));
    }

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {

        // Setup
        setupPresenterImpl();

        // Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verififications
        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        assertThat(initializeModelHasBeenCalled, is(true));
    }

    @Test
    public void sinkTypeChanged_callSinkTypeChanged_sinkConfigSpecificSectionsSetInvisible() {
        // Setup
        initializeAndStartPresenter();
        view.esSinkSection.setVisible(true);

        // Subject Under Test
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.ES);

        // Verifications
        assertThat(view.updateSinkSection.isVisible(), is(false));
        assertThat(view.esSinkSection.isVisible(), is(false));
        assertThat(view.imsSinkSection.isVisible(), is(false));
        assertThat(handleSinkConfigHasBeenCalled, is(true));
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {

        // Setup
        final String CHANGED_NAME = "UpdatedName";
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.nameChanged(CHANGED_NAME);

        // Verifications
        assertThat(presenterImpl.model.getSinkName(), is(CHANGED_NAME));
    }

    @Test
    public void queueChanged_callResourceChanged_queueIsChangedAccordingly() {

        // Setup
        final String CHANGED_QUEUE = "sink::UpdatedQueue";
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.queueChanged(CHANGED_QUEUE);

        // Verifications
        assertThat(presenterImpl.model.getQueue(), is(CHANGED_QUEUE));
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {

        // Setup
        final String CHANGED_DESCRIPTION = "UpdatedDescription";
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);

        // Verifications
        assertThat(presenterImpl.model.getDescription(), is(CHANGED_DESCRIPTION));
    }

    @Test
    public void openUpdateUserIdChanged_callUserIdChanged_userIdIsChangedAccordingly() {

        // Setup
        final String USER_ID = "UserId";
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.OPENUPDATE);
        presenterImpl.model.setSinkConfig(new OpenUpdateSinkConfig());

        // Subject Under Test
        presenterImpl.openUpdateUserIdChanged(USER_ID);

        // Verifications
        assertThat(presenterImpl.model.getOpenUpdateUserId(), is(USER_ID));
    }

    @Test
    public void passwordChanged_callPasswordChanged_passwordIsChangedAccordingly() {

        // Setup
        final String PASSWORD = "Password";
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.OPENUPDATE);
        presenterImpl.model.setSinkConfig(new OpenUpdateSinkConfig());

        // Subject Under Test
        presenterImpl.passwordChanged(PASSWORD);

        // Verifications
        assertThat(presenterImpl.model.getOpenUpdatePassword(), is(PASSWORD));
    }

    @Test
    public void queueProvidersChanged_callQueueProvidersChanged_queueProvidersAreChangedAccordingly() {

        // Setup
        final List<String> QUEUE_PROVIDERS = Arrays.asList("QProvider1", "QProvider2", "QProvider3");
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.OPENUPDATE);
        presenterImpl.model.setSinkConfig(new OpenUpdateSinkConfig());

        // Subject Under Test
        presenterImpl.queueProvidersChanged(QUEUE_PROVIDERS);

        // Verifications
        assertThat(presenterImpl.model.getOpenUpdateAvailableQueueProviders(), is(QUEUE_PROVIDERS));
    }

    @Test
    public void endpointChanged_callEndpointChanged_endpointIsChangedAccordingly() {

        // Setup
        final String ENDPOINT = "Endpoint";
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.OPENUPDATE);
        presenterImpl.model.setSinkConfig(new OpenUpdateSinkConfig());

        // Subject Under Test
        presenterImpl.endpointChanged(ENDPOINT);

        // Verifications
        assertThat(presenterImpl.model.getOpenUpdateEndpoint(), is(ENDPOINT));
    }

    @Test
    public void esUserIdChanged_callEsUserIdChanged_esUserIdIsChangedAccordingly() {
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.ES);
        presenterImpl.model.setSinkConfig(new EsSinkConfig());

        // Subject Under Test
        presenterImpl.esUserIdChanged("3");

        // Verifications
        assertThat(presenterImpl.model.getEsUserId(), is(3));
    }

    @Test
    public void esDatabaseChanged_callEsDatabaseChanged_esDatabaseIsChangedAccordingly() {
        final String database = "changed database";
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.ES);
        presenterImpl.model.setSinkConfig(new EsSinkConfig());

        // Subject Under Test
        presenterImpl.esDatabaseChanged(database);

        // Verifications
        assertThat(presenterImpl.model.getEsDatabase(), is(database));
    }

    @Test
    public void imsEndpointChanged_callImsEndpointChanged_ImsEndpointIsChangedAccordingly() {
        final String imsEndpoint = "changed imsEndpoint";
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.IMS);
        presenterImpl.model.setSinkConfig(new ImsSinkConfig());

        // Subject Under Test
        presenterImpl.imsEndpointChanged(imsEndpoint);

        // Verifications
        assertThat(presenterImpl.model.getImsEndpoint(), is(imsEndpoint));
    }

    @Test
    public void worldCatUserIdChanged_callWordCatUserIdChanged_worldCatUserIdIsChangedAccordingly() {
        final String userId = "changed user id";
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.WORLDCAT);
        presenterImpl.model.setSinkConfig(new WorldCatSinkConfig());

        // Subject Under Test
        presenterImpl.worldCatUserIdChanged(userId);

        // Verifications
        assertThat(presenterImpl.model.getWorldCatUserId(), is(userId));
    }

    @Test
    public void worldCatPasswordChanged_callWordCatPasswordChanged_worldCatPasswordIsChangedAccordingly() {
        final String password = "new password";
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.WORLDCAT);
        presenterImpl.model.setSinkConfig(new WorldCatSinkConfig());

        // Subject Under Test
        presenterImpl.worldCatPasswordChanged(password);

        // Verifications
        assertThat(presenterImpl.model.getWorldCatPassword(), is(password));
    }

    @Test
    public void worldCatProjectIdChanged_callWordCatProjectIdChanged_worldCatProjectIdIsChangedAccordingly() {
        final String projectId = "new projectId";
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.WORLDCAT);
        presenterImpl.model.setSinkConfig(new WorldCatSinkConfig());

        // Subject Under Test
        presenterImpl.worldCatProjectIdChanged(projectId);

        // Verifications
        assertThat(presenterImpl.model.getWorldCatProjectId(), is(projectId));
    }

    @Test
    public void worldCatEndpointChanged_callWordCatEndpointChanged_worldCatEndpointIsChangedAccordingly() {
        final String endpoint = "url";
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.WORLDCAT);
        presenterImpl.model.setSinkConfig(new WorldCatSinkConfig());

        // Subject Under Test
        presenterImpl.worldCatEndpointChanged(endpoint);

        // Verifications
        assertThat(presenterImpl.model.getWorldCatEndpoint(), is(endpoint));
    }

    @Test
    public void worldCatRetryDiagnosticsChanged_callWorldCatRetryDiagnosticsChanged_worldCatRetryDiagnosticsAreChangedAccordingly() {

        // Setup
        final List<String> retryDiagnostics = Arrays.asList("diagnostic1", "diagnostic2");
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.WORLDCAT);
        presenterImpl.model.setSinkConfig(new WorldCatSinkConfig());

        // Subject Under Test
        presenterImpl.worldCatRetryDiagnosticsChanged(retryDiagnostics);

        // Verifications
        assertThat(presenterImpl.model.getWorldCatRetryDiagnostics(), is(retryDiagnostics));
    }

    @Test
    public void saveButtonPressed_nameFieldEmpty_ErrorTextIsDisplayed() {

        // Setup
        initializeAndStartPresenter();
        presenterImpl.model.setSinkName("");

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(saveModelHasBeenCalled, is(false));
    }

    @Test
    public void saveButtonPressed_descriptionFieldEmpty_ErrorTextIsDisplayed() {

        // Setup
        initializeAndStartPresenter();
        presenterImpl.model.setDescription("");

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_InputFieldValidationError();
        assertThat(saveModelHasBeenCalled, is(false));
    }

    @Test
    public void saveButtonPressed_allFieldsOk_saveButtonHasBeenCalled() {

        // Setup
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verifyNoMoreInteractions(mockedTexts);  // No error texts
        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test
    public void queueProvidersAddButtonPressed_callQueueProvidersAddButtonPressed_popupActivated() {

        // Setup
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.queueProvidersAddButtonPressed();

        // Verifications
        verify(view.queueProviders).clear();
        verify(view.queueProviders).setEnabled(false);
        verifyNoMoreInteractions(view.queueProviders);
        verify(view.queueProvidersPopupTextBox).show();
        verifyNoMoreInteractions(view.queueProvidersPopupTextBox);
    }

    @Test
    public void worldCatRetryDiagnosticsAddButtonPressed_callWorldCatRetryDiagnosticsAddButtonPressed_popupActivated() {

        // Setup
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.worldCatRetryDiagnosticsAddButtonPressed();

        // Verifications
        verify(view.worldCatRetryDiagnostics).clear();
        verify(view.worldCatRetryDiagnostics).setEnabled(false);
        verifyNoMoreInteractions(view.worldCatRetryDiagnostics);
        verify(view.worldCatPopupTextBox).show();
        verifyNoMoreInteractions(view.worldCatPopupTextBox);
    }

    @Test
    public void worldCatRetryDiagnosticRemoveButtonPressed_diagnosticRemoved_ok() {
        // Setup
        initializeAndStartPresenter();
        final Map<String, String> retryDiagnostics = new HashMap<>(1);
        retryDiagnostics.put("201", "201");

        when(view.worldCatRetryDiagnostics.getValue()).thenReturn(retryDiagnostics);

        // Subject Under Test
        presenterImpl.worldCatRetryDiagnosticRemoveButtonPressed("201");

        // Verifications
        assertThat(retryDiagnostics.size(), is(0));

        verify(view.worldCatRetryDiagnostics, times(1)).setEnabled(true);
        verify(view.worldCatRetryDiagnostics).setEnabled(false);
        verify(view.worldCatRetryDiagnostics).clear();
        verify(view.worldCatRetryDiagnostics).getValue();
        verify(view.worldCatRetryDiagnostics).setValue(new HashMap<>());
        verifyNoMoreInteractions(view.worldCatRetryDiagnostics);
    }

    @Test
    public void sequenceAnalysisOptionIdOnlyButtonPressed_callSequenceAnalysisOptionIdOnlyButtonPressed_sequenceAnalysisOptionIsChangedAccordingly() {

        // Setup
        initializeAndStartPresenter();
        presenterImpl.model.setSequenceAnalysisOption(SinkContent.SequenceAnalysisOption.ID_ONLY);

        // Subject Under Test
        presenterImpl.sequenceAnalysisSelectionChanged("ID_ONLY");

        // Verifications
        assertThat(presenterImpl.model.getSequenceAnalysisOption(), is(SinkContent.SequenceAnalysisOption.ID_ONLY));
    }

    @Test
    public void sequenceAnalysisOptionAllButtonPressed_callSequenceAnalysisOptionAllButtonPressed_sequenceAnalysisOptionIsChangedAccordingly() {

        // Setup
        initializeAndStartPresenter();
        presenterImpl.model.setSequenceAnalysisOption(SinkContent.SequenceAnalysisOption.ALL);

        // Subject Under Test
        presenterImpl.sequenceAnalysisSelectionChanged("ALL");

        // Verifications
        assertThat(presenterImpl.model.getSequenceAnalysisOption(), is(SinkContent.SequenceAnalysisOption.ALL));
    }

    @Test
    public void keyPressed_callKeyPressed_statusFieldIsCleared() {

        // Setup
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.keyPressed();

        // Verifications
        verify(view.status).setText("");
    }

    @Test
    public void saveSinkModelFilteredAsyncCallback_successfulCallback_setStatusTextCalledInView() {

        // Setup
        final String SUCCESS_TEXT = "SuccessText";
        initializeAndStartPresenter();
        when(mockedTexts.status_SinkSuccessfullySaved()).thenReturn(SUCCESS_TEXT);

        // Subject Under Test
        presenterImpl.saveSinkModelFilteredAsyncCallback.onSuccess(sinkModel);  // Emulate a successful callback from flowstore

        // Verifications
        verify(view.status).setText(SUCCESS_TEXT);  // Expect the status text to be set in View
        assertThat(presenterImpl.model, is(sinkModel));
    }

    @Test
    public void sinkModelFilteredAsyncCallback_unsuccessfulCallback_setErrorTextCalledInView() {

        // Setup
        initializeAndStartPresenter();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.CONFLICT_ERROR);

        // Subject Under Test
        presenterImpl.saveSinkModelFilteredAsyncCallback.onFailure(mockedProxyException); // Emulate an unsuccessful callback from flowstore

        // Verifications
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_conflictError();
    }


    /*
     * Private methods
     */
    private void initializeAndStartPresenter() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImplConcrete(header);
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
    }
}
