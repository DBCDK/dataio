/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.PingResponseModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
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
public class PresenterImplTest extends PresenterImplTestBase {

    @Mock private SinkServiceProxyAsync mockedSinkServiceProxy;
    @Mock private Texts mockedTexts;
    @Mock private Exception mockedException;
    @Mock private ViewGinjector mockedViewGinjector;

    private View view;

    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;
    private static boolean initializeModelHasBeenCalled;

    private final SinkModel sinkModel = new SinkModelBuilder().build();

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory, String header) {
            super(header);
            view = PresenterImplTest.this.view;
            model = sinkModel;
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
        public PingSinkServiceFilteredAsyncCallback pingSinkServiceFilteredAsyncCallback = new PingSinkServiceFilteredAsyncCallback();

        // Test method for reading flowStoreProxy
        public FlowStoreProxyAsync getFlowStoreProxy() {
            return mockedFlowStore;
        }

        // Test method for reading sinkServiceProxy
        public SinkServiceProxyAsync getSinkServiceProxy() {
            return mockedSinkServiceProxy;
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
            deleteButtonPressed();
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        when(mockedViewGinjector.getSinkServiceProxyAsync()).thenReturn(mockedSinkServiceProxy);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedViewGinjector.getView()).thenReturn(view);
        mock(ContentPanel.class);

    }

    @Before
    public void setupView() {
        view = new View();
    }


    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        setupPresenterImpl();

        // Verifications
        assertThat(presenterImpl.getFlowStoreProxy(), is(mockedFlowStore));
        assertThat(presenterImpl.getSinkServiceProxy(), is(mockedSinkServiceProxy));
        assertThat(presenterImpl.getSinkModifyConstants(), is(mockedTexts));
    }

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {

        // Setup
        setupPresenterImpl();

        // Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verififications
        verify(mockedContainerWidget).setWidget(Matchers.any(IsWidget.class));
        assertThat(initializeModelHasBeenCalled, is(true));
    }

    @Test
    public void sinkTypeChanged_callSinkTypeChanged_sinkTypeIsChangedAccordingly() {

        // Setup
        final SinkContent.SinkType CHANGED_SINK_TYPE = SinkContent.SinkType.DUMMY;
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.sinkTypeChanged("DUMMY");

        // Verifications
        assertThat(presenterImpl.model.getSinkType(), is(CHANGED_SINK_TYPE));
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
    public void resourceChanged_callResourceChanged_recourceIsChangedAccordingly() {

        // Setup
        final String CHANGED_RESOURCE = "UpdatedResource";
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.resourceChanged(CHANGED_RESOURCE);

        // Verifications
        assertThat(presenterImpl.model.getResourceName(), is(CHANGED_RESOURCE));
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
    public void userIdChanged_callUserIdChanged_userIdIsChangedAccordingly() {

        // Setup
        final String USER_ID = "UserId";
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.userIdChanged(USER_ID);

        // Verifications
        assertThat(presenterImpl.model.getOpenUpdateUserId(), is(USER_ID));
    }

    @Test
    public void passwordChanged_callPasswordChanged_passwordIsChangedAccordingly() {

        // Setup
        final String PASSWORD = "Password";
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.passwordChanged(PASSWORD);

        // Verifications
        assertThat(presenterImpl.model.getOpenUpdatePassword(), is(PASSWORD));
    }

    @Test
    public void endpointChanged_callEndpointChanged_endpointIsChangedAccordingly() {

        // Setup
        final String ENDPOINT = "Endpoint";
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.endpointChanged(ENDPOINT);

        // Verifications
        assertThat(presenterImpl.model.getOpenUpdateEndpoint(), is(ENDPOINT));
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithNameFieldEmpty_ErrorTextIsDisplayed() {

        // Setup
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, header);
        presenterImpl.model.setSinkName("");

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithDescriptionFieldEmpty_ErrorTextIsDisplayed() {

        // Setup
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, header);
        presenterImpl.model.setResourceName("");

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_InputFieldValidationError();
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
    public void saveButtonPressed_callSaveButtonPressed_pingIsCalled() {

        // Setup
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedSinkServiceProxy).ping(any(SinkModel.class), any(PresenterImpl.PingSinkServiceFilteredAsyncCallback.class));
    }

    @Test
    public void pingSinkServiceFilteredAsyncCallback_successfulCallbackStatusOk_saveModelIsCalled() {

        // Setup
        initializeAndStartPresenter();
        saveModelHasBeenCalled = false;

        // Subject Under Test
        presenterImpl.pingSinkServiceFilteredAsyncCallback.onSuccess(new PingResponseModel(PingResponseModel.Status.OK));

        // Verifications
        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test
    public void pingSinkServiceFilteredAsyncCallback_successfulCallbackStatusFailed_saveModelNotCalled() {

        // Setup
        initializeAndStartPresenter();
        saveModelHasBeenCalled = false;

        // Subject Under Test
        presenterImpl.pingSinkServiceFilteredAsyncCallback.onSuccess(new PingResponseModel(PingResponseModel.Status.FAILED));

        // Verifications
        assertThat(saveModelHasBeenCalled, is(false));
    }

    @Test
    public void pingSinkServiceFilteredAsyncCallback_unsuccessfulCallback_setStatusTextCalledInView() {

        // Setup
        initializeAndStartPresenter();

        // Subject Under Test
        presenterImpl.saveSinkModelFilteredAsyncCallback.onFailure(mockedException); // Emulate an unsuccessful callback from flowstore

        // Verifications
        verify(mockedException).getMessage();  // Is called before calling view.setErrorText, which we cannot verify, since view is not mocked
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
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, header);
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
    }
}