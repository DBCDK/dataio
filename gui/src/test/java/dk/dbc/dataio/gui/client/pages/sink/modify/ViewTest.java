package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
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
public class ViewTest {

    @Mock
    ViewGinjector mockedViewInjector;
    @Mock
    Presenter mockedPresenter;
    @Mock
    ValueChangeEvent mockedValueChangeEvent;
    @Mock
    ClickEvent mockedClickEvent;
    @Mock
    DialogEvent mockedDialogEvent;


    // Subject Under Test
    private View view;


    // Testing starts here...
    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        view = new View();

        // Test Verification
        verifyNoMoreInteractions(view.sinkTypeSelection);
        verifyNoMoreInteractions(view.name);
        verifyNoMoreInteractions(view.queue);
        verifyNoMoreInteractions(view.description);
        verifyNoMoreInteractions(view.updateSinkSection);
        verifyNoMoreInteractions(view.url);
        verifyNoMoreInteractions(view.openupdateuserid);
        verifyNoMoreInteractions(view.openupdatepassword);
        verifyNoMoreInteractions(view.queueProviders);
        verifyNoMoreInteractions(view.worldCatUserId);
        verifyNoMoreInteractions(view.worldCatPassword);
        verifyNoMoreInteractions(view.worldCatProjectId);
        verifyNoMoreInteractions(view.deleteButton);
        verifyNoMoreInteractions(view.status);
        verifyNoMoreInteractions(view.queueProvidersPopupTextBox);
        verifyNoMoreInteractions(view.worldCatPopupTextBox);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sinkTypeSelectionChanged_unknownSinkType_exception() {
        // Test preparation
        setupView();
        when(view.sinkTypeSelection.getSelectedKey()).thenReturn("UNKNOWN TYPE");

        // Subject Under Test
        view.sinkTypeSelectionChanged(mockedValueChangeEvent);
    }

