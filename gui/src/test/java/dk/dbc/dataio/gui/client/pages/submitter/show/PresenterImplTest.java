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


package dk.dbc.dataio.gui.client.pages.submitter.show;


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    @Mock private View mockedView;
    @Mock private Widget mockedViewWidget;
    @Mock private ProxyException mockedProxyException;
    @Mock private SingleSelectionModel<SubmitterModel> mockedSelectionModel;
    @Mock private ListDataProvider<SubmitterModel> mockedDataProvider;
    @Mock private ViewGinjector mockedViewGinjector;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getView()).thenReturn(mockedView);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        mockedView.selectionModel = mockedSelectionModel;
        mockedView.dataProvider = mockedDataProvider;
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete() {
            super(mockedPlaceController);
        }
        FetchSubmittersCallback fetchSubmittersCallback = new FetchSubmittersCallback();
    }

    // Test Data
    private SubmitterModel testModel1 = new SubmitterModelBuilder().setId(1L).setName("model1").build();
    private SubmitterModel testModel2 = new SubmitterModelBuilder().setId(2L).setName("model2").build();
    private List<SubmitterModel> testModels = new ArrayList<>(Arrays.asList(testModel1, testModel2));

    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {
        setupPresenterImpl();

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllSubmitters(any(AsyncCallback.class));
    }

    @Test
    public void showFlowBinders_call_gotoEditPlace() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.showFlowBinders(testModels.get(0));

        // Verify Test
        verifyZeroInteractions(mockedSelectionModel);
    }

    @Test
    public void editSubmitter_call_gotoEditPlace() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.editSubmitter(testModels.get(0));

        // Verify Test
        verifyZeroInteractions(mockedSelectionModel);
        verify(mockedPlaceController).goTo(any(EditPlace.class));
    }

    @Test
    public void createSubmitter_call_gotoCreatePlace() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.createSubmitter();

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedPlaceController).goTo(any(CreatePlace.class));
    }

    @Test
    public void fetchSubmitters_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = setupPresenterConcreteImplAndStart();

        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedCommonGinjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void fetchSubmitters_callbackWithSuccess_SubmittersAreFetchedInitialCallback() {
        PresenterImplConcrete presenterImpl = setupPresenterConcreteImplAndStart();

        when(mockedDataProvider.getList()).thenReturn(new ArrayList<>());

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onSuccess(testModels);

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedView).setSubmitters(testModels);
    }

    @Test
    public void fetchSubmitters_callbackWithSuccess_SubmittersAreFetchedNoChanges() {
        PresenterImplConcrete presenterImpl = setupPresenterConcreteImplAndStart();

        when(mockedDataProvider.getList()).thenReturn(testModels);

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onSuccess(testModels);

        // Verify Test
        verifyZeroInteractions(mockedSelectionModel);
        verify(mockedView, times(0)).setSubmitters(testModels);
    }

    @Test
    public void fetchSubmitters_callbackWithSuccess_SubmittersAreFetchedOneHasChangedSelectionIsSet() {
        PresenterImplConcrete presenterImpl = setupPresenterConcreteImplAndStart();

        when(mockedDataProvider.getList()).thenReturn(testModels);
        when(mockedSelectionModel.getSelectedObject()).thenReturn(testModel1);

        SubmitterModel editedSubmitter = new SubmitterModelBuilder().setName("editedName").build();
        List<SubmitterModel> submitterModels = Arrays.asList(editedSubmitter, testModel2);

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onSuccess(submitterModels);

        // Verify Test
        verify(mockedSelectionModel).setSelected(editedSubmitter, true);
        verify(mockedView).setSubmitters(submitterModels);
    }

    @Test
    public void fetchSubmitters_callbackWithSuccess_SubmitterAreFetchedOneWasDeletedSelectionIsCleared() {
        PresenterImplConcrete presenterImpl = setupPresenterConcreteImplAndStart();

        when(mockedDataProvider.getList()).thenReturn(testModels);

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onSuccess(Collections.singletonList(testModel1));

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedView).setSubmitters(Collections.singletonList(testModel1));
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImpl(mockedPlaceController);
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.viewInjector = mockedViewGinjector;
    }

    private PresenterImplConcrete setupPresenterConcreteImplAndStart() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        return presenterImpl;
    }

}
