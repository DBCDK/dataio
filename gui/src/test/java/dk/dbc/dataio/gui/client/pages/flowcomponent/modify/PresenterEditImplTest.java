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

package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterEditImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest {
    @Mock private ClientFactory mockedClientFactory;
    @Mock private FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock private JavaScriptProjectFetcherAsync mockedJavaScriptProjectFetcher;
    @Mock private Texts mockedTexts;
    @Mock private ProxyErrorTexts mockedProxyErrorTexts;
    @Mock private AcceptsOneWidget mockedContainerWidget;
    @Mock private EventBus mockedEventBus;
    @Mock private EditPlace mockedPlace;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;

    private EditView editView;

    private PresenterEditImpl presenterEditImpl;
    private final static long DEFAULT_FLOW_COMPONENT_ID = 426L;


    class PresenterEditImplConcrete extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, ClientFactory clientFactory) {
            super(place, clientFactory);
        }

        public GetFlowComponentModelFilteredAsyncCallback callback = new GetFlowComponentModelFilteredAsyncCallback();
    }

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getJavaScriptProjectFetcherAsync()).thenReturn(mockedJavaScriptProjectFetcher);
        when(mockedClientFactory.getFlowComponentEditView()).thenReturn(editView);
        when(mockedClientFactory.getFlowComponentModifyTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedPlace.getFlowComponentId()).thenReturn(DEFAULT_FLOW_COMPONENT_ID);
    }

    @Before
    public void setupView() {
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowComponentEdit()).thenReturn("Header Text");
        editView = new EditView(mockedClientFactory);  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterEditImpl = new PresenterEditImpl(mockedPlace, mockedClientFactory);
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly

        verify(mockedClientFactory).getFlowComponentEditView();
        verify(mockedPlace).getFlowComponentId();
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {
        presenterEditImpl = new PresenterEditImpl(mockedPlace, mockedClientFactory);
        assertThat(presenterEditImpl.model, is(notNullValue()));
        assertThat(presenterEditImpl.model.getName(), is(""));
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        assertThat(presenterEditImpl.model, is(notNullValue()));
        assertThat(presenterEditImpl.model.getName(), is(""));
        assertThat(presenterEditImpl.model.getSvnProject(), is(""));
        assertThat(presenterEditImpl.model.getSvnRevision(), is(""));
        assertThat(presenterEditImpl.model.getInvocationJavascript(), is(""));
        assertThat(presenterEditImpl.model.getInvocationMethod(), is(""));
        assertThat(presenterEditImpl.model.getJavascriptModules(), is(notNullValue()));
        assertThat(presenterEditImpl.model.getJavascriptModules().isEmpty(), is(true));

        verify(mockedFlowStoreProxy).getFlowComponent(eq(DEFAULT_FLOW_COMPONENT_ID), any(PresenterEditImpl.GetFlowComponentModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_callSaveModel_updateFlowComponentInFlowStoreCalled() throws Throwable {
        presenterEditImpl = new PresenterEditImpl(mockedPlace, mockedClientFactory);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String FLOW_COMPONENT_MODEL_NAME = "Flow Component Model Name";
        FlowComponentModel flowComponentModel = new FlowComponentModel();
        flowComponentModel.setName(FLOW_COMPONENT_MODEL_NAME);
        presenterEditImpl.nameChanged(FLOW_COMPONENT_MODEL_NAME);

        presenterEditImpl.saveModel();

        ArgumentCaptor<FlowComponentModel> flowComponentModelArgumentCaptor = ArgumentCaptor.forClass(FlowComponentModel.class);
        verify(mockedFlowStoreProxy).updateFlowComponent(flowComponentModelArgumentCaptor.capture(), any(PresenterImpl.SaveFlowComponentModelFilteredAsyncCallback.class));
        assertThat(flowComponentModelArgumentCaptor.getValue().getName(), is(FLOW_COMPONENT_MODEL_NAME));
    }

    @Test
    public void getFlowComponentModelFilteredAsyncCallback_unSuccessfulCallback_errorMessage() {
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedPlace, mockedClientFactory);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.ENTITY_NOT_FOUND);

        presenterEditImpl.callback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_notFoundError();
    }

    @Test
    public void getFlowComponentModelFilteredAsyncCallback_successfulCallback_modelUpdated() {
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedPlace, mockedClientFactory);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String FLOW_COMPONENT_MODEL_NAME = "New Flow Component Model Name";
        FlowComponentModel flowComponentModel = new FlowComponentModel();
        flowComponentModel.setName(FLOW_COMPONENT_MODEL_NAME);

        presenterEditImpl.callback.onSuccess(flowComponentModel);

        verify(editView.name).setText(FLOW_COMPONENT_MODEL_NAME);  // view is not mocked, but view.name is - we therefore do verify, that the model has been updated, by verifying view.name
    }

}