    @Test
    public void nameChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.name.getText()).thenReturn("-name-");

        // Subject Under Test
        view.nameChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).nameChanged("-name-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void queueChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.queue.getText()).thenReturn("-queue-");

        // Subject Under Test
        view.queueChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).queueChanged("-queue-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void descriptionChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.description.getText()).thenReturn("-description-");

        // Subject Under Test
        view.descriptionChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).descriptionChanged("-description-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void urlChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.url.getText()).thenReturn("-url-");

        // Subject Under Test
        view.urlChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).endpointChanged("-url-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void userIdChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.openupdateuserid.getText()).thenReturn("-userid-");

        // Subject Under Test
        view.useridChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).openUpdateUserIdChanged("-userid-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void passwordChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.openupdatepassword.getText()).thenReturn("-openupdatepassword-");

        // Subject Under Test
        view.passwordChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).passwordChanged("-openupdatepassword-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void availableQueueProvidersChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        Map<String, String> queueProviders = new HashMap<>();
        queueProviders.put("key1", "value1");
        queueProviders.put("key2", "value2");
        when(view.queueProviders.getValue()).thenReturn(queueProviders);

        // Subject Under Test
        view.availableQueueProvidersChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).queueProvidersChanged(Arrays.asList("value1", "value2"));
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void esUserIdChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.esUserId.getText()).thenReturn("-es user id-");

        // Subject Under Test
        view.esUserIdChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).esUserIdChanged("-es user id-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void esDatabaseChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.esDatabase.getText()).thenReturn("-es dbase-");

        // Subject Under Test
        view.esDatabaseChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).esDatabaseChanged("-es dbase-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void imsEndpointChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.imsEndpoint.getText()).thenReturn("-ims endpoint-");

        // Subject Under Test
        view.imsEndpointChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).imsEndpointChanged("-ims endpoint-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void worldCatUserIdChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.worldCatUserId.getText()).thenReturn("-userid-");

        // Subject Under Test
        view.worldCatUserIdChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).worldCatUserIdChanged("-userid-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void worldCatPasswordChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.worldCatPassword.getText()).thenReturn("-worldcatpassword-");

        // Subject Under Test
        view.worldCatPasswordChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).worldCatPasswordChanged("-worldcatpassword-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void worldCatProjectIdChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.worldCatProjectId.getText()).thenReturn("-worldcatprojectid-");

        // Subject Under Test
        view.worldCatProjectIdChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).worldCatProjectIdChanged("-worldcatprojectid-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void worldCatEndpointChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        when(view.worldCatEndpoint.getText()).thenReturn("-worldcatendpoint-");

        // Subject Under Test
        view.worldCatEndpointChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).worldCatEndpointChanged("-worldcatendpoint-");
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void worldCatRetryDiagnosticsChanged_called_presenterNotified() {
        // Test preparation
        setupView();
        Map<String, String> retryDiagnotics = new HashMap<>();
        retryDiagnotics.put("key1", "value1");
        retryDiagnotics.put("key2", "value2");
        when(view.worldCatRetryDiagnostics.getValue()).thenReturn(retryDiagnotics);

        // Subject Under Test
        view.worldCatRetryDiagnosticsChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).worldCatRetryDiagnosticsChanged(Arrays.asList("value1", "value2"));
        verify(mockedPresenter).keyPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void saveButtonPressed_called_presenterNotified() {
        // Test preparation
        setupView();

        // Subject Under Test
        view.saveButtonPressed(mockedClickEvent);

        // Test Verification
        verify(mockedPresenter).saveButtonPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void deleteButtonPressed_called_presenterNotified() {
        // Test preparation
        setupView();

        // Subject Under Test
        view.deleteButtonPressed(mockedClickEvent);

        // Test Verification
        verify(view.confirmation).show();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void availableQueueProvidersButtonClicked_isAddEvent_presenterNotified() {
        // Test preparation
        setupView();
        when(view.queueProviders.isAddEvent(any(ClickEvent.class))).thenReturn(true);

        // Subject Under Test
        view.availableQueueProvidersButtonClicked(mockedClickEvent);

        // Test Verification
        verify(mockedPresenter).queueProvidersAddButtonPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void availableQueueProvidersButtonClicked_isNotAddEvent_presenterNotNotified() {
        // Test preparation
        setupView();
        when(view.queueProviders.isAddEvent(any(ClickEvent.class))).thenReturn(false);

        // Subject Under Test
        view.availableQueueProvidersButtonClicked(mockedClickEvent);

        // Test Verification
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void sequenceAnalysisSelectionChanged_all_presenterNotified() {
        // Test preparation
        setupView();
        when(mockedValueChangeEvent.getValue()).thenReturn("ALL");

        // Subject Under Test
        view.sequenceAnalysisSelectionChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).sequenceAnalysisSelectionChanged("ALL");
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void sequenceAnalysisSelectionChanged_idOnly_presenterNotified() {
        // Test preparation
        setupView();
        when(mockedValueChangeEvent.getValue()).thenReturn("ID_ONLY");

        // Subject Under Test
        view.sequenceAnalysisSelectionChanged(mockedValueChangeEvent);

        // Test Verification
        verify(mockedPresenter).sequenceAnalysisSelectionChanged("ID_ONLY");
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test(expected = NullPointerException.class)
    public void popupTextBoxChanged_nullEvent_exception() {
        // Test preparation
        setupView();

        // Subject Under Test
        view.popupTextBoxChanged(null);
    }

    @Test
    public void popupTextBoxChanged_cancelEvent_setPopupContent() {
        // Test preparation
        setupView();
        Map<String, String> qProviders = new HashMap<>();
        qProviders.put("key1", "value1");
        when(view.queueProviders.getValue()).thenReturn(qProviders);
        when(mockedDialogEvent.getDialogButton()).thenReturn(DialogEvent.DialogButton.CANCEL_BUTTON);
        when(view.queueProvidersPopupTextBox.getValue()).thenReturn("provider2");

        // Subject Under Test
        view.popupTextBoxChanged(mockedDialogEvent);

        // Test Verification
        verify(mockedDialogEvent).getDialogButton();
        verifyNoMoreInteractions(mockedDialogEvent);
        verifyNoMoreInteractions(view.queueProviders);
    }

    @Test
    public void popupTextBoxChanged_okEvent_setPopupContent() {
        // Test preparation
        setupView();
        Map<String, String> qProviders = new HashMap<>();
        qProviders.put("key1", "value1");
        when(view.queueProviders.getValue()).thenReturn(qProviders);
        when(mockedDialogEvent.getDialogButton()).thenReturn(DialogEvent.DialogButton.OK_BUTTON);
        when(view.queueProvidersPopupTextBox.getValue()).thenReturn("provider2");
        when(view.sinkTypeSelection.getSelectedKey()).thenReturn("DUMMY");

        // Subject Under Test
        view.popupTextBoxChanged(mockedDialogEvent);

        // Test Verification
        verify(mockedDialogEvent).getDialogButton();
        verifyNoMoreInteractions(mockedDialogEvent);
        verify(view.queueProviders).getValue();
        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("key1", "value1");
        expectedResult.put("provider2", "provider2");
        verify(view.queueProviders).setValue(expectedResult, true);
        verifyNoMoreInteractions(view.queueProviders);
    }

    @Test
    public void confirmationButtonClicked_okButton_presenterCalled() {
        // Test preparation
        setupView();
        when(mockedDialogEvent.getDialogButton()).thenReturn(DialogEvent.DialogButton.OK_BUTTON);

        // Subject Under Test
        view.confirmationButtonClicked(mockedDialogEvent);

        // Test Verification
        verify(mockedPresenter).deleteButtonPressed();
        verifyNoMoreInteractions(mockedPresenter);
    }

    @Test
    public void confirmationButtonClicked_cancelButton_presenterNotCalled() {
        // Test preparation
        setupView();
        when(mockedDialogEvent.getDialogButton()).thenReturn(DialogEvent.DialogButton.CANCEL_BUTTON);

        // Subject Under Test
        view.confirmationButtonClicked(mockedDialogEvent);

        // Test Verification
        verifyNoMoreInteractions(mockedPresenter);
    }


    /**
     * Private methods
     */
    private void setupView() {
        view = new View();
        view.setPresenter(mockedPresenter);
    }

}
