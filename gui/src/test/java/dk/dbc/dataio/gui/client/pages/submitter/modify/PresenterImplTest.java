package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private Texts mockedTexts;
    @Mock
    private ViewGinjector mockedViewGinjector;

    private View view;
    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;
    private static boolean initializeModelHasBeenCalled;
    private final SubmitterModel submitterModel = new SubmitterModelBuilder().build();

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(String header) {
            super(header);
            view = PresenterImplTest.this.view;
            model = submitterModel;
            viewInjector = mockedViewGinjector;
            commonInjector = mockedCommonGinjector;
        }

        @Override
        void initializeModel() {
            initializeModelHasBeenCalled = true;
        }

        @Override
        void saveModel() {
            saveModelHasBeenCalled = true;
        }

        public SaveSubmitterModelFilteredAsyncCallback saveSubmitterModelFilteredAsyncCallback = new SaveSubmitterModelFilteredAsyncCallback();

        // Test method for reading flowStoreProxy
        public FlowStoreProxyAsync getFlowStoreProxy() {
            return mockedFlowStore;
        }

        // Test method for reading constants
        public Texts getSubmitterModifyTexts() {
            return mockedTexts;
        }

        public ProxyErrorTexts getProxyErrorTexts() {
            return mockedProxyErrorTexts;
        }

        @Override
        public void deleteButtonPressed() {
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        view = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedViewGinjector.getView()).thenReturn(view);
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenterImplConcrete();
        assertThat(presenterImpl.getFlowStoreProxy(), is(mockedFlowStore));
        assertThat(presenterImpl.getSubmitterModifyTexts(), is(mockedTexts));
        assertThat(presenterImpl.getProxyErrorTexts(), is(mockedProxyErrorTexts));
    }

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(mockedContainerWidget).setWidget(any(IsWidget.class));
        assertThat(initializeModelHasBeenCalled, is(true));
    }

    @Test
    public void numberChanged_callNumber_numberIsChangedAccordingly() {
        final String CHANGED_NUMBER = "3435";
        initializeAndStartPresenter();
        presenterImpl.numberChanged(CHANGED_NUMBER);
        assertThat(presenterImpl.model.getNumber(), is(CHANGED_NUMBER));
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "3435";
        initializeAndStartPresenter();
        presenterImpl.nameChanged(CHANGED_NAME);
        assertThat(presenterImpl.model.getName(), is(CHANGED_NAME));
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {
        final String CHANGED_DESCRIPTION = "3435";
        initializeAndStartPresenter();
        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);
        assertThat(presenterImpl.model.getDescription(), is(CHANGED_DESCRIPTION));
    }

    @Test
    public void priorityChanged_callPriorityChangedNullPriority_priorityIsChangedAccordingly() {
        initializeAndStartPresenter();
        presenterImpl.priorityChanged(null);
        assertThat(presenterImpl.model.getPriority(), is(nullValue()));
    }

    @Test
    public void priorityChanged_callPriorityChangedNegativePriority_priorityIsChangedAccordingly() {
        initializeAndStartPresenter();
        presenterImpl.priorityChanged("-123");
        assertThat(presenterImpl.model.getPriority(), is(nullValue()));
    }

    @Test
    public void priorityChanged_callPriorityChangedPositivePriority_priorityIsChangedAccordingly() {
        initializeAndStartPresenter();
        presenterImpl.priorityChanged("4");
        assertThat(presenterImpl.model.getPriority(), is(4));
    }

    @Test
    public void disabledStatusChanged_callDisabledStatusChangedWithFalse_enabledChangedAccordingly() {
        initializeAndStartPresenter();
        presenterImpl.disabledStatusChanged(false);
        assertThat(presenterImpl.model.isEnabled(), is(true));
    }

    @Test
    public void disabledStatusChanged_callDisabledStatusChangedWithTrue_enabledChangedAccordingly() {
        initializeAndStartPresenter();
        presenterImpl.disabledStatusChanged(true);
        assertThat(presenterImpl.model.isEnabled(), is(false));
    }

    @Test
    public void keyPressed_callKeyPressed_statusFieldIsCleared() {
        initializeAndStartPresenter();
        presenterImpl.keyPressed();
        verify(view.status).setText("");
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressed_saveModelIsCalled() {
        initializeAndStartPresenter();
        saveModelHasBeenCalled = false;
        presenterImpl.saveButtonPressed();
        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithNameFieldEmpty_ErrorTextIsDisplayed() {
        setupPresenterImplConcrete();
        presenterImpl.model.setName("");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithDescriptionFieldEmpty_ErrorTextIsDisplayed() {
        setupPresenterImplConcrete();
        presenterImpl.model.setDescription("");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithInvalidCharactersInNameField_ErrorTextIsDisplayed() {
        setupPresenterImplConcrete();
        presenterImpl.model.setName("*(Flow name)*_%€");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_NameFormatValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithInvalidNumberFormatInNameField_ErrorTextIsDisplayed() {
        setupPresenterImplConcrete();
        presenterImpl.model.setNumber("Not a number");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_NumberInputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithValidData_SaveModelIsCalled() {
        setupPresenterImplConcrete();

        presenterImpl.saveButtonPressed();

        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test
    public void submitterModelFilteredAsyncCallback_successfulCallback_setStatusTextCalledInView() {
        final String SUCCESS_TEXT = "SuccessText";
        initializeAndStartPresenter();
        when(mockedTexts.status_SubmitterSuccessfullySaved()).thenReturn(SUCCESS_TEXT);

        presenterImpl.saveSubmitterModelFilteredAsyncCallback.onSuccess(submitterModel);  // Emulate a successful callback from flowstore

        verify(view.status).setText(SUCCESS_TEXT);  // Expect the status text to be set in View
        assertThat(presenterImpl.model, is(submitterModel));

    }

    @Test
    public void submitterModelFilteredAsyncCallback_unsuccessfulCallbackNotAcceptableError_setErrorTextCalledInView() {
        initializeAndStartPresenter();
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.NOT_ACCEPTABLE);

        presenterImpl.saveSubmitterModelFilteredAsyncCallback.onFailure(mockedProxyException);  // Emulate an unsuccessful callback from flowstore
        verify(mockedProxyException).getErrorCode();

        verify(mockedProxyErrorTexts).flowStoreProxy_keyViolationError();
    }

    @Test
    public void setAvailablePriorities_call_prioritiesInDropdownBoxSetCorrect() {
        when(mockedTexts.selection_High()).thenReturn("High");
        when(mockedTexts.selection_Normal()).thenReturn("Normal");
        when(mockedTexts.selection_Low()).thenReturn("Low");
        when(mockedTexts.selection_UseFlowbinderPriority()).thenReturn("UseFlowbinder");
        initializeAndStartPresenter();  // setAvailablePriorities is called from here

        verify(view.priority).addAvailableItem("High", "7");
        verify(view.priority).addAvailableItem("Normal", "4");
        verify(view.priority).addAvailableItem("Low", "1");
        verify(view.priority).addAvailableItem("UseFlowbinder", "-1");
        verify(view.priority).setSelectedValue("-1");
        verify(view.priority).setEnabled(false);
        verifyNoMoreInteractions(view.priority);
    }


    /*
     * Private methods
     */
    private void initializeAndStartPresenter() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }

    private void setupPresenterImplConcrete() {
        presenterImpl = new PresenterImplConcrete(header);
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.viewInjector = mockedViewGinjector;
    }
}
