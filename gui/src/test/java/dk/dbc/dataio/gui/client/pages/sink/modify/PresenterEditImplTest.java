package dk.dbc.dataio.gui.client.pages.sink.modify;

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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {
    @Mock
    private EditPlace mockedEditPlace;
    @Mock
    private ViewGinjector mockedViewGinjector;
    @Mock
    private SinkModel mockedSinkModel;

    private View editView;

    private PresenterEditImpl presenterEditImpl;
    private final static long DEFAULT_SINK_ID = 433L;

    class PresenterEditImplConcrete<Place extends EditPlace> extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, String header) {
            super(place, header);
        }

        public GetSinkModelFilteredAsyncCallback getSinkModelFilteredAsyncCallback = new GetSinkModelFilteredAsyncCallback();
    }
    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupView() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedEditPlace.getSinkId()).thenReturn(DEFAULT_SINK_ID);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_SinkEdit()).thenReturn("Header Text");
        editView = new View(); // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedViewGinjector.getView()).thenReturn(editView);
    }


    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        setupPresenterEditImpl();

        // Verifications
        verify(mockedEditPlace).getSinkId();
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_getSinkIsInvoked() {

        // Expectations
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        // initializeModel has the responsibility to setup the model in the presenter correctly
        // In this case, we expect the model to be initialized with the submitter values.
        verify(mockedFlowStore).getSink(any(Long.class), any(PresenterEditImpl.GetSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_sinkContentOk_updateSinkCalled() {

        // Expectations
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SinkModel();

        presenterEditImpl.nameChanged("a");                   // Name is ok
        presenterEditImpl.queueChanged("sink::queue");

        presenterEditImpl.saveModel();

        // Verifications
        verify(mockedFlowStore).updateSink(eq(presenterEditImpl.model), any(PresenterImpl.SaveSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void getSinkModelFilteredAsyncCallback_successfulCallback_modelUpdated() {

        // Expectations
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;

        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final SinkModel sinkModel = new SinkModelBuilder().setSinkType(SinkContent.SinkType.DUMMY).build();

        // Initial state
        assertThat(presenterEditImpl.model, is(notNullValue()));
        assertThat(presenterEditImpl.model, not(sinkModel));

        // Subject Under Test
        presenterEditImpl.getSinkModelFilteredAsyncCallback.onSuccess(sinkModel);  // Emulate a successful callback from flowstore

        // Assert that the sink model has been updated correctly
        assertThat(presenterEditImpl.model, is(sinkModel));

        // Assert that the view is not showing sinkConfig specific sections
        assertThat(editView.esSinkSection.isVisible(), is(false));
        assertThat(editView.updateSinkSection.isVisible(), is(false));
        assertThat(editView.esSinkSection.isVisible(), is(false));
        assertThat(editView.worldCatSinkSection.isVisible(), is(false));
        assertThat(editView.sequenceAnalysisSection.isVisible(), is(false));

        // Assert that the view is displaying the correct values
        verify(editView.name).setText(sinkModel.getSinkName());  // view is not mocked, but view.name is - we therefore do verify, that the model has been updated, by verifying view.name
        verify(editView.description).setText(sinkModel.getDescription());
        verify(editView.sequenceAnalysisSelection).setValue(sinkModel.getSequenceAnalysisOption().name());
    }


    @Test
    public void getSinkModelFilteredAsyncCallback_successfulCallback_modelUpdatedCorrectlyForOpenUpdate() {

        // Expectations
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;

        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final OpenUpdateSinkConfig sinkConfig = new OpenUpdateSinkConfig()
                .withUserId("user")
                .withEndpoint("url")
                .withPassword("pass")
                .withAvailableQueueProviders(Collections.singletonList("avail1"));

        final SinkModel sinkModel = new SinkModelBuilder()
                .setSinkType(SinkContent.SinkType.OPENUPDATE)
                .setSinkConfig(sinkConfig).build();

        // Subject Under Test
        presenterEditImpl.getSinkModelFilteredAsyncCallback.onSuccess(sinkModel);  // Emulate a successful callback from flowstore

        // Assert that the sink model had the sink model updated correctly
        assertThat(presenterEditImpl.model, is(sinkModel));

        // Assert that the view is displaying the correct model values
        verify(editView.openupdateuserid).setText(sinkConfig.getUserId());
        verify(editView.url).setText(sinkConfig.getEndpoint());
        verify(editView.openupdatepassword).setText(sinkConfig.getPassword());
        verify(editView.updateSinkSection).setVisible(true);
        verify(editView.sinkTypeSelection).setEnabled(false);
        verify(editView.sequenceAnalysisSection).setVisible(true);

    }

    @Test
    public void getSinkModelFilteredAsyncCallback_successfulCallback_modelUpdatedCorrectlyForEs() {

        // Expectations
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final EsSinkConfig sinkConfig = new EsSinkConfig().withUserId(1).withDatabaseName("database");

        final SinkModel sinkModel = new SinkModelBuilder()
                .setSinkType(SinkContent.SinkType.ES)
                .setSinkConfig(sinkConfig).build();

        presenterEditImpl.getSinkModelFilteredAsyncCallback.onSuccess(sinkModel);  // Emulate a successful callback from flowstore

        // Assert that the sink model has been updated correctly
        assertThat(presenterEditImpl.model.getSinkConfig(), is(sinkConfig));

        // Assert that the view is displaying the correct model values
        verify(editView.esUserId).setText(String.valueOf(sinkConfig.getUserId()));
        verify(editView.esDatabase).setText(sinkConfig.getDatabaseName());
        verify(editView.esSinkSection).setVisible(true);
        verify(editView.sinkTypeSelection).setEnabled(false);
        verify(editView.sequenceAnalysisSection).setVisible(true);
    }

    @Test
    public void getSinkModelFilteredAsyncCallback_successfulCallback_modelUpdatedCorrectlyForWorldCat() {

        // Expectations
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;

        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final WorldCatSinkConfig sinkConfig = new WorldCatSinkConfig()
                .withUserId("user")
                .withPassword("pass")
                .withProjectId("42")
                .withRetryDiagnostics(Collections.singletonList("diagnostic"));

        final SinkModel sinkModel = new SinkModelBuilder()
                .setSinkType(SinkContent.SinkType.WORLDCAT)
                .setSinkConfig(sinkConfig).build();

        // Subject Under Test
        presenterEditImpl.getSinkModelFilteredAsyncCallback.onSuccess(sinkModel);  // Emulate a successful callback from flowstore

        // Assert that the sink model had the sink model updated correctly
        assertThat(presenterEditImpl.model, is(sinkModel));

        // Assert that the view is displaying the correct model values
        verify(editView.worldCatUserId).setText(sinkConfig.getUserId());
        verify(editView.worldCatPassword).setText(sinkConfig.getPassword());
        verify(editView.worldCatProjectId).setText(sinkConfig.getProjectId());
        verify(editView.worldCatSinkSection).setVisible(true);
        verify(editView.sinkTypeSelection).setEnabled(false);
        verify(editView.sequenceAnalysisSection).setVisible(true);
    }

    @Test
    public void getSinkModelFilteredAsyncCallback_successfulCallback_modelUpdatedCorrectlyForTickle() {

        // Expectations
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;

        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final WorldCatSinkConfig sinkConfig = new WorldCatSinkConfig()
                .withUserId("user")
                .withPassword("pass")
                .withProjectId("42")
                .withRetryDiagnostics(Collections.singletonList("diagnostic"));

        final SinkModel sinkModel = new SinkModelBuilder()
                .setSinkType(SinkContent.SinkType.TICKLE)
                .setSinkConfig(sinkConfig).build();

        // Subject Under Test
        presenterEditImpl.getSinkModelFilteredAsyncCallback.onSuccess(sinkModel);  // Emulate a successful callback from flowstore

        // Assert that the sink model had the sink model updated correctly
        assertThat(presenterEditImpl.model, is(sinkModel));

        // Assert that the view is displaying the correct model values
        verify(editView.sinkTypeSelection).setEnabled(false);
        verify(editView.sequenceAnalysisSection).setVisible(false);
    }

    @Test
    public void getSinkModelFilteredAsyncCallback_unsuccessfulCallback_errorMessage() {

        // Expectations
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;
        presenterEditImpl.commonInjector = mockedCommonGinjector;

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.ENTITY_NOT_FOUND);

        // Emulate an unsuccessful callback from flowstore
        presenterEditImpl.getSinkModelFilteredAsyncCallback.onFailure(mockedProxyException);
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_notFoundError();
    }

    @Test
    public void deleteSinkModelFilteredAsyncCallback_callback_invoked() {

        // Expectations
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;
        presenterEditImpl.commonInjector = mockedCommonGinjector;

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);

        presenterEditImpl.deleteModel();

        // Verify that the proxy call is invoked... Cannot emulate the callback as the return type is Void
        verify(mockedFlowStore).deleteSink(
                eq(presenterEditImpl.model.getId()),
                eq(presenterEditImpl.model.getVersion()),
                any(PresenterEditImpl.DeleteSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void handleSinkConfig_sinkConfigIsNull_noConfigAdded() {
        // Setup expectations
        setupPresenterEditImpl();
        final SinkModel sinkModel = new SinkModelBuilder().setSinkConfig(null).build();
        presenterEditImpl.model = sinkModel;

        // Subject Under Test
        presenterEditImpl.handleSinkConfig(SinkContent.SinkType.DUMMY);

        // Verifications
        verifyNoMoreViewInteractions();
    }

    @Test
    public void handleSinkConfig_sinkTypeEs_EsSinkConfigAdded() {
        // Setup expectations
        setupPresenterEditImpl();
        final SinkModel sinkModel = new SinkModelBuilder().setSinkConfig(new EsSinkConfig().withUserId(42).withDatabaseName("database")).build();

        presenterEditImpl.model = sinkModel;

        // Subject Under Test
        presenterEditImpl.handleSinkConfig(SinkContent.SinkType.ES);

        // Verifications
        verify(editView.esUserId).setText(String.valueOf(sinkModel.getEsUserId()));
        verify(editView.esDatabase).setText(sinkModel.getEsDatabase());
        verify(editView.esSinkSection).setVisible(true);
        verifyNoMoreViewInteractions();
    }

    @Test
    public void handleSinkConfig_sinkTypeOpenUpdate_OpenUpdateSinkConfigAdded() {
        // Setup expectations
        setupPresenterEditImpl();
        final SinkModel sinkModel = new SinkModelBuilder().setSinkConfig(new OpenUpdateSinkConfig()
                .withUserId("42").withPassword("openupdatepassword").withEndpoint("url")).build();

        presenterEditImpl.model = sinkModel;

        // Subject Under Test
        presenterEditImpl.handleSinkConfig(SinkContent.SinkType.OPENUPDATE);

        // Verifications
        verify(editView.url).setText(sinkModel.getOpenUpdateEndpoint());
        verify(editView.openupdateuserid).setText(sinkModel.getOpenUpdateUserId());
        verify(editView.openupdatepassword).setText(sinkModel.getOpenUpdatePassword());
        verify(editView.queueProviders).clear();
        verify(editView.updateSinkSection).setVisible(true);
        verifyNoMoreViewInteractions();
    }

    @Test
    public void handleSinkConfig_sinkTypeIms_ImsSinkConfigAdded() {
        // Setup expectations
        setupPresenterEditImpl();
        final SinkModel sinkModel = new SinkModelBuilder().setSinkConfig(new ImsSinkConfig().withEndpoint("ImsEndPoint")).build();
        presenterEditImpl.model = sinkModel;

        // Subject Under Test
        presenterEditImpl.handleSinkConfig(SinkContent.SinkType.IMS);

        // Verifications
        verify(editView.imsEndpoint).setText(sinkModel.getImsEndpoint());
        verify(editView.imsSinkSection).setVisible(true);
        verifyNoMoreViewInteractions();
    }

    @Test
    public void handleSinkConfig_sinkTypeWorldCat_WorldCatSinkConfigAdded() {
        // Setup expectations
        setupPresenterEditImpl();
        final SinkModel sinkModel = new SinkModelBuilder().setSinkConfig(new WorldCatSinkConfig().withUserId("42")
                .withPassword("passsword").withProjectId("projectId").withEndpoint("url")).build();

        presenterEditImpl.model = sinkModel;

        // Subject Under Test
        presenterEditImpl.handleSinkConfig(SinkContent.SinkType.WORLDCAT);

        // Verifications
        verify(editView.worldCatUserId).setText(sinkModel.getWorldCatUserId());
        verify(editView.worldCatPassword).setText(sinkModel.getWorldCatPassword());
        verify(editView.worldCatProjectId).setText(sinkModel.getWorldCatProjectId());
        verify(editView.worldCatEndpoint).setText(sinkModel.getWorldCatEndpoint());
        verify(editView.worldCatRetryDiagnostics).clear();
        verify(editView.worldCatSinkSection).setVisible(true);
        verifyNoMoreViewInteractions();
    }


    // Private methods

    private void setupPresenterEditImpl() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;
        presenterEditImpl.commonInjector = mockedCommonGinjector;
    }

    private void verifyNoMoreViewInteractions() {
        verifyNoMoreInteractions(editView.name);
        verifyNoMoreInteractions(editView.queue);
        verifyNoMoreInteractions(editView.description);
        verifyNoMoreInteractions(editView.updateSinkSection);
        verifyNoMoreInteractions(editView.esSinkSection);
        verifyNoMoreInteractions(editView.imsSinkSection);
        verifyNoMoreInteractions(editView.url);
        verifyNoMoreInteractions(editView.openupdateuserid);
        verifyNoMoreInteractions(editView.openupdatepassword);
        verifyNoMoreInteractions(editView.queueProviders);
        verifyNoMoreInteractions(editView.esUserId);
        verifyNoMoreInteractions(editView.esDatabase);
        verifyNoMoreInteractions(editView.imsEndpoint);
        verifyNoMoreInteractions(editView.deleteButton);
        verifyNoMoreInteractions(editView.status);
        verifyNoMoreInteractions(editView.queueProvidersPopupTextBox);
        verifyNoMoreInteractions(editView.sequenceAnalysisSelection);
        verifyNoMoreInteractions(editView.confirmation);
        verifyNoMoreInteractions(editView.worldCatSinkSection);
        verifyNoMoreInteractions(editView.worldCatUserId);
        verifyNoMoreInteractions(editView.worldCatPassword);
        verifyNoMoreInteractions(editView.worldCatProjectId);
        verifyNoMoreInteractions(editView.worldCatEndpoint);
        verifyNoMoreInteractions(editView.worldCatRetryDiagnostics);
    }
}
