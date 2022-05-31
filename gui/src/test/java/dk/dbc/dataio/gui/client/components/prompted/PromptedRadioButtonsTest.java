package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
public class PromptedRadioButtonsTest {
    @Mock
    RadioButton mockedRadioButton;
    @SuppressWarnings("deprecation")
    @Mock
    com.google.gwt.user.client.Element mockedElement;
    @SuppressWarnings("deprecation")
    @Mock
    com.google.gwt.user.client.Element mockedChildElement;
    @Mock
    com.google.gwt.dom.client.Style mockedProperty;

    /**
     * Subject Under Test
     */
    PromptedRadioButtons promptedRadioButtons;


    @Before
    public void setupMocks() {
        promptedRadioButtons = new PromptedRadioButtons("guid", "prompt");
        when(mockedRadioButton.getElement()).thenReturn(mockedElement);
        when(mockedRadioButton.getValue()).thenReturn(false);
        when(mockedElement.getStyle()).thenReturn(mockedProperty);
        when(mockedElement.getFirstChildElement()).thenReturn(mockedChildElement);
        when(mockedChildElement.getAttribute("value")).thenReturn("ChildElementValue");
        when(promptedRadioButtons.containerPanel.getWidgetCount()).thenReturn(1);
        when(promptedRadioButtons.containerPanel.getWidget(0)).thenReturn(mockedRadioButton);
    }


    /**
     * Tests starts here...
     */

    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        PromptedRadioButtons radioButtonsUnderTest = new PromptedRadioButtons("guid", "prompt");

