package dk.dbc.dataio.gui.client.pages.submitter.show;


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.FlowBinderIdent;
import dk.dbc.dataio.gui.client.components.submitterfilter.SubmitterFilter;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private View mockedView;
    @Mock
    private Widget mockedViewWidget;
    @Mock
    private ProxyException mockedProxyException;
    @Mock
    private SingleSelectionModel<SubmitterModel> mockedSelectionModel;
    @Mock
    private ListDataProvider<SubmitterModel> mockedDataProvider;
    @Mock
    private ViewGinjector mockedViewGinjector;
    @Mock
    private Texts mockedTexts;
    @Mock
    private SubmitterFilter mockedSubmitterFilter;

    static String MOCKED_MENU_SUBMITTERS = "Mocked Submitter Text";
    static String MOCKED_NO_FLOW_BINDERS = "Mocked No Flow Binders";

    private final String header = "Mocked Submitter Text";

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getView()).thenReturn(mockedView);
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Submitters()).thenReturn(MOCKED_MENU_SUBMITTERS);
        when(mockedTexts.error_NoFlowBinders()).thenReturn(MOCKED_NO_FLOW_BINDERS);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        mockedView.selectionModel = mockedSelectionModel;
        mockedView.dataProvider = mockedDataProvider;
        mockedView.submitterFilter = mockedSubmitterFilter;
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete() {
            super(mockedPlaceController, mockedView, header);
        }

        FetchSubmittersCallback fetchSubmittersCallback = new FetchSubmittersCallback();
        GetFlowBindersForSubmitterCallback getFlowBindersForSubmitterCallback = new GetFlowBindersForSubmitterCallback();
    }

    // Test Data
    private SubmitterModel testModel1 = new SubmitterModelBuilder().setId(1L).setName("model1").build();
    private SubmitterModel testModel2 = new SubmitterModelBuilder().setId(2L).setName("model2").build();
    private List<SubmitterModel> testModels = new ArrayList<>(Arrays.asList(testModel1, testModel2));
    final FlowBinderIdent flowbinderIdent1 = new FlowBinderIdent("Flowbinder with submitter", 111L);
    final FlowBinderIdent flowbinderIdent2 = new FlowBinderIdent("Another flowbinder with submitter", 112L);
    List<FlowBinderIdent> testFlowBinderIdents = Arrays.asList(flowbinderIdent1, flowbinderIdent2);

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
    public void showFlowBinders_call_callViewMethodWithListOfFlowbinders() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.showFlowBinders(testModels.get(0));

        // Verify Test
        verifyNoInteractions(mockedSelectionModel);
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(MOCKED_MENU_SUBMITTERS);
        verify(mockedView).asWidget();
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void editSubmitter_call_gotoEditPlace() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.editSubmitter(testModels.get(0));

        // Verify Test
        verifyNoInteractions(mockedSelectionModel);
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
        verifyNoInteractions(mockedSelectionModel);
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


    @Test
    public void getFlowBindersForSubmitter_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = setupPresenterConcreteImplAndStart();

        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        // Test Subject Under Test
        presenterImpl.getFlowBindersForSubmitterCallback.onFailure(mockedProxyException);

        // Verify Test
        verify(mockedCommonGinjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void getFlowBindersForSubmitter_callbackWithSuccessNullPointer_NopInitialCallback() {
        PresenterImplConcrete presenterImpl = setupPresenterConcreteImplAndStart();

        // Test Subject Under Test
        presenterImpl.getFlowBindersForSubmitterCallback.onSuccess(null);

        // Verify Test
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(MOCKED_MENU_SUBMITTERS);
        verify(mockedView).asWidget();
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void getFlowBindersForSubmitter_callbackWithSuccessEmptyList_NopCallback() {
        PresenterImplConcrete presenterImpl = setupPresenterConcreteImplAndStart();

        // Test Subject Under Test
        presenterImpl.getFlowBindersForSubmitterCallback.onSuccess(new ArrayList<>());

        // Verify Test
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(MOCKED_MENU_SUBMITTERS);
        verify(mockedView).asWidget();
        verifyNoMoreInteractions(mockedView);
    }

    @Test
    public void getFlowBindersForSubmitter_callbackWithSuccess_SubmittersAreFetchedInitialCallback() {
        PresenterImplConcrete presenterImpl = setupPresenterConcreteImplAndStart();

        // Test Subject Under Test
        presenterImpl.getFlowBindersForSubmitterCallback.onSuccess(testFlowBinderIdents);

        // Verify Test
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(mockedView).showFlowBinders(captor.capture());
        assertThat(captor.getValue().size(), is(2));
        FlowBinderModel flowBinderModel = (FlowBinderModel) captor.getValue().get(0);
        assertThat(flowBinderModel.getId(), is(111L));
        assertThat(flowBinderModel.getName(), is("Flowbinder with submitter"));
        flowBinderModel = (FlowBinderModel) captor.getValue().get(1);
        assertThat(flowBinderModel.getId(), is(112L));
        assertThat(flowBinderModel.getName(), is("Another flowbinder with submitter"));
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).setHeader(MOCKED_MENU_SUBMITTERS);
        verify(mockedView).asWidget();
        verifyNoMoreInteractions(mockedView);
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImpl(mockedPlaceController, mockedView, header);
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
