package dk.dbc.dataio.gui.client.components.popup;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.events.DialogHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PopupValueBox unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PopupBoxTest {
    @Mock
    KeyPressEvent mockedKeyPressEvent;
    @Mock
    TextBox mockedWidget;
    @Mock
    FlowPanel mockedBasePanel;
    @Mock
    DialogBox mockedDialogBox;
    @Mock
    VerticalPanel mockedContainerPanel;
    @Mock
    FlowPanel mockedButtonPanel;
    @Mock
    Button mockedOkButton;
    @Mock
    Button mockedCancelButton;
    @Mock
    Button mockedExtraButton;
    @Mock
    ClickEvent mockedClickEvent;
    @Mock
    ValueChangeHandler mockedValueChangeHandler;
    @SuppressWarnings("deprecation")
    @Mock
    com.google.gwt.user.client.Element mockedElement;
    @Mock
    DialogHandler mockedDialogHandler;


    /**
     * Subject Under Test
     */
    PopupBox<TextBox> popupBox;


    /**
     * Tests starts here...
     */

    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        setupTest();

        // Test verification
        constructorVerification();
        noMoreMockInvocations();
    }

    @Test
    public void setDialogTitle_nullValue_nullValueSetAsDialogTitle() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setDialogTitle(null);

        // Test Verification
        verify(popupBox.dialogBox).setText(null);
        noMoreMockInvocations();
    }

    @Test
    public void setDialogTitle_emptyValue_emptyValueSetAsDialogTitle() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setDialogTitle("");

        // Test Verification
        verify(popupBox.dialogBox).setText("");
        noMoreMockInvocations();
    }

    @Test
    public void setDialogTitle_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setDialogTitle("-Dialog-Title-");

        // Test Verification
        verify(popupBox.dialogBox).setText("-Dialog-Title-");
        noMoreMockInvocations();
    }

    @Test(expected = NullPointerException.class)
    public void setOkButtonText_nullValue_exception() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupBox.setOkButtonText(null);
    }

    @Test
    public void setOkButtonText_emptyValue_buttonIsDisabled() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setOkButtonText("");

        // Test Verification
        verify(mockedOkButton).setEnabled(false);
        verify(mockedOkButton).setVisible(false);
        noMoreMockInvocations();
    }

    @Test
    public void setOkButtonText_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setOkButtonText("-Ok-Button-Text-");

        // Test Verification
        verify(mockedOkButton).setText("-Ok-Button-Text-");
        verify(mockedOkButton, times(2)).setEnabled(true);
        verify(mockedOkButton, times(2)).setVisible(true);
        noMoreMockInvocations();
    }

    @Test(expected = NullPointerException.class)
    public void setCancelButtonText_nullValue_exception() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupBox.setCancelButtonText(null);
    }

    @Test
    public void setCancelButtonText_emptyValue_buttonIsDisabled() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setCancelButtonText("");

        // Test Verification
        verify(mockedCancelButton, times(2)).setEnabled(false);
        verify(mockedCancelButton, times(2)).setVisible(false);
        noMoreMockInvocations();
    }

    @Test
    public void setCancelButtonText_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setCancelButtonText("-Cancel-Button-Text-");

        // Test Verification
        verify(mockedCancelButton).setText("-Cancel-Button-Text-");
        verify(mockedCancelButton).setEnabled(true);
        verify(mockedCancelButton).setVisible(true);
        noMoreMockInvocations();
    }

    @Test(expected = NullPointerException.class)
    public void setExtraButtonText_nullValue_exception() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupBox.setExtraButtonText(null);
    }

    @Test
    public void setExtraButtonText_emptyValue_buttonIsDisabled() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setExtraButtonText("");

        // Test Verification
        verify(mockedExtraButton, times(2)).setEnabled(false);
        verify(mockedExtraButton, times(2)).setVisible(false);
        noMoreMockInvocations();
    }

    @Test
    public void setExtraButtonText_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setExtraButtonText("-Extra-Button-Text-");

        // Test Verification
        verify(mockedExtraButton).setText("-Extra-Button-Text-");
        verify(mockedExtraButton).setEnabled(true);
        verify(mockedExtraButton).setVisible(true);
        noMoreMockInvocations();
    }

    @Test(expected = NullPointerException.class)
    public void setGuid_nullValue_exception() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.setGuid(null);
    }

    @Test
    public void setGuid_emptyValue_noAction() {
        // Test Preparation
        setupTest();
        constructorVerification();
        when(mockedDialogBox.getElement()).thenReturn(mockedElement);

        // Activate Subject Under Test
        popupBox.setGuid("");

        // Test Verification
        noMoreMockInvocations();
    }

    @Test
    public void setGuid_validValue_setGuid() {
        // Test Preparation
        setupTest();
        constructorVerification();
        when(mockedDialogBox.getElement()).thenReturn(mockedElement);

        // Activate Subject Under Test
        popupBox.setGuid("gui ID");

        // Test Verification
        verify(mockedDialogBox).getElement();
        verify(mockedElement).setId("gui ID");
        noMoreMockInvocations();
    }

    @Test
    public void okClickHandler_call_hide() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.okClickHandler(mockedClickEvent);

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        noMoreMockInvocations();
    }

    @Test
    public void cancelClickHandler_call_hide() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.cancelClickHandler(mockedClickEvent);

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        noMoreMockInvocations();
    }

    @Test
    public void extraClickHandler_call_hide() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.extraClickHandler(mockedClickEvent);

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        noMoreMockInvocations();
    }

    @Test
    public void show_call_show() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.show();

        // Test Verification
        verify(mockedDialogBox, times(2)).center();
        verify(mockedDialogBox, times(2)).show();
        noMoreMockInvocations();
    }

    @Test
    public void hide_call_hide() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.hide();

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        noMoreMockInvocations();
    }

    @Test
    public void addDialogHandler_okClick_bothHideAndTriggerEvent() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupBox.addDialogHandler(mockedDialogHandler);
        popupBox.okClickHandler(mockedClickEvent);

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        verify(mockedDialogHandler).onDialogButtonClick(any(DialogEvent.class));
        noMoreMockInvocations();
    }


    /**
     * Private methods
     */

    private void setupTest() {
        popupBox = new PopupBox<TextBox>(
                mockedWidget,
                "Dialog Title",
                "Ok Button Text",
                mockedBasePanel,
                mockedDialogBox,
                mockedContainerPanel,
                mockedButtonPanel,
                mockedOkButton,
                mockedCancelButton,
                mockedExtraButton);
    }

    private void constructorVerification() {
        verify(mockedDialogBox).setText("Dialog Title");
        verify(mockedOkButton).setText("Ok Button Text");
        verify(mockedOkButton).setEnabled(true);
        verify(mockedOkButton).setVisible(true);
        verify(mockedCancelButton).setEnabled(false);
        verify(mockedCancelButton).setVisible(false);
        verify(mockedExtraButton).setEnabled(false);
        verify(mockedExtraButton).setVisible(false);
        verify(mockedOkButton).addClickHandler(any(ClickHandler.class));
        verify(mockedCancelButton).addClickHandler(any(ClickHandler.class));
        verify(mockedExtraButton).addClickHandler(any(ClickHandler.class));
        verify(mockedButtonPanel).add(mockedOkButton);
        verify(mockedButtonPanel).add(mockedCancelButton);
        verify(mockedButtonPanel).add(mockedExtraButton);
//        verify(mockedContainerPanel).add(mockedWidget);   // For some reason this should be verified, but Mockito states, that it is not...!!!
        verify(mockedContainerPanel).add(any(IsWidget.class));  // ... therefore this is the only way, that the call to add(mockedWidget) can be verified.... !!!
        verify(mockedContainerPanel).add(mockedButtonPanel);
        verify(mockedDialogBox).add(mockedContainerPanel);
        verify(mockedBasePanel).add(mockedDialogBox);
        verify(mockedDialogBox).addStyleName("dio-PopupBox");
        verify(mockedDialogBox).setAutoHideEnabled(true);
        verify(mockedDialogBox).setModal(true);
        verify(mockedDialogBox).isAnimationEnabled();
        verify(mockedDialogBox, times(2)).setAnimationEnabled(false);
        verify(mockedDialogBox).center();
        verify(mockedDialogBox).show();
        verify(mockedDialogBox).hide();
    }

    private void noMoreMockInvocations() {
        verifyNoMoreInteractions(mockedDialogBox);
        verifyNoMoreInteractions(mockedOkButton);
        verifyNoMoreInteractions(mockedCancelButton);
        verifyNoMoreInteractions(mockedExtraButton);
        verifyNoMoreInteractions(mockedButtonPanel);
        verifyNoMoreInteractions(mockedContainerPanel);
        verifyNoMoreInteractions(mockedBasePanel);
        verifyNoMoreInteractions(mockedWidget);
        verifyNoMoreInteractions(mockedKeyPressEvent);
        verifyNoMoreInteractions(mockedElement);
        verifyNoMoreInteractions(mockedDialogHandler);
    }

}
