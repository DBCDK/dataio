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
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Mock private Texts mockedTexts;
    @Mock private Exception mockedException;
    @Mock private ViewGinjector mockedViewGinjector;

    private View view;

    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;
    private static boolean initializeModelHasBeenCalled;

    private final SinkModel sinkModel = new SinkModelBuilder().build();

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(String header) {
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
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.OPENUPDATE.name());

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
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.OPENUPDATE.name());

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
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.OPENUPDATE.name());

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
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.OPENUPDATE.name());

        // Subject Under Test
        presenterImpl.endpointChanged(ENDPOINT);

        // Verifications
        assertThat(presenterImpl.model.getOpenUpdateEndpoint(), is(ENDPOINT));
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithNameFieldEmpty_ErrorTextIsDisplayed() {

        // Setup
        initializeAndStartPresenter();
        presenterImpl.model.setSinkName("");

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithDescriptionFieldEmpty_ErrorTextIsDisplayed() {

        // Setup
        initializeAndStartPresenter();
        presenterImpl.model.setDescription("");

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void queueProvidersAddButtonPressed_callQueueProvidersAddButtonPressedWith3ItemsList_popupActivated() {

        // Setup
        initializeAndStartPresenter();
        presenterImpl.sinkTypeChanged(SinkContent.SinkType.OPENUPDATE.name());
        final List<String> QUEUE_PROVIDERS = Arrays.asList("QProvider1", "QProvider2", "QProvider3");
        presenterImpl.model.setOpenUpdateAvailableQueueProviders(QUEUE_PROVIDERS);

        // Subject Under Test
        presenterImpl.queueProvidersAddButtonPressed();

        // Verifications
        verify(view.queueProviders).clear();
        verify(view.queueProviders).setEnabled(false);
        verifyNoMoreInteractions(view.queueProviders);
        verify(view.popupTextBox).show();
        verifyNoMoreInteractions(view.popupTextBox);
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