package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    Texts mockedTexts;
    @Mock
    ViewGinjector mockedViwGinjector;
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
        public PresenterImplConcrete(PlaceController placeController, String header) {
            super(placeController, header);
            commonInjector = mockedCommonGinjector;
            viewInjector = mockedViwGinjector;
            this.placeController = mockedPlaceController;
            viewWidget.model = new FlowModelBuilder().setId(DEFAULT_ID).setVersion(DEFAULT_VERSION).setName(DEFAULT_NAME).setDescription(DEFAULT_DESCRIPTION).setComponents(selectedFlowComponentModelList).build();
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

        @Override
        public void deleteButtonPressed() {
        }

        public SaveFlowModelAsyncCallback saveFlowCallback = new SaveFlowModelAsyncCallback();

        public FindAllFlowComponentsAsyncCallback findAllFlowComponentsCallback = new FindAllFlowComponentsAsyncCallback();

        public ViewWidget getView() {
            return viewWidget;
        }

        public void setAvailableFlowComponentModels(List<FlowComponentModel> flowComponentModels) {
            availableFlowComponentModels = flowComponentModels;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupFlowComponentsLists() {
        flowComponentModel1 = new FlowComponentModelBuilder().setId(111).setName("FlowComponentName1").build();
        flowComponentModel2 = new FlowComponentModelBuilder().setId(222).setName("FlowComponentName2").build();
        flowComponentModel3 = new FlowComponentModelBuilder().setId(333).setName("FlowComponentName3").build();
        flowComponentModel4 = new FlowComponentModelBuilder().setId(444).setName("FlowComponentName4").build();
        selectedFlowComponentModelList = new ArrayList<>();  // Selected Flow Components contains elements 1 and 3
        selectedFlowComponentModelList.add(flowComponentModel1);
        selectedFlowComponentModelList.add(flowComponentModel3);
        availableFlowComponentModelList = new ArrayList<>();  // Available Flow Components contains elements 1, 2, 3 and 4
        availableFlowComponentModelList.add(flowComponentModel1);
        availableFlowComponentModelList.add(flowComponentModel2);
        availableFlowComponentModelList.add(flowComponentModel3);
        availableFlowComponentModelList.add(flowComponentModel4);
    }

    @Before
    public void setupMockedObjects() {
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViwGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    @Before
    public void setupView() {
        viewWidget = new ViewWidget();  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(viewWidget.name).clearText();
        verify(viewWidget.name).setEnabled(false);
        verify(viewWidget.description).clearText();
        verify(viewWidget.description).setEnabled(false);
        verify(viewWidget.flowComponents).clear();
        verify(viewWidget.flowComponents).setEnabled(false);
        verify(viewWidget.status).setText("");
        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllFlowComponents(any(PresenterImpl.FindAllFlowComponentsAsyncCallback.class));
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "UpdatedName";

        setupPresenterImpl();

        presenterImpl.nameChanged(CHANGED_NAME);

        assertThat(viewWidget.model.getFlowName(), is(CHANGED_NAME));
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {

        // Setup
        final String CHANGED_DESCRIPTION = "UpdatedDescription";
        setupPresenterImpl();

        // Subject Under Test
        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);

        // Verifications
        assertThat(viewWidget.model.getDescription(), is(CHANGED_DESCRIPTION));
    }

    @Test(expected = IllegalArgumentException.class)
    public void flowComponentsChanged_callFlowComponentsChangedWithUnknownFlowComponent_flowComponentsAreChangedAccordingly() {

        // Setup
        final String NEW_AND_UNKNOWN_FLOW_COMPONENT = "UpdatedFlowComponent";
        setupPresenterImpl();
        HashMap<String, String> changedFlowComponentsMap = new HashMap<>();
        changedFlowComponentsMap.put("123", NEW_AND_UNKNOWN_FLOW_COMPONENT);

        // Subject Under Test
        presenterImpl.flowComponentsChanged(changedFlowComponentsMap);
    }

    @Test
    public void flowComponentsChanged_callFlowComponentsChangedWithKnownFlowComponent_flowComponentsAreChangedAccordingly() {

        // Setup
        setupPresenterImpl();
        HashMap<String, String> changedFlowComponentsMap = new HashMap<>();
        changedFlowComponentsMap.put(String.valueOf(flowComponentModel4.getId()), flowComponentModel4.getName());

        // Subject Under Test
        presenterImpl.flowComponentsChanged(changedFlowComponentsMap);

        // Verifications
        assertThat(viewWidget.model.getFlowComponents().size(), is(1));
        assertThat(viewWidget.model.getFlowComponents().get(0).getName(), is(flowComponentModel4.getName()));
    }

    @Test
    public void keyPressed_callKeyPressed_statusFieldIsCleared() {

        // Setup
        setupPresenterImpl();

        // Subject Under Test
        presenterImpl.keyPressed();

        // Verifications
        verify(viewWidget.status).setText("");
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithNameFieldEmpty_ErrorTextIsDisplayed() {

        // Setup
        setupPresenterImpl();
        viewWidget.model.setFlowName("");

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithDescriptionFieldEmpty_ErrorTextIsDisplayed() {

        // Setup
        setupPresenterImpl();
        viewWidget.model.setDescription("");

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithFlowComponentListEmpty_ErrorTextIsDisplayed() {

        // Setup
        setupPresenterImpl();
        viewWidget.model.setFlowComponents(new ArrayList<>());

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithInvalidCharactersInNameField_ErrorTextIsDisplayed() {

        // Setup
        setupPresenterImpl();
        viewWidget.model = new FlowModelBuilder().setName("*(Flow name)*_%€").build();

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        verify(mockedTexts).error_NameFormatValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithValidData_SaveModelIsCalled() {

        // Setup
        setupPresenterImpl();
        viewWidget.model = new FlowModelBuilder().build();

        // Subject Under Test
        presenterImpl.saveButtonPressed();

        // Verifications
        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test
    public void addButtonPressed_callAddButtonPressedWithNullFlowComponents_popupListBoxNotCalled() {

        // Setup
        setupPresenterImpl();
        presenterImpl.setAvailableFlowComponentModels(null);

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verifications
        verifyNoMoreInteractions(viewWidget.popupListBox);
    }

    @Test
    public void addButtonPressed_callAddButtonPressedWithEmptyFlowComponents_popupListBoxCleared() {

        // Setup
        setupPresenterImpl();
        presenterImpl.setAvailableFlowComponentModels(new ArrayList<>());

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verifications
        verify(viewWidget.popupListBox).clear();
        verify(viewWidget.popupListBox).show();
        verifyNoMoreInteractions(viewWidget.popupListBox);
    }

    @Test
    public void addButtonPressed_callAddButtonPressedWithFlowComponents_popupListBoxFilled() {

        // Setup
        setupPresenterImpl();

        // Subject Under Test
        presenterImpl.addButtonPressed();

        // Verifications
        verify(viewWidget.popupListBox).clear();
        verify(viewWidget.popupListBox).addItem("FlowComponentName2", "222");
        verify(viewWidget.popupListBox).addItem("FlowComponentName4", "444");
        verify(viewWidget.popupListBox).show();
        verifyNoMoreInteractions(viewWidget.popupListBox);
    }

    @Test
    public void removeButtonPressed_callRemoveButtonPressedNoFlowComponentsSelected_noFlowComponentsRemoved() {
        // Setup
        setupPresenterImpl();
        when(viewWidget.flowComponents.getSelectedItem()).thenReturn(null);

        // Subject Under Test
        presenterImpl.removeButtonPressed();

        // Verifications
        List<FlowComponentModel> flowComponents = viewWidget.model.getFlowComponents();
        assertThat(flowComponents.size(), is(2));
        assertThat(flowComponents.get(0).getId(), is(111L));
        assertThat(flowComponents.get(1).getId(), is(333L));
    }

    @Test
    public void removeButtonPressed_callRemoveButtonPressedAFlowComponentsSelected_oneFlowComponentsRemoved() {
        // Setup
        setupPresenterImpl();
        when(viewWidget.flowComponents.getSelectedItem()).thenReturn("111");

        // Subject Under Test
        presenterImpl.removeButtonPressed();

        // Verifications
        List<FlowComponentModel> flowComponents = viewWidget.model.getFlowComponents();
        assertThat(flowComponents.size(), is(1));
        assertThat(flowComponents.get(0).getId(), is(333L));
    }

    @Test(expected = NullPointerException.class)
    public void selectFlowComponentButtonPressed_nullInput_exception() {
        // Setup
        setupPresenterImpl();

        // Subject Under Test
        presenterImpl.selectFlowComponentButtonPressed(null);
    }

    @Test(expected = NumberFormatException.class)
    public void selectFlowComponentButtonPressed_emptyInput_noAction() {
        // Setup
        setupPresenterImpl();

        // Subject Under Test
        final Map<String, String> flowComponent = new HashMap<>();
        flowComponent.put("", "");
        presenterImpl.selectFlowComponentButtonPressed(flowComponent);
    }

    @Test
    public void selectFlowComponentButtonPressed_numberInput_selectTheRightFlowComponent() {
        // Setup
        setupPresenterImpl();

        // Subject Under Test
        final Map<String, String> flowComponent = new HashMap<>();
        flowComponent.put("444", "444");
        presenterImpl.selectFlowComponentButtonPressed(flowComponent);

        // Verifications
        List<FlowComponentModel> flowComponents = viewWidget.model.getFlowComponents();
        assertThat(flowComponents.size(), is(3));
        assertThat(flowComponents.get(0).getId(), is(111L));
        assertThat(flowComponents.get(1).getId(), is(333L));
        assertThat(flowComponents.get(2).getId(), is(444L));  // Newly selected
    }

    @Test
    public void newFlowComponentButtonPressed_call_gotoCreateFlowComponentPlace() {
        // Setup
        setupPresenterImpl();

        // Subject Under Test
        presenterImpl.newFlowComponentButtonPressed();

        // Verifications
        assertThat(presenterImpl.getView().showAvailableFlowComponents, is(true));
        verify(mockedPlaceController).goTo(any(dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace.class));
    }

    @Test
    public void findAllFlowComponentsAsyncCallback_successfulCallback_FlowComponentsAddedAndModelUpdated() {
        // Setup
        final long EXTRA_ID = 127;
        final String EXTRA_NAME = "Extra Name";
        setupPresenterImpl();

        List<FlowComponentModel> flowComponentModels = new ArrayList<>(availableFlowComponentModelList);
        flowComponentModels.add(new FlowComponentModelBuilder().setId(EXTRA_ID).setName(EXTRA_NAME).build());

        // Subject Under Test
        presenterImpl.findAllFlowComponentsCallback.onSuccess(flowComponentModels);

        // Verifications
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
    public void findAllFlowComponentsAsyncCallback_successfulCallback_showAvailableFlowComponentsIsTrueAddButtonPressed() {
        // Setup
        setupPresenterImpl();
        viewWidget.showAvailableFlowComponents = true;

        // Subject Under Test
        presenterImpl.findAllFlowComponentsCallback.onSuccess(availableFlowComponentModelList);

        // Verifications
        assertThat(viewWidget.showAvailableFlowComponents, is(false));
        verify(viewWidget.flowComponents).setEnabled(true);
    }

    @Test
    public void findAllFlowComponentsAsyncCallback_unsuccessfulCallbackNotFoundError_errorMessageDisplayed() {
        // Setup
        setupPresenterImpl();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.ENTITY_NOT_FOUND);

        // Subject Under Test
        presenterImpl.findAllFlowComponentsCallback.onFailure(mockedProxyException);

        // Verifications
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_notFoundError();
    }

    @Test
    public void saveFlowModelAsyncCallback_successfulCallback_statusMessageDisplayed() {
        // Setup
        final String STATUS_MESSAGE = "Success Message";
        setupPresenterImpl();
        FlowModel model = new FlowModel();
        when(mockedTexts.status_FlowSuccessfullySaved()).thenReturn(STATUS_MESSAGE);

        // Subject Under Test
        presenterImpl.saveFlowCallback.onSuccess(model);

        // Verifications
        verify(viewWidget.status).setText(STATUS_MESSAGE);
    }

    @Test
    public void saveFlowModelAsyncCallback_unsuccessfulCallbackConflictError_errorMessageDisplayed() {
        // Setup
        setupPresenterImpl();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.CONFLICT_ERROR);

        // Subject Under Test
        presenterImpl.saveFlowCallback.onFailure(mockedProxyException);

        // Verifications
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_conflictError();
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImplConcrete(mockedPlaceController, header);
    }
}