        // Test verification
        verify(radioButtonsUnderTest.containerPanel).addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        verifyNoMoreInteractions(radioButtonsUnderTest.containerPanel);
        testNoMoreUnwantedInteractions();
    }

    @Test(expected = NullPointerException.class)
    public void addButton_nullRadioButton_exception() {
        // Test Preparation
        PromptedRadioButtons radioButtonsUnderTest = new PromptedRadioButtons("guid", "prompt");

        // Activate Subject Under Test
        radioButtonsUnderTest.addButton((RadioButton) null, "value");
        verifyNoMoreInteractions(radioButtonsUnderTest.containerPanel);
        testNoMoreUnwantedInteractions();
    }

    @Test
    public void addButton_validRadioButton_ok() {
        // Activate Subject Under Test
        promptedRadioButtons.addButton(mockedRadioButton, "RADIO_BUTTON_VALUE");

        // Test Verification
        verify(mockedProperty).setProperty("display", "block");
        verify(mockedChildElement).setAttribute("value", "RADIO_BUTTON_VALUE");
        verify(promptedRadioButtons.containerPanel).addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        verify(promptedRadioButtons.containerPanel).add(mockedRadioButton);
        verify(mockedRadioButton, times(4)).getElement();
        verify(mockedRadioButton).addValueChangeHandler(any(ValueChangeHandler.class));
        verify(mockedElement).getStyle();
        verify(mockedElement, times(2)).getFirstChildElement();
        testNoMoreUnwantedInteractions();
    }

    @Test
    public void setOrientation_null_noAction() {
        // Activate Subject Under Test
        promptedRadioButtons.setOrientation(null);
        testNoMoreUnwantedInteractions();
    }

    @Test
    public void setOrientation_vertical_setBlockDisplayProperty() {
        // Activate Subject Under Test
        promptedRadioButtons.setOrientation("verTIcal");

        // Test Verification
        verify(mockedRadioButton).getElement();
        verify(mockedElement).getStyle();
        verify(promptedRadioButtons.containerPanel, times(2)).getWidgetCount();
        verify(promptedRadioButtons.containerPanel).getWidget(0);

        verify(mockedProperty).setProperty("display", "block");
        testNoMoreUnwantedInteractions();
    }

    @Test
    public void setOrientation_horisontal_setInlineDisplayProperty() {
        // Activate Subject Under Test
        promptedRadioButtons.setOrientation("HORisontal");

        // Test Verification
        verify(mockedRadioButton).getElement();
        verify(mockedElement).getStyle();
        verify(promptedRadioButtons.containerPanel, times(2)).getWidgetCount();
        verify(promptedRadioButtons.containerPanel).getWidget(0);

        verify(mockedProperty).setProperty("display", "inline");
        testNoMoreUnwantedInteractions();
    }

    @Test
    public void clear_callClear_allCleared() {
        // Activate Subject Under Test
        promptedRadioButtons.clear();

        // Test Verification
        verify(promptedRadioButtons.containerPanel, times(2)).getWidgetCount();
        verify(promptedRadioButtons.containerPanel).getWidget(0);
        verify(mockedRadioButton).setValue(false);
        testNoMoreUnwantedInteractions();
    }

    @Test
    public void getValue_noSelection_emptyString() {
        // Activate Subject Under Test
        String value = promptedRadioButtons.getValue();

        // Test Verification
        verify(mockedRadioButton).getValue();
        verify(promptedRadioButtons.containerPanel, times(2)).getWidgetCount();
        verify(promptedRadioButtons.containerPanel).getWidget(0);

        assertThat(value, is(""));
        testNoMoreUnwantedInteractions();
    }

    @Test
    public void getValue_elementSelected_selectedItemReturned() {
        // Test Preparation
        when(mockedRadioButton.getValue()).thenReturn(true);

        // Activate Subject Under Test
        String value = promptedRadioButtons.getValue();

        // Test Verification
        verify(mockedRadioButton).getValue();
        verify(mockedRadioButton, times(3)).getElement();
        verify(promptedRadioButtons.containerPanel).getWidgetCount();
        verify(promptedRadioButtons.containerPanel).getWidget(0);
        verify(mockedElement, times(2)).getFirstChildElement();
        verify(mockedChildElement).getAttribute("value");

        assertThat(value, is("ChildElementValue"));
        testNoMoreUnwantedInteractions();
    }

    @Test
    public void setValue_knownValue_selectionDone() {
        // Test Preparation

        // Activate Subject Under Test
        promptedRadioButtons.setValue("ChildElementValue");

        // Test Verification
        verify(promptedRadioButtons.containerPanel, times(2)).getWidgetCount();
        verify(promptedRadioButtons.containerPanel, times(2)).getWidgetCount();
        verify(promptedRadioButtons.containerPanel).getWidget(0);
        verify(mockedElement, times(2)).getFirstChildElement();
        verify(mockedRadioButton, times(3)).getElement();
        verify(mockedChildElement).getAttribute("value");

        verify(mockedRadioButton).setValue(true);
        testNoMoreUnwantedInteractions();

        when(mockedRadioButton.getValue()).thenReturn(true);
        String value = promptedRadioButtons.getValue();
        assertThat(value, is("ChildElementValue"));
    }

    @Test
    public void setValue_unknownValue_selectionNotDone() {
        // Test Preparation

        // Activate Subject Under Test
        promptedRadioButtons.setValue("UnknownChildElementValue");

        // Test Verification
        verify(promptedRadioButtons.containerPanel, times(2)).getWidgetCount();
        verify(promptedRadioButtons.containerPanel, times(2)).getWidgetCount();
        verify(promptedRadioButtons.containerPanel).getWidget(0);
        verify(mockedElement, times(2)).getFirstChildElement();
        verify(mockedRadioButton, times(3)).getElement();
        verify(mockedChildElement).getAttribute("value");

        verify(mockedRadioButton).setValue(false);
        testNoMoreUnwantedInteractions();

        when(mockedRadioButton.getValue()).thenReturn(false);
        String value = promptedRadioButtons.getValue();
        assertThat(value, is(""));
    }



    /*
     * Private methods
     */

    private void testNoMoreUnwantedInteractions() {
        verifyNoMoreInteractions(mockedRadioButton);
        verifyNoMoreInteractions(mockedElement);
        verifyNoMoreInteractions(mockedChildElement);
        verifyNoMoreInteractions(mockedProperty);
        verify(promptedRadioButtons.containerPanel).addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        verifyNoMoreInteractions(promptedRadioButtons.containerPanel);
    }

}
