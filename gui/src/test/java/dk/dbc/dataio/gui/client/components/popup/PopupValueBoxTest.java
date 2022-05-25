package dk.dbc.dataio.gui.client.components.popup;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
public class PopupValueBoxTest {
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
    ValueChangeHandler mockedValueChangeHandler;

    /**
     * Subject Under Test
     */
    PopupValueBox<TextBox, String> popupValueBox;


    /**
     * Tests starts here...
     */

    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        setupTest();

        // Test verification
        verify(mockedWidget).setValue(null);
        verify(mockedWidget).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }


    @Test
    public void show_call_show() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupValueBox.show();

        // Test Verification
        verify(mockedWidget, times(2)).setValue(null);
        verify(mockedWidget, times(2)).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }

    @Test
    public void getValue_call_getTheValue() {
        // Test Preparation
        setupTest();
        when(mockedWidget.getValue()).thenReturn("correct value");

        // Activate Subject Under Test
        String value = popupValueBox.getValue();

        // Test Verification
        assertThat(value, is("correct value"));
        verify(mockedWidget).getValue();
        verify(mockedWidget).setValue(null);
        verify(mockedWidget).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }

    @Test
    public void setValue_call_newValueSet() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupValueBox.setValue("new value");

        // Test Verification
        verify(mockedWidget).setValue("new value");
        verify(mockedWidget).setValue(null);
        verify(mockedWidget).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }

    @Test
    public void setValue_callWithFireEvent_newValueSetAndFireEvent() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupValueBox.setValue("new value", true);

        // Test Verification
        verify(mockedWidget).setValue("new value", true);
        verify(mockedWidget).setValue(null);
        verify(mockedWidget).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }

    @Test
    public void addValueChangeHandler_addValueHandler_handlerAdded() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupValueBox.addValueChangeHandler(mockedValueChangeHandler);

        // Test Verification
        verify(mockedWidget).addValueChangeHandler(mockedValueChangeHandler);
        verify(mockedWidget).setValue(null);
        verify(mockedWidget).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }


    /**
     * Private methods
     */

    private void setupTest() {
        popupValueBox = new PopupValueBox<TextBox, String>(
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

}
