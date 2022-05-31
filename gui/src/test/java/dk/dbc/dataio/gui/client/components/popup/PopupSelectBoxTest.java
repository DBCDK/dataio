package dk.dbc.dataio.gui.client.components.popup;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PopupSelectBoxTest unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PopupSelectBoxTest {

    @Mock
    RadioButton mockedRadioButtonLeft;
    @Mock
    RadioButton mockedRadioButtonRight;
    @Mock
    FlowPanel mockedRadioButtonsPanel;
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
    ValueChangeEvent mockedValueChangeEvent;

    private PopupSelectBox popupSelectBox;

    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        setupTest();

        // Test verification
        verify(mockedContainerPanel).add(mockedRadioButtonsPanel);
        verify(mockedRadioButtonsPanel).add(mockedRadioButtonLeft);
        verify(mockedRadioButtonsPanel).add(mockedRadioButtonRight);
    }

    @Test(expected = NullPointerException.class)
    public void setLeftRadioButtonText_nullValue_exception() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupSelectBox.setLeftRadioButtonText(null);
    }

    @Test
    public void setLeftRadioButtonText_emptyValue_leftRadioButtonIsDisabled() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupSelectBox.setLeftRadioButtonText("");

        // Test Verification
        verify(mockedRadioButtonLeft, times(2)).setEnabled(false);
        verify(mockedRadioButtonLeft, times(2)).setVisible(false);
    }

    @Test
    public void setLeftRadioButtonText_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        final String text = "Genkør alle poster";

        // Activate Subject Under Test
        popupSelectBox.setLeftRadioButtonText(text);

        // Test Verification
        verify(mockedRadioButtonLeft).setText(text);
        verify(mockedRadioButtonLeft).setEnabled(true);
        verify(mockedRadioButtonLeft).setVisible(true);
        verify(mockedRadioButtonLeft).setValue(true);
    }

    @Test(expected = NullPointerException.class)
    public void setRightRadioButtonText_nullValue_exception() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupSelectBox.setRightRadioButtonText(null);
    }

    @Test
    public void setRightRadioButtonText_emptyValue_rightRadioButtonIsDisabled() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupSelectBox.setRightRadioButtonText("");

        // Test Verification
        verify(mockedRadioButtonRight, times(2)).setEnabled(false);
        verify(mockedRadioButtonRight, times(2)).setVisible(false);
    }

    @Test
    public void setRightRadioButtonText_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        final String text = "Genkør kun fejlede poster";

        // Activate Subject Under Test
        popupSelectBox.setRightRadioButtonText(text);

        // Test Verification
        verify(mockedRadioButtonRight).setText(text);
        verify(mockedRadioButtonRight).setEnabled(true);
        verify(mockedRadioButtonRight).setVisible(true);
        verify(mockedRadioButtonRight).setValue(false);
    }

    @Test
    public void leftRadioButtonChangeEventHandler_valueChange_valuesAreChanged() {
        // Test Preparation
        setupTest();
        when(mockedValueChangeEvent.getValue()).thenReturn(true);

        // Activate Subject Under Test
        popupSelectBox.leftRadioButtonChangeEventHandler(mockedValueChangeEvent);

        // Test Verification
        verify(mockedRadioButtonLeft).setValue(true);
        verify(mockedRadioButtonRight).setValue(false);
        assertThat(popupSelectBox.isRightSelected(), is(false));
    }


    @Test
    public void rightRadioButtonChangeEventHandler_valueChange_valuesAreChanged() {
        // Test Preparation
        setupTest();
        when(mockedValueChangeEvent.getValue()).thenReturn(true);
        // Activate Subject Under Test
        popupSelectBox.rightRadioButtonChangeEventHandler(mockedValueChangeEvent);

        // Test Verification
        verify(mockedRadioButtonRight).setValue(true);
        verify(mockedRadioButtonLeft).setValue(false);
        assertThat(popupSelectBox.isRightSelected(), is(true));
    }

    /**
     * Private methods
     */

    private void setupTest() {
        popupSelectBox = new PopupSelectBox(
                mockedWidget,
                "",
                "",
                "",
                "",
                mockedBasePanel,
                mockedDialogBox,
                mockedContainerPanel,
                mockedButtonPanel,
                mockedRadioButtonsPanel,
                mockedRadioButtonLeft,
                mockedRadioButtonRight);
        popupSelectBox.setRightSelected(false);
    }
}
