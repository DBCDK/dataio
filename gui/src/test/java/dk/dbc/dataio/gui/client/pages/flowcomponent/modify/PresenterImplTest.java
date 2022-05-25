package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    @Mock
    private Texts mockedTexts;
    @Mock
    private JavaScriptProjectFetcherAsync mockedJavaScriptProjectFetcher;
    @Mock
    private ViewGinjector mockedViewGinjector;

    private static boolean initializeModelHasBeenCalled;
    private static boolean saveModelHasBeenCalled;

    private View view;
    private PresenterImplConcrete presenterImpl;

    private final static String NAME = "FlowComponentName";
    private final static String PROJECT = "datawell-convert";
    private final static String REVISION = "8779";
    private final static String JAVA_SCRIPT_NAME = "javaScriptName";
    private final static String INVOCATION_METHOD = "invocationMethod";
    private final RevisionInfo.ChangedItem changedItem = new RevisionInfo.ChangedItem("path", "type");
    private final RevisionInfo revisionInfo = new RevisionInfo(1L, "author", new Date(), "message", Arrays.asList(changedItem));
    private final FlowComponentModel flowComponentModel = new FlowComponentModelBuilder()
            .setName(NAME)
            .setSvnProject(PROJECT)
            .setSvnRevision(REVISION)
            .setInvocationJavascript(JAVA_SCRIPT_NAME)
            .setInvocationMethod(INVOCATION_METHOD)
            .build();

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(String header) {
            super(header);
            viewInjector = mockedViewGinjector;
            commonInjector = mockedCommonGinjector;
            model = flowComponentModel;
            initializeModelHasBeenCalled = false;
            saveModelHasBeenCalled = false;
        }

        @Override
        void initializeModel() {
            initializeModelHasBeenCalled = true;
        }

        @Override
        void saveModel() {
            saveModelHasBeenCalled = true;
        }

        @Override
        public void deleteButtonPressed() {

        }

        // The following instances of the callback classes allows a unit test for each of them
        public FetchRevisionsFilteredAsyncCallback fetchRevisionsFilteredAsyncCallback = new FetchRevisionsFilteredAsyncCallback();
        public FetchScriptsFilteredAsyncCallback fetchScriptsFilteredAsyncCallback = new FetchScriptsFilteredAsyncCallback();
        public FetchInvocationMethodsFilteredAsyncCallback fetchInvocationMethodsFilteredAsyncCallback = new FetchInvocationMethodsFilteredAsyncCallback();

        public SaveFlowComponentModelFilteredAsyncCallback saveFlowComponentModelFilteredAsyncCallback = new SaveFlowComponentModelFilteredAsyncCallback();
    }

    @Before
    public void setupMockedObjects() {
        view = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedCommonGinjector.getJavaScriptProjectFetcherAsync()).thenReturn(mockedJavaScriptProjectFetcher);
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedViewGinjector.getView()).thenReturn(view);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        setupPresenter();
    }


    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        assertThat(initializeModelHasBeenCalled, is(true));
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "userInputName";
        initializeAndStartPresenter();
        presenterImpl.nameChanged(CHANGED_NAME);
        assertThat(presenterImpl.model.getName(), is(CHANGED_NAME));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void projectChanged_callProjectChanged_projectIsChangedAccordingly() {
        final String CHANGED_PROJECT = "userInputProject";
        initializeAndStartPresenter();
        presenterImpl.projectChanged(CHANGED_PROJECT);
        assertThat(presenterImpl.model.getSvnProject(), is(CHANGED_PROJECT));
        verify(mockedJavaScriptProjectFetcher).fetchRevisions(
                eq(presenterImpl.model.getSvnProject()),
                any(FilteredAsyncCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setAvailableRevisions_callSetAvailableRevisions_projectDependentModelValuesAreChangedAccordingly() {
        initializeAndStartPresenter();
        presenterImpl.setAvailableRevisions(Arrays.asList(revisionInfo));
        assertThat(presenterImpl.model.getName(), is(NAME));
        assertThat(presenterImpl.model.getSvnProject(), is(PROJECT));
        assertThat(presenterImpl.model.getSvnRevision(), is(REVISION));
        assertThat(presenterImpl.model.getInvocationJavascript(), is(JAVA_SCRIPT_NAME));
        assertThat(presenterImpl.model.getInvocationMethod(), is(INVOCATION_METHOD));

        presenterImpl.setAvailableRevisions(Arrays.asList(revisionInfo));
        assertThat(presenterImpl.model.getName(), is(NAME));
        assertThat(presenterImpl.model.getSvnProject(), is(PROJECT));
        assertThat(presenterImpl.model.getSvnRevision(), is(""));
        assertThat(presenterImpl.model.getInvocationJavascript(), is(""));
        assertThat(presenterImpl.model.getInvocationMethod(), is(""));
    }

    @Test
    public void setAvailableRevisions_callSetAvailableRevisions_availableRevisionsAreChangedAccordingly() {
        initializeAndStartPresenter();
        assertThat(presenterImpl.availableRevisions.isEmpty(), is(true));
        presenterImpl.setAvailableRevisions(Arrays.asList(revisionInfo));
        assertThat(presenterImpl.availableRevisions.size(), is(1));
        assertThat(presenterImpl.availableRevisions.get(0), is(Long.valueOf(revisionInfo.getRevision()).toString()));
    }

    @Test
    public void setAvailableScripts_callSetAvailableScripts_availableScriptsAreChangedAccordingly() {
        initializeAndStartPresenter();
        assertThat(presenterImpl.availableScripts.isEmpty(), is(true));
        presenterImpl.setAvailableScripts(Collections.singletonList(JAVA_SCRIPT_NAME));
        assertThat(presenterImpl.availableScripts.size(), is(1));
        assertThat(presenterImpl.availableScripts.get(0), is(JAVA_SCRIPT_NAME));
    }

    @Test
    public void setAvailableInvocationMethods_callSetAvailableInvocationMethods_availableInvocationMethodsAreChangedAccordingly() {
        initializeAndStartPresenter();
        assertThat(presenterImpl.availableInvocationMethods.isEmpty(), is(true));
        presenterImpl.setAvailableInvocationMethods(Collections.singletonList(INVOCATION_METHOD));
        assertThat(presenterImpl.availableInvocationMethods.size(), is(1));
        assertThat(presenterImpl.availableInvocationMethods.get(0), is(INVOCATION_METHOD));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void revisionChanged_callRevisionChanged_revisionIsChangedAccordingly() {
        final String CHANGED_REVISION = "6544";
        initializeAndStartPresenter();
        presenterImpl.revisionChanged(CHANGED_REVISION);
        assertThat(presenterImpl.model.getSvnRevision(), is(CHANGED_REVISION));
        verify(mockedJavaScriptProjectFetcher).fetchJavaScriptFileNames(
                eq(presenterImpl.model.getSvnProject()),
                eq(Long.valueOf(presenterImpl.model.getSvnRevision())),
                any(FilteredAsyncCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void scriptNameChanged_callScriptNameChanged_scriptNameIsChangedAccordingly() throws Throwable {
        final String CHANGED_SCRIPT_NAME = "changedJavaScriptName";
        initializeAndStartPresenter();
        presenterImpl.scriptNameChanged(CHANGED_SCRIPT_NAME);
        assertThat(presenterImpl.model.getInvocationJavascript(), is(CHANGED_SCRIPT_NAME));
        verify(mockedJavaScriptProjectFetcher).fetchJavaScriptInvocationMethods(
                eq(presenterImpl.model.getSvnProject()),
                eq(Long.valueOf(presenterImpl.model.getSvnRevision())),
                eq(presenterImpl.model.getInvocationJavascript()),
                any(FilteredAsyncCallback.class));
    }

    @Test
    public void invocationMethodChanged_callInvocationMethodChanged_invocationMethodIsChangedAccordingly() {
        final String CHANGED_INVOCATION_METHOD = "begin";
        initializeAndStartPresenter();
        presenterImpl.invocationMethodChanged(CHANGED_INVOCATION_METHOD);
        assertThat(presenterImpl.model.getInvocationMethod(), is(CHANGED_INVOCATION_METHOD));
    }

    @Test
    public void keyPressed_callKeyPressed_statusTextEmptied() {
        initializeAndStartPresenter();
        presenterImpl.keyPressed();
        verify(view.status, times(2)).setText("");
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedNameEmpty_errorMessageDisplayed() throws Throwable {
        initializeAndStartPresenter();
        presenterImpl.model.setName("");  // We do only test an empty name, since all other empty cases are tested in the model
        assertThat(saveModelHasBeenCalled, is(false));

        presenterImpl.saveButtonPressed();

        assertThat(saveModelHasBeenCalled, is(false));
        verify(mockedTexts).error_InputFieldValidationError();  // We cannot verify a call to view.setErrorText since it is not mocked, but it has a call to texts.error_InputFieldValidationError() - so we will verify this instead
    }

    @Test
    public void fetchRevisionsFilteredAsyncCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchRevisionsFilteredAsyncCallback.onFilteredFailure(mockedException);

        verify(mockedException).getMessage();  // Is called before calling view.setErrorText, which we cannot verify, since view is not mocked
    }

    @Test
    public void fetchRevisionsFilteredAsyncCallback_successfulCallback_statusMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchRevisionsFilteredAsyncCallback.onSuccess(Arrays.asList(revisionInfo));

        assertThat(presenterImpl.availableRevisions.size(), is(1));
        assertThat(Long.valueOf(presenterImpl.availableRevisions.get(0)), is(revisionInfo.getRevision()));
    }

    @Test
    public void fetchScriptsFilteredAsyncCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchScriptsFilteredAsyncCallback.onFilteredFailure(mockedException);

        verify(mockedException).getMessage();  // Is called before calling view.setErrorText, which we cannot verify, since view is not mocked
    }

    @Test
    public void fetchScriptsFilteredAsyncCallback_successfulCallback_statusMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchScriptsFilteredAsyncCallback.onSuccess(Arrays.asList(JAVA_SCRIPT_NAME));

        assertThat(presenterImpl.availableScripts.size(), is(1));
        assertThat(presenterImpl.availableScripts.get(0), is(JAVA_SCRIPT_NAME));
    }

    @Test
    public void fetchInvocationMethodsFilteredAsyncCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchInvocationMethodsFilteredAsyncCallback.onFilteredFailure(mockedException);

        verify(mockedException).getMessage();  // Is called before calling view.setErrorText, which we cannot verify, since view is not mocked
    }

    @Test
    public void fetchInvocationMethodsFilteredAsyncCallback_successfulCallback_statusMessageDisplayed() {
        initializeAndStartPresenter();
        presenterImpl.fetchInvocationMethodsFilteredAsyncCallback.onSuccess(Arrays.asList(INVOCATION_METHOD));

        assertThat(presenterImpl.availableInvocationMethods.size(), is(1));
        assertThat(presenterImpl.availableInvocationMethods.get(0), is(INVOCATION_METHOD));
    }

    @Test
    public void saveFlowComponentModelFilteredAsyncCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.CONFLICT_ERROR);

        presenterImpl.saveFlowComponentModelFilteredAsyncCallback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_conflictError();
    }

    @Test
    public void saveFlowComponentModelFilteredAsyncCallback_successfulCallback_statusMessageDisplayed() {
        final String SUCCESS_TEXT = "Check!";
        when(mockedTexts.status_FlowComponentSuccessfullySaved()).thenReturn(SUCCESS_TEXT);
        initializeAndStartPresenter();

        presenterImpl.saveFlowComponentModelFilteredAsyncCallback.onSuccess(flowComponentModel);

        verify(view.status).setText(SUCCESS_TEXT);
        assertThat(presenterImpl.model, is(flowComponentModel));
    }

    // Private methods
    private void initializeAndStartPresenter() {
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }

    private void setupPresenter() {
        presenterImpl = new PresenterImplConcrete(header);
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.viewInjector = mockedViewGinjector;
    }
}
