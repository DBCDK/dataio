package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
* PresenterImpl unit tests
*
* The test methods of this class uses the following naming convention:
*
*  unitOfWork_stateUnderTest_expectedBehavior
*/
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest {
    @Mock
    ClientFactory mockedClientFactory;
    @Mock
    FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock
    Texts mockedTexts;
    @Mock
    AcceptsOneWidget mockedContainerWidget;
    @Mock
    EventBus mockedEventBus;
    @Mock
    ProxyErrorTexts mockedProxyErrorTexts;

    private ViewWidget viewWidget;

    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;
    List<FlowComponentModel> selectedFlowComponentModelList;
    List<FlowComponentModel> availableFlowComponentModelList;
    FlowComponentModel flowComponentModel1;
    FlowComponentModel flowComponentModel2;
    FlowComponentModel flowComponentModel3;
    FlowComponentModel flowComponentModel4;

    private final static long DEFAULT_ID = 0;
    private final static long DEFAULT_VERSION = 0;
    private final static String DEFAULT_NAME = "FlowName";
    private final static String DEFAULT_DESCRIPTION = "FlowDescription";

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
            view = PresenterImplTest.this.viewWidget;
            flowStoreProxy = mockedFlowStoreProxy;
            model = new FlowModel(DEFAULT_ID, DEFAULT_VERSION, DEFAULT_NAME, DEFAULT_DESCRIPTION, selectedFlowComponentModelList);
            availableFlowComponentModels = availableFlowComponentModelList;
            saveModelHasBeenCalled = false;
        }

        @Override
        void initializeModel() {
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

        public Texts getFlowModifyConstants() {
            return texts;
        }

        public ProxyErrorTexts getProxyErrorTexts() {
            return proxyErrorTexts;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupFlowComponentsLists() {
        flowComponentModel1 = new FlowComponentModelBuilder().setId(111).setName("FlowComponentName1").build();
        flowComponentModel2 = new FlowComponentModelBuilder().setId(222).setName("FlowComponentName2").build();
        flowComponentModel3 = new FlowComponentModelBuilder().setId(333).setName("FlowComponentName3").build();
        flowComponentModel4 = new FlowComponentModelBuilder().setId(444).setName("FlowComponentName4").build();
        selectedFlowComponentModelList = new ArrayList<FlowComponentModel>();  // Selected Flow Components contains elements 1 and 3
        selectedFlowComponentModelList.add(flowComponentModel1);
        selectedFlowComponentModelList.add(flowComponentModel3);
        availableFlowComponentModelList = new ArrayList<FlowComponentModel>();  // Available Flow Components contains elements 1, 2, 3 and 4
        availableFlowComponentModelList.add(flowComponentModel1);
        availableFlowComponentModelList.add(flowComponentModel2);
        availableFlowComponentModelList.add(flowComponentModel3);
        availableFlowComponentModelList.add(flowComponentModel4);
    }

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getFlowModifyTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    @Before
    public void setupView() {
        viewWidget = new ViewWidget("Header Text");  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        assertThat(presenterImpl.getFlowStoreProxy(), is(mockedFlowStoreProxy));
        assertThat(presenterImpl.getFlowModifyConstants(), is(mockedTexts));
        assertThat(presenterImpl.getProxyErrorTexts(), is(mockedProxyErrorTexts));
    }

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(viewWidget.name).clearText();
        verify(viewWidget.name).setEnabled(false);
        verify(viewWidget.description).clearText();
        verify(viewWidget.description).setEnabled(false);
        verify(viewWidget.flowComponents).clear();
        verify(viewWidget.flowComponents).setEnabled(false);
        verify(viewWidget.status).setText("");
        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        verify(mockedFlowStoreProxy).findAllFlowComponents(any(PresenterImpl.FindAllFlowComponentsAsyncCallback.class));
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "UpdatedName";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        presenterImpl.nameChanged(CHANGED_NAME);

        assertThat(presenterImpl.model.getFlowName(), is(CHANGED_NAME));
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {
        final String CHANGED_DESCRIPTION = "UpdatedDescription";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);

        assertThat(presenterImpl.model.getDescription(), is(CHANGED_DESCRIPTION));
    }

    @Test(expected = IllegalArgumentException.class)
    public void flowComponentsChanged_callFlowComponentsChangedWithUnknownFlowComponent_flowComponentsAreChangedAccordingly() {
        final String NEW_AND_UNKNOWN_FLOW_COMPONENT = "UpdatedFlowComponent";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        HashMap<String, String> changedFlowComponentsMap = new HashMap<String, String>();
        changedFlowComponentsMap.put("123", NEW_AND_UNKNOWN_FLOW_COMPONENT);
        presenterImpl.flowComponentsChanged(changedFlowComponentsMap);
    }

    @Test
    public void flowComponentsChanged_callFlowComponentsChangedWithKnownFlowComponent_flowComponentsAreChangedAccordingly() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        HashMap<String, String> changedFlowComponentsMap = new HashMap<String, String>();
        changedFlowComponentsMap.put(String.valueOf(flowComponentModel4.getId()), flowComponentModel4.getName());
        presenterImpl.flowComponentsChanged(changedFlowComponentsMap);

        assertThat(presenterImpl.model.getFlowComponents().size(), is(1));
        assertThat(presenterImpl.model.getFlowComponents().get(0).getName(), is(flowComponentModel4.getName()));
    }

    @Test
    public void keyPressed_callKeyPressed_statusFieldIsCleared() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        presenterImpl.keyPressed();

        verify(viewWidget.status).setText("");
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithNameFieldEmpty_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.model.setFlowName("");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithDescriptionFieldEmpty_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.model.setDescription("");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithFlowComponentListEmpty_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.model.setFlowComponents(new ArrayList<FlowComponentModel>());

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithInvalidCharactersInNameField_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.model.setFlowName("*(Flow name)*_%€");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_NameFormatValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithValidData_SaveModelIsCalled() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        presenterImpl.saveButtonPressed();

        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test
    public void findAllFlowComponentsAsyncCallback_successfulCallback_FlowComponentsAddedAndModelUpdated() {
        final long EXTRA_ID = 127;
        final String EXTRA_NAME = "Extra Name";
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        List<FlowComponentModel> flowComponentModels = new ArrayList<FlowComponentModel>(availableFlowComponentModelList);
        flowComponentModels.add(new FlowComponentModelBuilder().setId(EXTRA_ID).setName(EXTRA_NAME).build());

        presenterImpl.findAllFlowComponentsCallback.onSuccess(flowComponentModels);

        verify(viewWidget.name).setText(DEFAULT_NAME);
        verify(viewWidget.name).setEnabled(true);
        verify(viewWidget.description).setText(DEFAULT_DESCRIPTION);
        verify(viewWidget.description).setEnabled(true);
        verify(viewWidget.flowComponents).clear();
        verify(viewWidget.name).setFocus(true);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(viewWidget.flowComponents, times(2)).addValue(textCaptor.capture(), keyCaptor.capture());
        List<String> texts = textCaptor.getAllValues();
        List<String> keys = keyCaptor.getAllValues();

        verify(viewWidget.flowComponents).setEnabled(true);

        assertThat(texts.size(), is(2));
        assertThat(keys.size(), is(2));
        assertThat(texts.get(0), is(flowComponentModel1.getName()));
        assertThat(texts.get(1), is(flowComponentModel3.getName()));
        assertThat(keys.get(0), is(String.valueOf(flowComponentModel1.getId())));
        assertThat(keys.get(1), is(String.valueOf(flowComponentModel3.getId())));
    }

    @Test
    public void findAllFlowComponentsAsyncCallback_unsuccessfulCallbackNotFoundError_errorMessageDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.ENTITY_NOT_FOUND);

        presenterImpl.findAllFlowComponentsCallback.onFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_notFoundError();
    }

    @Test
    public void saveFlowModelAsyncCallback_successfulCallback_statusMessageDisplayed() {
        final String STATUS_MESSAGE = "Success Message";
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        FlowModel model = new FlowModel();
        when(mockedTexts.status_FlowSuccessfullySaved()).thenReturn(STATUS_MESSAGE);

        presenterImpl.saveFlowCallback.onSuccess(model);

        verify(viewWidget.status).setText(STATUS_MESSAGE);
    }

    @Test
    public void saveFlowModelAsyncCallback_unsuccessfulCallbackConflictError_errorMessageDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.CONFLICT_ERROR);

        presenterImpl.saveFlowCallback.onFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_conflictError();
    }
}