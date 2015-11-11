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


package dk.dbc.dataio.gui.client.pages.sink.show;


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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

    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock ProxyException mockedProxyException;
    @Mock SingleSelectionModel<SinkModel> mockedSelectionModel;
    @Mock ListDataProvider<SinkModel> mockedDataProvider;
    @Mock private ViewGinjector mockedViewGinjector;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedViewGinjector.getView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        mockedView.selectionModel = mockedSelectionModel;
        mockedView.dataProvider = mockedDataProvider;
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete() {
            super(mockedPlaceController, header);
        }
        public FetchSinksCallback fetchSinksCallback = new FetchSinksCallback();
    }

    // Test Data
    private SinkModel testModel1 = new SinkModelBuilder().setName("SinkNam1").build();
    private SinkModel testModel2 = new SinkModelBuilder().setName("SinkNam2").build();
    private List<SinkModel> testModels = Arrays.asList(testModel1, testModel2);


    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {

        // Setup
        setupPresenterImpl();

        // Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllSinks(any(AsyncCallback.class));
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImpl(mockedPlaceController, header);
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.placeController = mockedPlaceController;
    }

    @Test
    public void editSink_call_gotoEditPlace() {

        // Setup
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject Under Test
        presenterImpl.editSink(testModel1);

        // Verify Test
        verify(mockedPlaceController).goTo(any(EditPlace.class));

    }

    @Test
    public void createSink_call_gotoCreatePlace() {

        // Setup
        setupPresenterImpl();

        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject Under Test
        presenterImpl.createSink();

        // Verify Test
        verify(mockedPlaceController).goTo(any(CreatePlace.class));
    }

    @Test
    public void fetchSinks_callbackWithError_errorMessageInView() {

        // Setup
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.commonInjector = mockedCommonGinjector;

        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        // Subject Under Test
        presenterImpl.fetchSinksCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedCommonGinjector).getFlowStoreProxyAsync();
        verify(mockedCommonGinjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void fetchSinks_callbackWithSuccess_sinksAreFetchedInitialCallback() {

        // Setup
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject Under Test
        presenterImpl.fetchSinksCallback.onSuccess(testModels);

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedView).setSinks(testModels);
    }
    @Test
    public void fetchSinks_callbackWithSuccess_sinksAreFetchedNoChanges() {

        // Setup
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(testModels);

        // Subject Under Test
        presenterImpl.fetchSinksCallback.onSuccess(testModels);

        // Verify Test
        verifyZeroInteractions(mockedSelectionModel);
        verify(mockedView, times(0)).setSinks(testModels);
    }

    @Test
    public void fetchSinks_callbackWithSuccess_sinksAreFetchedOneHasChangedSelectionIsSet() {

        // Setup
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(testModels);
        when(mockedSelectionModel.getSelectedObject()).thenReturn(testModel1);

        SinkModel editedSink = new SinkModelBuilder().setName("editedName").build();
        List<SinkModel> sinkModels = Arrays.asList(editedSink, testModel2);

        // Subject Under Test
        presenterImpl.fetchSinksCallback.onSuccess(sinkModels);

        // Verify Test
        verify(mockedSelectionModel).setSelected(editedSink, true);
        verify(mockedView).setSinks(sinkModels);
    }

}
