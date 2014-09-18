package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class PresenterImplTest {
    private ClientFactory mockedClientFactory;
    private FlowStoreProxyAsync mockedFlowStoreProxy;
    private dk.dbc.dataio.gui.client.pages.flow.modify.Texts mockedTexts;
    private AcceptsOneWidget mockedContainerWidget;
    private EventBus mockedEventBus;
    private View mockedView;

    private PresenterImplConcrete presenterImpl;
    private static boolean initializeModelHasBeenCalled;
    private static boolean saveModelHasBeenCalled;
    List<FlowComponentModel> selectedFlowComponentModelList;
    Map<String, String> selectedFlowComponentModelMap;
    List<FlowComponentModel> availableFlowComponentModelList;
    Map<String, String> availableFlowComponentModelMap;

    private final static long   DEFAULT_ID = 0;
    private final static long   DEFAULT_VERSION = 0;
    private final static String DEFAULT_NAME = "FlowName";
    private final static String DEFAULT_DESCRIPTION = "FlowDescription";
    private final static long   FLOW_COMPONENT_ID_1 = 111L;
    private final static String FLOW_COMPONENT_NAME_1 = "FlowComponentName1";
    private final static long   FLOW_COMPONENT_ID_2 = 222L;
    private final static String FLOW_COMPONENT_NAME_2 = "FlowComponentName2";
    private final static long   FLOW_COMPONENT_ID_3 = 333L;
    private final static String FLOW_COMPONENT_NAME_3 = "FlowComponentName3";
    private final static long   FLOW_COMPONENT_ID_4 = 444L;
    private final static String FLOW_COMPONENT_NAME_4 = "FlowComponentName4";

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory, dk.dbc.dataio.gui.client.pages.flow.modify.Texts texts) {
            super(clientFactory, texts);
            flowStoreProxy = mockedFlowStoreProxy;
            view = mockedView;
            model = new FlowModel(DEFAULT_ID, DEFAULT_VERSION, DEFAULT_NAME, DEFAULT_DESCRIPTION, selectedFlowComponentModelList);
            availableFlowComponentModels = availableFlowComponentModelList;
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

        public SaveFlowModelAsyncCallback saveFlowCallback = new SaveFlowModelAsyncCallback();

        public FindAllFlowComponentsAsyncCallback findAllFlowComponentsCallback = new FindAllFlowComponentsAsyncCallback();

        /*
         * Test methods implemented for the test only
         */
        public FlowStoreProxyAsync getFlowStoreProxy() {
            return flowStoreProxy;
        }

        public dk.dbc.dataio.gui.client.pages.flow.modify.Texts getFlowModifyConstants() {
            return texts;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupFlowComponentsLists() {
        selectedFlowComponentModelList = new ArrayList<FlowComponentModel>();  // Selected Flow Components contains elements 1 and 3
        selectedFlowComponentModelList.add(newFlowComponentModel(FLOW_COMPONENT_ID_1, FLOW_COMPONENT_NAME_1));
        selectedFlowComponentModelList.add(newFlowComponentModel(FLOW_COMPONENT_ID_3, FLOW_COMPONENT_NAME_3));
        selectedFlowComponentModelMap = new HashMap<String, String>();
        selectedFlowComponentModelMap.put(String.valueOf(FLOW_COMPONENT_ID_1), FLOW_COMPONENT_NAME_1);
        selectedFlowComponentModelMap.put(String.valueOf(FLOW_COMPONENT_ID_3), FLOW_COMPONENT_NAME_3);
        availableFlowComponentModelList = new ArrayList<FlowComponentModel>();  // Available Flow Components contains elements 1, 2, 3 and 4
        availableFlowComponentModelList.add(newFlowComponentModel(FLOW_COMPONENT_ID_1, FLOW_COMPONENT_NAME_1));
        availableFlowComponentModelList.add(newFlowComponentModel(FLOW_COMPONENT_ID_2, FLOW_COMPONENT_NAME_2));
        availableFlowComponentModelList.add(newFlowComponentModel(FLOW_COMPONENT_ID_3, FLOW_COMPONENT_NAME_3));
        availableFlowComponentModelList.add(newFlowComponentModel(FLOW_COMPONENT_ID_4, FLOW_COMPONENT_NAME_4));
        availableFlowComponentModelMap = new HashMap<String, String>();
        availableFlowComponentModelMap.put(String.valueOf(FLOW_COMPONENT_ID_1), FLOW_COMPONENT_NAME_1);
        availableFlowComponentModelMap.put(String.valueOf(FLOW_COMPONENT_ID_2), FLOW_COMPONENT_NAME_2);
        availableFlowComponentModelMap.put(String.valueOf(FLOW_COMPONENT_ID_3), FLOW_COMPONENT_NAME_3);
        availableFlowComponentModelMap.put(String.valueOf(FLOW_COMPONENT_ID_4), FLOW_COMPONENT_NAME_4);
    }

    @Before
    public void setupMockedObjects() {
        mockedClientFactory = mock(ClientFactory.class);
        mockedFlowStoreProxy = mock(FlowStoreProxyAsync.class);
        mockedTexts = mock(Texts.class);
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        mockedContainerWidget = mock(AcceptsOneWidget.class);
        mockedEventBus = mock(EventBus.class);
        mockedView = mock(View.class);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);
        assertThat(presenterImpl.getFlowStoreProxy(), is(mockedFlowStoreProxy));
        assertThat(presenterImpl.getFlowModifyConstants(), is(mockedTexts));
    }

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        verify(mockedFlowStoreProxy).findAllFlowComponents(any(PresenterImpl.FindAllFlowComponentsAsyncCallback.class));
        verify(mockedView).setName(DEFAULT_NAME);
        verify(mockedView).setDescription(DEFAULT_DESCRIPTION);
        verify(mockedView).setSelectedFlowComponents(selectedFlowComponentModelMap);

        assertThat(initializeModelHasBeenCalled, is(true));
        assertThat(presenterImpl.model.getId(), is(DEFAULT_ID));
        assertThat(presenterImpl.model.getFlowName(), is(DEFAULT_NAME));
        assertThat(presenterImpl.model.getDescription(), is(DEFAULT_DESCRIPTION));
        assertThat(presenterImpl.model.getFlowComponents().size(), is(2));
        assertThat(presenterImpl.model.getFlowComponents().get(0).getName(), is(FLOW_COMPONENT_NAME_1));
        assertThat(presenterImpl.model.getFlowComponents().get(1).getName(), is(FLOW_COMPONENT_NAME_3));
        assertThat(presenterImpl.availableFlowComponentModels.size(), is(4));
        assertThat(presenterImpl.availableFlowComponentModels.get(0).getName(), is(FLOW_COMPONENT_NAME_1));
        assertThat(presenterImpl.availableFlowComponentModels.get(1).getName(), is(FLOW_COMPONENT_NAME_2));
        assertThat(presenterImpl.availableFlowComponentModels.get(2).getName(), is(FLOW_COMPONENT_NAME_3));
        assertThat(presenterImpl.availableFlowComponentModels.get(3).getName(), is(FLOW_COMPONENT_NAME_4));
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "UpdatedName";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);

        presenterImpl.nameChanged(CHANGED_NAME);

        assertThat(presenterImpl.model.getFlowName(), is(CHANGED_NAME));
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {
        final String CHANGED_DESCRIPTION = "UpdatedDescription";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);

        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);

        assertThat(presenterImpl.model.getDescription(), is(CHANGED_DESCRIPTION));
    }

    @Test(expected = IllegalArgumentException.class)
    public void flowComponentsChanged_callFlowComponentsChangedWithUnknownFlowComponent_flowComponentsAreChangedAccordingly() {
        final String NEW_AND_UNKNOWN_FLOW_COMPONENT = "UpdatedFlowComponent";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);

        HashMap<String, String> changedFlowComponentsMap = new HashMap<String, String>();
        changedFlowComponentsMap.put("123", NEW_AND_UNKNOWN_FLOW_COMPONENT);
        presenterImpl.flowComponentsChanged(changedFlowComponentsMap);
    }

    @Test
    public void flowComponentsChanged_callFlowComponentsChangedWithKnownFlowComponent_flowComponentsAreChangedAccordingly() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);

        HashMap<String, String> changedFlowComponentsMap = new HashMap<String, String>();
        changedFlowComponentsMap.put(String.valueOf(FLOW_COMPONENT_ID_4), FLOW_COMPONENT_NAME_4);
        presenterImpl.flowComponentsChanged(changedFlowComponentsMap);

        assertThat(presenterImpl.model.getFlowComponents().size(), is(1));
        assertThat(presenterImpl.model.getFlowComponents().get(0).getName(), is(FLOW_COMPONENT_NAME_4));
    }

    @Test
    public void keyPressed_callKeyPressed_statusFieldIsCleared() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);

        presenterImpl.keyPressed();

        verify(mockedView).setStatusText("");
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithNameFieldEmpty_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);
        presenterImpl.model.setFlowName("");

        presenterImpl.saveButtonPressed();

        verify(mockedView).setErrorText(mockedTexts.error_InputFieldValidationError());
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithDescriptionFieldEmpty_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);
        presenterImpl.model.setDescription("");

        presenterImpl.saveButtonPressed();

        verify(mockedView).setErrorText(mockedTexts.error_InputFieldValidationError());
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithFlowComponentListEmpty_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);
        presenterImpl.model.setFlowComponents(new ArrayList<FlowComponentModel>());

        presenterImpl.saveButtonPressed();

        verify(mockedView).setErrorText(mockedTexts.error_InputFieldValidationError());
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithValidData_SaveModelIsCalled() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);

        presenterImpl.saveButtonPressed();

        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test
    public void findAllFlowComponentsAsyncCallback_successfulCallback_FlowComponentsAddedAndModelUpdated() {
        final long EXTRA_ID = 127;
        final String EXTRA_NAME = "Extra Name";
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);

        List<FlowComponentModel> flowComponentModels = new ArrayList<FlowComponentModel>(availableFlowComponentModelList);
        flowComponentModels.add(newFlowComponentModel(EXTRA_ID, EXTRA_NAME));

        presenterImpl.findAllFlowComponentsCallback.onSuccess(flowComponentModels);

        verify(mockedView).setName(DEFAULT_NAME);
        verify(mockedView).setDescription(DEFAULT_DESCRIPTION);

        ArgumentCaptor<Map> selectedFlowComponentsArgument = ArgumentCaptor.forClass(Map.class);
        verify(mockedView).setSelectedFlowComponents(selectedFlowComponentsArgument.capture());
        Map<String, String> selected = selectedFlowComponentsArgument.getValue();
        assertThat(selected.size(), is(2));
        assertThat(selected.get(String.valueOf(FLOW_COMPONENT_ID_1)), is(FLOW_COMPONENT_NAME_1));
        assertThat(selected.get(String.valueOf(FLOW_COMPONENT_ID_3)), is(FLOW_COMPONENT_NAME_3));

        ArgumentCaptor<Map> availableFlowComponentsArgument = ArgumentCaptor.forClass(Map.class);
        verify(mockedView).setAvailableFlowComponents(availableFlowComponentsArgument.capture());
        Map<String, String> available = availableFlowComponentsArgument.getValue();
        assertThat(available.size(), is(3));
        assertThat(available.get(String.valueOf(FLOW_COMPONENT_ID_2)), is(FLOW_COMPONENT_NAME_2));
        assertThat(available.get(String.valueOf(FLOW_COMPONENT_ID_4)), is(FLOW_COMPONENT_NAME_4));
        assertThat(available.get(String.valueOf(EXTRA_ID)), is(EXTRA_NAME));

    }

    @Test
    public void findAllFlowComponentsAsyncCallback_unsuccessfulCallback_errorMessageDisplayed() {
        final String FAILURE = "Failure";
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);

        IllegalArgumentException exception = new IllegalArgumentException(FAILURE);
        presenterImpl.findAllFlowComponentsCallback.onFailure(exception);

        verify(mockedView).setErrorText(exception.getClass().getName() + " - " + exception.getMessage() + " - " + Arrays.toString(exception.getStackTrace()));
    }

    @Test
    public void saveFlowModelAsyncCallback_successfullCallback_statusMessageDisplayed() {
        final String STATUS_MESSAGE = "Success Message";
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);
        FlowModel model = new FlowModel();
        when(mockedTexts.status_FlowSuccessfullySaved()).thenReturn(STATUS_MESSAGE);

        presenterImpl.saveFlowCallback.onSuccess(model);

        verify(mockedView).setStatusText(STATUS_MESSAGE);
    }

    @Test
    public void saveFlowModelAsyncCallback_unsuccessfullCallback_errorMessageDisplayed() {
        final String FAILURE = "Failure!!!";
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);

        IllegalArgumentException exception = new IllegalArgumentException(FAILURE);
        presenterImpl.saveFlowCallback.onFailure(exception);

        verify(mockedView).setErrorText(exception.getClass().getName() + " - " + exception.getMessage() + " - " + Arrays.toString(exception.getStackTrace()));
    }

    /*
     * Private methods
     */

    private FlowComponentModel newFlowComponentModel(long id, String name) {
        return new FlowComponentModel(id, 1L, name, "", "", "", "", new ArrayList<String>());
    }

}