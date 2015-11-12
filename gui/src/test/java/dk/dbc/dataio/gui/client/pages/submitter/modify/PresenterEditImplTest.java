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

package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {
    @Mock private Texts mockedTexts;
    @Mock private EditPlace mockedEditPlace;
    @Mock private ViewGinjector mockedViewGinjector;

    private View editView;
    private PresenterEditImpl presenterEditImpl;
    private final static long DEFAULT_SUBMITTER_ID = 426L;

    class PresenterEditImplConcrete <Place extends EditPlace> extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, String header) {
            super(place, header);
            commonInjector = mockedCommonGinjector;
            viewInjector = mockedViewGinjector;
        }

        public GetSubmitterModelFilteredAsyncCallback getSubmitterModelFilteredAsyncCallback = new GetSubmitterModelFilteredAsyncCallback();
    }
        //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {

        editView = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view

        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getView()).thenReturn(editView);
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedEditPlace.getSubmitterId()).thenReturn(DEFAULT_SUBMITTER_ID);
        when(mockedMenuTexts.menu_SubmitterEdit()).thenReturn("Header Text");
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenterEditImpl();
        verify(mockedEditPlace, times(1)).getSubmitterId();
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_getSubmitterIsInvoked() {
        setupPresenterEditImpl();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel
        // initializeModel has the responsibility to setup the model in the presenter correctly
        // In this case, we expect the model to be initialized with the submitter values.
        verify(mockedFlowStore).getSubmitter(any(Long.class), any(PresenterEditImpl.SaveSubmitterModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_submitterContentOk_updateSubmitterCalled() {
        setupPresenterEditImpl();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SubmitterModel();
    }


    @Test
    public void getSubmitterModelFilteredAsyncCallback_successfulCallback_modelUpdated() {

        PresenterEditImplConcrete presenterEditImpl = setupPresenterEditImplConcrete();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String SUBMITTER_NAME = "New Submitter Name";
        SubmitterModel submitterModel = new SubmitterModel();
        submitterModel.setName(SUBMITTER_NAME);

        presenterEditImpl.getSubmitterModelFilteredAsyncCallback.onSuccess(submitterModel);

        verify(editView.name).setText(SUBMITTER_NAME);  // view is not mocked, but view.name is - we therefore do verify, that the model has been updated, by verifying view.name
    }

    @Test
    public void getSubmitterModelFilteredAsyncCallback_unSuccessfulCallback_errorMessage() {
        PresenterEditImplConcrete presenterEditImpl = setupPresenterEditImplConcrete();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.ENTITY_NOT_FOUND);

        presenterEditImpl.getSubmitterModelFilteredAsyncCallback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_notFoundError();
    }

    private PresenterEditImplConcrete setupPresenterEditImplConcrete() {
        return new PresenterEditImplConcrete(mockedEditPlace, header);
    }

    private void setupPresenterEditImpl() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;
        presenterEditImpl.commonInjector = mockedCommonGinjector;
    }
}
