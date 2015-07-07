
package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
    @Mock private ClientFactory mockedClientFactory;
    @Mock private FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock private Texts mockedTexts;
    @Mock private ProxyErrorTexts mockedProxyErrorTexts;
    @Mock private AcceptsOneWidget mockedContainerWidget;
    @Mock private EventBus mockedEventBus;
    @Mock private ProxyException mockedProxyException;

    private View view;
    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;
    private static boolean initializeModelHasBeenCalled;
    private final SubmitterModel submitterModel = new SubmitterModelBuilder().build();

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
            view = PresenterImplTest.this.view;
            model = submitterModel;
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
            return flowStoreProxy;
        }

        // Test method for reading constants
        public Texts getSubmitterModifyTexts() {
            return texts;
        }

        public ProxyErrorTexts getProxyErrorTexts() {
            return proxyErrorTexts;
        }

        @Override
        public void deleteButtonPressed() {
            deleteButtonPressed();
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getSubmitterModifyTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    @Before
    public void setupView() {
        view = new View("Header Text");  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        assertThat(presenterImpl.getFlowStoreProxy(), is(mockedFlowStoreProxy));
        assertThat(presenterImpl.getSubmitterModifyTexts(), is(mockedTexts));
        assertThat(presenterImpl.getProxyErrorTexts(), is(mockedProxyErrorTexts));
    }

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(mockedContainerWidget).setWidget(Matchers.any(IsWidget.class));
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
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.model.setName("");

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
    public void saveButtonPressed_callSaveButtonPressedWithInvalidCharactersInNameField_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.model.setName("*(Flow name)*_%€");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_NameFormatValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithInvalidNumberFormatInNameField_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.model.setNumber("Not a number");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_NumberInputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithValidData_SaveModelIsCalled() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

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

     /*
     * Private methods
     */

    private void initializeAndStartPresenter() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }

}