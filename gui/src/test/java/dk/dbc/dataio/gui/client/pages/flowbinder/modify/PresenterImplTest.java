package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.OpenUpdateSinkConfig;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowBinderModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    private ViewGinjector mockedViewInjector;

    private static boolean initializeModelHasBeenCalled;
    private static boolean saveModelHasBeenCalled;
    List<SubmitterModel> availableSubmitterModelList;

    private View view;
    private PresenterImplConcrete presenterImpl;

    private final FlowModel flowModel1 = new FlowModelBuilder().setId(101).build();
    private final FlowModel flowModel2 = new FlowModelBuilder().setId(102).build();
    private final FlowModel flowModel3 = new FlowModelBuilder().setId(103).build();
    private final SubmitterModel submitterModel1 = new SubmitterModelBuilder().setId(201L).setNumber("2201").setName("SName 1").build();
    private final SubmitterModel submitterModel2 = new SubmitterModelBuilder().setId(202L).setNumber("2202").setName("SName 2").build();
    private final SubmitterModel submitterModel3 = new SubmitterModelBuilder().setId(203L).setNumber("2203").setName("SName 3").build();
    private final SubmitterModel submitterModel4 = new SubmitterModelBuilder().setId(204L).setNumber("2204").setName("SName 4").build();
    private final SinkModel sinkModel1 = new SinkModelBuilder().setId(301L).setName("Snm1").build();
    private final SinkModel sinkModel2 = new SinkModelBuilder().setId(302L).setName("Snm2").build();
    private final SinkModel sinkModel3 = new SinkModelBuilder().setId(303L).setName("Snm3").build();
    private final OpenUpdateSinkConfig openUpdateSinkConfig = new OpenUpdateSinkConfig().withAvailableQueueProviders(Arrays.asList("avail1", "avail2")).withUserId("user").withPassword("pass").withEndpoint("url");
    private final SinkModel sinkModel4 = new SinkModelBuilder().setId(304L).setName("Snm4").setSinkType(SinkContent.SinkType.OPENUPDATE).setSinkConfig(openUpdateSinkConfig).build();

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(String header) {
            super(header);
            List<SubmitterModel> submitterModelList = new ArrayList<>();
            submitterModelList.add(submitterModel1);
            model = new FlowBinderModelBuilder().setFlowModel(flowModel1).setSubmitterModels(submitterModelList).setSinkModel(sinkModel1).build();
            setAvailableSubmitters(availableSubmitterModelList);
            setAvailableFlows(Arrays.asList(flowModel1, flowModel2, flowModel3));
            setAvailableSinks(Arrays.asList(sinkModel1, sinkModel2, sinkModel3, sinkModel4));
            setAvailableRecordSplitters();
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
        View getView() {
            return view;
        }

        // The following instances of the callback classes allows a unit test for each of them
        public FetchAvailableSubmittersCallback fetchAvailableSubmittersCallback = new FetchAvailableSubmittersCallback();
        public FetchAvailableFlowsCallback fetchAvailableFlowsCallback = new FetchAvailableFlowsCallback();
        public FetchAvailableSinksCallback fetchAvailableSinksCallback = new FetchAvailableSinksCallback();
        public SaveFlowBinderModelFilteredAsyncCallback saveFlowBinderCallback = new SaveFlowBinderModelFilteredAsyncCallback();

        // Test methods implemented for the test only

        @Override
        public void deleteButtonPressed() {
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        view = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view
        availableSubmitterModelList = new ArrayList<>(4);
        availableSubmitterModelList.add(submitterModel1);
        availableSubmitterModelList.add(submitterModel2);
        availableSubmitterModelList.add(submitterModel3);
        availableSubmitterModelList.add(submitterModel4);
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedViewInjector.getView()).thenReturn(view);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenterImpl();
        assertThat(presenterImpl.getTexts(), is(mockedTexts));
    }


    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllSubmitters(any(FilteredAsyncCallback.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllFlows(any(FilteredAsyncCallback.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllSinks(any(FilteredAsyncCallback.class));
        assertThat(initializeModelHasBeenCalled, is(true));
    }

    @Test
    public void initialization__prioritiesSetCorrectly() {
        when(mockedTexts.selection_Low()).thenReturn("How Low Can You Go");
        when(mockedTexts.selection_Normal()).thenReturn("How Normal Can You Go");
        when(mockedTexts.selection_High()).thenReturn("How High Can You Go");

        initializeAndStartPresenter();

        verify(view.priority).clear();
        verify(view.priority).setEnabled(false);
        verify(view.priority).addAvailableItem("How Low Can You Go", "1");
        verify(view.priority).addAvailableItem("How Normal Can You Go", "4");
        verify(view.priority).addAvailableItem("How High Can You Go", "7");
        verifyNoMoreInteractions(view.priority);
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "UpdatedName";

        initializeAndStartPresenter();

        presenterImpl.nameChanged(CHANGED_NAME);

        assertThat(presenterImpl.model.getName(), is(CHANGED_NAME));
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {
        final String CHANGED_DESCRIPTION = "UpdatedDescription";

        initializeAndStartPresenter();

        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);

        assertThat(presenterImpl.model.getDescription(), is(CHANGED_DESCRIPTION));
    }

    @Test
    public void frameChanged_callFrameChanged_packagingIsChangedAccordingly() {
        final String CHANGED_PACKAGING = "UpdatedPackaging";

        initializeAndStartPresenter();

        presenterImpl.frameChanged(CHANGED_PACKAGING);

        assertThat(presenterImpl.model.getPackaging(), is(CHANGED_PACKAGING));
    }

    @Test
    public void formatChanged_callFormatChanged_formatIsChangedAccordingly() {
        final String CHANGED_FORMAT = "UpdatedFormat";

        initializeAndStartPresenter();

        presenterImpl.formatChanged(CHANGED_FORMAT);

        assertThat(presenterImpl.model.getFormat(), is(CHANGED_FORMAT));
    }

    @Test
    public void charsetChanged_callCharsetChanged_charsetIsChangedAccordingly() {
        final String CHANGED_CHARSET = "UpdatedCharset";

        initializeAndStartPresenter();

        presenterImpl.charsetChanged(CHANGED_CHARSET);

        assertThat(presenterImpl.model.getCharset(), is(CHANGED_CHARSET));
    }

    @Test
    public void destinationChanged_callDestinationChanged_destinationIsChangedAccordingly() {
        final String CHANGED_DESTINATION = "UpdatedDestination";

        initializeAndStartPresenter();

        presenterImpl.destinationChanged(CHANGED_DESTINATION);

        assertThat(presenterImpl.model.getDestination(), is(CHANGED_DESTINATION));
    }

    @Test
    public void recordSplitterChanged_callRecordSplitterChanged_recordSplitterIsChangedAccordingly() {
        final String CHANGED_RECORDSPLITTER = "UpdatedRecordSplitter";

        initializeAndStartPresenter();

        presenterImpl.recordSplitterChanged(CHANGED_RECORDSPLITTER);

        assertThat(presenterImpl.model.getRecordSplitter(), is(CHANGED_RECORDSPLITTER));
    }

    @Test
    public void priorityChanged_callPriorityChanged_priorityIsChangedAccordingly() {
        initializeAndStartPresenter();

        presenterImpl.priorityChanged("3");

        assertThat(presenterImpl.model.getPriority(), is(3));
    }

    @Test
    public void submittersChanged_callSubmittersChangedWithKnownSubmitter_selectedSubmittersIsChangedAccordingly() {
        initializeAndStartPresenter();

        Map<String, String> newSelectedSubmitters = new HashMap<>();
        newSelectedSubmitters.put("202", "85 (knallert)");
        presenterImpl.submittersChanged(newSelectedSubmitters);

        assertThat(presenterImpl.model.getSubmitterModels().size(), is(1));
        assertThat(presenterImpl.model.getSubmitterModels().get(0).getId(), is(202L));
        assertThat(presenterImpl.model.getSubmitterModels().get(0).getNumber(), is("2202"));
        assertThat(presenterImpl.model.getSubmitterModels().get(0).getName(), is("SName 2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void submittersChanged_callSubmittersChangedWithUnknownSubmitter_throws() {
        initializeAndStartPresenter();

        Map<String, String> newSelectedSubmitters = new HashMap<>();
        newSelectedSubmitters.put("210", "85 (knallert)");
        presenterImpl.submittersChanged(newSelectedSubmitters);
    }

    @Test(expected = NullPointerException.class)
    public void addSubmitter_nullValue_exception() {
        initializeAndStartPresenter();

        presenterImpl.addSubmitters(null);
    }

    @Test(expected = NumberFormatException.class)
    public void addSubmitter_emptyValue_exception() {
        initializeAndStartPresenter();

        final Map<String, String> submitters = new HashMap<>();
        submitters.put("", "");
        presenterImpl.addSubmitters(submitters);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addSubmitter_unknownId_exception() {
        initializeAndStartPresenter();

        final Map<String, String> submitters = new HashMap<>();
        submitters.put("123", "123");
        presenterImpl.addSubmitters(submitters);
    }

    @Test
    public void addSubmitter_knownId_submitterAdded() {
        initializeAndStartPresenter();

        final Map<String, String> submitters = new HashMap<>();
        submitters.put("202", "202");
        presenterImpl.addSubmitters(submitters);

        verify(view.submitters, times(2)).setEnabled(true);
        verify(view.submitters).setEnabled(false);
        verify(view.submitters).clear();
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("201", "2201 (SName 1)");
        expectedMap.put("202", "2202 (SName 2)");
        verify(view.submitters).setValue(expectedMap);
        verifyNoMoreInteractions(view.submitters);
    }

    @Test
    public void addSubmitter_duplicateId_submitterAdded() {
        initializeAndStartPresenter();

        final Map<String, String> submitters = new HashMap<>();
        submitters.put("201", "201");
        presenterImpl.addSubmitters(submitters);

        verify(view.submitters, times(2)).setEnabled(true);
        verify(view.submitters).setEnabled(false);
        verify(view.submitters).clear();
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("201", "2201 (SName 1)");
        verify(view.submitters).setValue(expectedMap);
        verifyNoMoreInteractions(view.submitters);
    }

    @Test(expected = NumberFormatException.class)
    public void removeSubmitter_nullId_exception() {
        initializeAndStartPresenter();

        presenterImpl.removeSubmitter(null);
    }

    @Test(expected = NumberFormatException.class)
    public void removeSubmitter_emptyId_exception() {
        initializeAndStartPresenter();

        presenterImpl.removeSubmitter("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeSubmitter_unknownId_exception() {
        initializeAndStartPresenter();

        presenterImpl.removeSubmitter("123");
    }

    @Test
    public void removeSubmitter_knownId_exception() {
        initializeAndStartPresenter();

        presenterImpl.removeSubmitter("201");

        verify(view.submitters, times(2)).setEnabled(true);
        verify(view.submitters).setEnabled(false);
        verify(view.submitters).clear();
        verify(view.submitters).setValue(new HashMap<>());
        verifyNoMoreInteractions(view.submitters);
    }

    @Test
    public void flowChanged_callFlowChangedWithNullSubmitter_flowNotChanged() {
        initializeAndStartPresenter();

        presenterImpl.flowChanged(null);

        assertThat(presenterImpl.model.getFlowModel().getId(), is(flowModel1.getId()));
        assertThat(presenterImpl.model.getFlowModel().getFlowName(), is(flowModel1.getFlowName()));
    }

    @Test
    public void flowChanged_callFlowChangedWithEmptySubmitter_flowNotChanged() {
        initializeAndStartPresenter();

        presenterImpl.flowChanged("");

        assertThat(presenterImpl.model.getFlowModel().getId(), is(flowModel1.getId()));
        assertThat(presenterImpl.model.getFlowModel().getFlowName(), is(flowModel1.getFlowName()));
    }

    @Test
    public void flowChanged_callFlowChangedWithKnownSubmitter_selectedSubmittersIsChangedAccordingly() {
        initializeAndStartPresenter();

        presenterImpl.flowChanged("103");

        assertThat(presenterImpl.model.getFlowModel().getId(), is(flowModel3.getId()));
        assertThat(presenterImpl.model.getFlowModel().getFlowName(), is(flowModel3.getFlowName()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void flowChanged_callFlowChangedWithUnknownSubmitter_throws() {
        initializeAndStartPresenter();

        presenterImpl.flowChanged("145");
    }

    @Test
    public void sinkChanged_callSinkChangedWithNullSink_sinkNotChanged() {
        initializeAndStartPresenter();

        presenterImpl.sinkChanged(null);

        assertThat(presenterImpl.model.getSinkModel().getId(), is(sinkModel1.getId()));
        assertThat(presenterImpl.model.getSinkModel().getSinkName(), is(sinkModel1.getSinkName()));
    }

    @Test
    public void sinkChanged_callSinkChangedWithEmptySink_sinkNotChanged() {
        initializeAndStartPresenter();

        presenterImpl.sinkChanged("");

        assertThat(presenterImpl.model.getSinkModel().getId(), is(sinkModel1.getId()));
        assertThat(presenterImpl.model.getSinkModel().getSinkName(), is(sinkModel1.getSinkName()));
    }

    @Test
    public void sinkChanged_callSinkChangedWithKnownSink_sinkIsChangedAccordingly() {
        initializeAndStartPresenter();

        presenterImpl.sinkChanged("303");

        assertThat(presenterImpl.model.getSinkModel().getId(), is(sinkModel3.getId()));
        assertThat(presenterImpl.model.getSinkModel().getSinkName(), is(sinkModel3.getSinkName()));
    }

    @Test
    public void sinkChanged_openUpdateSink_queryProviderIsSet() {
        initializeAndStartPresenter();

        presenterImpl.model.setQueueProvider(null);
        presenterImpl.sinkChanged("304");

        assertThat(presenterImpl.model.getQueueProvider(), is(openUpdateSinkConfig.getAvailableQueueProviders().get(0)));

        verify(view.updateSinkSection, times(1)).setVisible(true);
        verify(view.queueProvider, times(1)).setEnabled(true);
        verify(view.queueProvider, times(1)).setValue(presenterImpl.model.getQueueProvider());
    }

    @Test
    public void sinkChanged_toNoneOpenUpdateSink_queryProviderIsSet() {
        initializeAndStartPresenter();

        presenterImpl.sinkChanged("301");

        assertThat(presenterImpl.model.getQueueProvider(), is(nullValue()));

        verify(view.updateSinkSection, times(1)).setVisible(false);
        verify(view.queueProvider, times(1)).setEnabled(false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sinkChanged_callSinkChangedWithUnknownSink_throws() {
        initializeAndStartPresenter();

        presenterImpl.sinkChanged("645");
    }

    @Test
    public void queueProviderChanged_callQueueProviderChangedWithNullQueueProvider_queueProviderNotChanged() {
        initializeAndStartPresenter();

        presenterImpl.queueProviderChanged(null);

        assertThat(presenterImpl.model.getQueueProvider(), is("queue-provider"));
    }

    @Test
    public void queueProviderChanged_callQueueProviderChangedWithEmptyQueueProvider_queueProviderIsEmpty() {
        initializeAndStartPresenter();

        presenterImpl.queueProviderChanged("");

        assertThat(presenterImpl.model.getQueueProvider(), is(""));
    }

    @Test
    public void queueProviderChanged_callQueueProviderChangedWithNonEmptyQueueProvider_queueProviderIsChangedAccordingly() {
        initializeAndStartPresenter();

        presenterImpl.queueProviderChanged("hello queue provider");

        assertThat(presenterImpl.model.getQueueProvider(), is("hello queue provider"));
    }

    @Test
    public void keyPressed_callKeyPressed_statusTextEmptyed() {
        initializeAndStartPresenter();

        presenterImpl.keyPressed();
        verify(view.status, times(2)).setText("");
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedNameEmpty_errorMessageDisplayed() {
        initializeAndStartPresenter();
        presenterImpl.model.setName("");  // We do only test an empty name, since all other empty cases are tested in the model
        assertThat(saveModelHasBeenCalled, is(false));

        presenterImpl.saveButtonPressed();

        assertThat(saveModelHasBeenCalled, is(false));
        verify(mockedTexts).error_InputFieldValidationError();  // We cannot verify a call to view.setErrorText since it is not mocked, but it has a call to texts.error_InputFieldValidationError() - so we will verify this instead
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithInvalidCharactersInNameField_ErrorTextIsDisplayed() {
        setupPresenterImpl();
        presenterImpl.model.setName("*(Flow binder name)*_%€");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_NameFormatValidationError();
    }

    @Test
    public void fetchAvailableSubmittersCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        presenterImpl.fetchAvailableSubmittersCallback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
    }

    @Test
    public void fetchAvailableSubmittersCallback_successfullCallback_statusMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchAvailableSubmittersCallback.onSuccess(Collections.singletonList(submitterModel3));

        assertThat(presenterImpl.availableSubmitters.size(), is(1));
        assertThat(presenterImpl.availableSubmitters.get(0).getId(), is(submitterModel3.getId()));
    }

    @Test
    public void fetchAvailableFlowsCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchAvailableFlowsCallback.onFilteredFailure(mockedException);

        verify(mockedException).getMessage();  // Is called before calling view.setErrorText, which we cannot verify, since view is not mocked
    }

    @Test
    public void fetchAvailableFlowsCallback_successfullCallback_statusMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchAvailableFlowsCallback.onSuccess(Collections.singletonList(flowModel2));

        assertThat(presenterImpl.availableFlows.size(), is(1));
        assertThat(presenterImpl.availableFlows.get(0).getId(), is(flowModel2.getId()));
    }

    @Test
    public void fetchAvailableSinksCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.BAD_REQUEST);

        presenterImpl.fetchAvailableSinksCallback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_dataValidationError();
    }

    @Test
    public void fetchAvailableSinksCallback_successfullCallback_statusMessageDisplayed() {
        initializeAndStartPresenter();

        presenterImpl.fetchAvailableSinksCallback.onSuccess(Collections.singletonList(sinkModel2));

        assertThat(presenterImpl.availableSinks.size(), is(1));
        assertThat(presenterImpl.availableSinks.get(0).getId(), is(sinkModel2.getId()));
    }


    @Test
    public void saveFlowBinderModelCallback_unsuccessfulCallback_errorMessageDisplayed() {
        initializeAndStartPresenter();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.CONFLICT_ERROR);
        presenterImpl.saveFlowBinderCallback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_conflictError();
    }

    @Test
    public void saveFlowBinderModelCallback_successfullCallback_statusMessageDisplayed() {
        final String SUCCESS_TEXT = "Check!";
        FlowBinderModel flowBinderModel = new FlowBinderModelBuilder().setFlowModel(flowModel3).setSubmitterModels(Collections.singletonList(submitterModel3)).setSinkModel(sinkModel3).build();
        when(mockedTexts.status_SaveSuccess()).thenReturn(SUCCESS_TEXT);
        initializeAndStartPresenter();

        presenterImpl.saveFlowBinderCallback.onSuccess(flowBinderModel);

        verify(view.status).setText(SUCCESS_TEXT);
    }


    // Private methods
    private void initializeAndStartPresenter() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImplConcrete(header);
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.viewInjector = mockedViewInjector;
    }
}
