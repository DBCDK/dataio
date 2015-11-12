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

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {
    @Mock private Texts mockedTexts;
    @Mock private EditPlace mockedEditPlace;
    @Mock private ViewGinjector mockedViewGinjector;

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
        verify(mockedFlowStore).getSink(any(Long.class), any(PresenterEditImpl.SaveSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_sinkContentOk_updateSinkCalled() {

        // Expectations
        setupPresenterEditImpl();

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SinkModel();

        presenterEditImpl.nameChanged("a");                   // Name is ok
        presenterEditImpl.resourceChanged("resource");        // Resource is ok

        presenterEditImpl.saveModel();

        // Verifications
        verify(mockedFlowStore).updateSink(eq(presenterEditImpl.model), any(PresenterImpl.SaveSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void getSinkModelFilteredAsyncCallback_successfulCallback_modelUpdated() {

        // Expectations
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String SINK_NAME = "New Sink Name";
        SinkModel sinkModel = new SinkModel();
        sinkModel.setSinkName(SINK_NAME);

        assertThat(presenterEditImpl.model, is(notNullValue()));
        assertThat(presenterEditImpl.model.getSinkName(), is(""));

        presenterEditImpl.getSinkModelFilteredAsyncCallback.onSuccess(sinkModel);  // Emulate a successful callback from flowstore

        // Assert that the sink model has been updated correctly
        assertThat(presenterEditImpl.model.getSinkName(), is(sinkModel.getSinkName()));

        // Assert that the view is displaying the correct values
        verify(editView.name).setText(SINK_NAME);  // view is not mocked, but view.name is - we therefore do verify, that the model has been updated, by verifying view.name
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

    private void setupPresenterEditImpl() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;
        presenterEditImpl.commonInjector = mockedCommonGinjector;
    }

}
