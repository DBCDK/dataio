package dk.dbc.dataio.gui.client.components;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * DateTimeBox unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class DateTimeBoxTest {
    @Mock
    KeyPressEvent mockedKeyPressEvent;
    @Mock
    NativeEvent mockedNativeEvent;
    @Mock
    ValueChangeEvent<Date> mockedValueChangeEvent;

    //
    // Tests starts here...
    //
    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        new DateTimeBox();
    }

    @Test
    public void keyPressedInTextBox_metaKeyPressed_ok() {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();
        when(mockedKeyPressEvent.isMetaKeyDown()).thenReturn(true);

        // Activate subject under test
        dateTimeBox.keyPressedInTextBox(mockedKeyPressEvent);

        // Verify test
        verifyNoMoreInteractions(dateTimeBox.textBox);
    }

    @Test
    public void keyPressedInTextBox_controlKeyPressed_ok() {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();
        when(mockedKeyPressEvent.isMetaKeyDown()).thenReturn(false);
        when(mockedKeyPressEvent.isControlKeyDown()).thenReturn(true);

        // Activate subject under test
        dateTimeBox.keyPressedInTextBox(mockedKeyPressEvent);

        // Verify test
        verifyNoMoreInteractions(dateTimeBox.textBox);
    }

    private void testNumericKey(char c) {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();
        when(mockedKeyPressEvent.isMetaKeyDown()).thenReturn(false);
        when(mockedKeyPressEvent.isControlKeyDown()).thenReturn(false);
        when(mockedKeyPressEvent.getCharCode()).thenReturn(c);

        // Activate subject under test
        dateTimeBox.keyPressedInTextBox(mockedKeyPressEvent);

        // Verify test
        verifyNoMoreInteractions(dateTimeBox.textBox);
    }

    @Test
    public void keyPressedInTextBox_NumericKeysPressed_ok() {
        // Test of all legal numeric keys
        testNumericKey('0');
        testNumericKey('1');
        testNumericKey('2');
        testNumericKey('3');
        testNumericKey('4');
        testNumericKey('5');
        testNumericKey('6');
        testNumericKey('7');
        testNumericKey('8');
        testNumericKey('9');
        testNumericKey('-');
        testNumericKey(':');
        testNumericKey(' ');
    }

    private void testAlphaNumericKey(char c) {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();
        when(mockedKeyPressEvent.isMetaKeyDown()).thenReturn(false);
        when(mockedKeyPressEvent.isControlKeyDown()).thenReturn(false);
        when(mockedKeyPressEvent.getCharCode()).thenReturn(c);

        // Activate subject under test
        dateTimeBox.keyPressedInTextBox(mockedKeyPressEvent);

        // Verify test
        verify(dateTimeBox.textBox).cancelKey();
        verifyNoMoreInteractions(dateTimeBox.textBox);
    }

    @Test
    public void keyPressedInTextBox_AlphaNumericKeysPressed_ok() {
        // Test of sample alphanumeric keys
        testAlphaNumericKey('a');
        testAlphaNumericKey('å');
        testAlphaNumericKey('|');
        testAlphaNumericKey('\n');
    }

    private void testLegalActionKey(int c) {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();
        when(mockedKeyPressEvent.isMetaKeyDown()).thenReturn(false);
        when(mockedKeyPressEvent.isControlKeyDown()).thenReturn(false);
        when(mockedKeyPressEvent.getCharCode()).thenReturn((char) 0);
        when(mockedKeyPressEvent.getNativeEvent()).thenReturn(mockedNativeEvent);
        when(mockedNativeEvent.getKeyCode()).thenReturn(c);

        // Activate subject under test
        dateTimeBox.keyPressedInTextBox(mockedKeyPressEvent);

        // Verify test
        verifyNoMoreInteractions(dateTimeBox.textBox);
    }

    @Test
    public void keyPressedInTextBox_LegalActionKeys_ok() {
        // Test of all legal action keys
        testLegalActionKey(KeyCodes.KEY_BACKSPACE);
        testLegalActionKey(KeyCodes.KEY_DELETE);
        testLegalActionKey(KeyCodes.KEY_RIGHT);
        testLegalActionKey(KeyCodes.KEY_LEFT);
        testLegalActionKey(KeyCodes.KEY_TAB);
    }

    private void testIllegalActionKey(int c) {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();
        when(mockedKeyPressEvent.isMetaKeyDown()).thenReturn(false);
        when(mockedKeyPressEvent.isControlKeyDown()).thenReturn(false);
        when(mockedKeyPressEvent.getCharCode()).thenReturn((char) 0);
        when(mockedKeyPressEvent.getNativeEvent()).thenReturn(mockedNativeEvent);
        when(mockedNativeEvent.getKeyCode()).thenReturn(c);

        // Activate subject under test
        dateTimeBox.keyPressedInTextBox(mockedKeyPressEvent);

        // Verify test
        verify(dateTimeBox.textBox).cancelKey();
        verifyNoMoreInteractions(dateTimeBox.textBox);
    }

    @Test
    public void keyPressedInTextBox_IllegalActionKeys_ok() {
        // Test of sample illegal keys
        testIllegalActionKey(KeyCodes.KEY_DOWN);
        testIllegalActionKey(KeyCodes.KEY_F10);
        testIllegalActionKey(KeyCodes.KEY_NUM_FOUR);
        testIllegalActionKey(KeyCodes.KEY_PAGEDOWN);
        testIllegalActionKey(KeyCodes.KEY_ESCAPE);
    }

    @Test
    public void textBoxLostFocus_call_ok() {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();
        when(dateTimeBox.textBox.getValue()).thenReturn("20151012102030");

        // Activate subject under test
        dateTimeBox.textBoxLostFocus(null); // Event is not used, so just use null

        // Verify test
        verify(dateTimeBox.textBox).getValue();
        verify(dateTimeBox.textBox).setValue("2015-10-12 10:20:30", true);
        verifyNoMoreInteractions(dateTimeBox.textBox);
        verify(dateTimeBox.datePicker).setValue(any(Date.class), eq(true));
        verifyNoMoreInteractions(dateTimeBox.datePicker);
        verify(dateTimeBox.datePickerPanel).show();
        verify(dateTimeBox.datePickerPanel).hide();
        verifyNoMoreInteractions(dateTimeBox.datePickerPanel);
    }

    @Test
    public void textBoxGotFocus_call_ok() {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();
        when(dateTimeBox.textBox.getValue()).thenReturn("2015-10-12 10:20:30");

        // Activate subject under test
        dateTimeBox.textBoxGotFocus(null); // Event is not used, so just use null

        // Verify test
        verify(dateTimeBox.textBox).getValue();
        verify(dateTimeBox.textBox).setValue("20151012102030");
        verifyNoMoreInteractions(dateTimeBox.textBox);
        verifyNoMoreInteractions(dateTimeBox.datePicker);
        verify(dateTimeBox.datePickerPanel).show();
        verify(dateTimeBox.datePickerPanel).hide();
        verifyNoMoreInteractions(dateTimeBox.datePickerPanel);
    }

    @Test
    public void calendarIconClicked_call_ok() {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();

        // Activate subject under test
        dateTimeBox.calendarIconClicked(null); // Event is not used, so just use null

        // Verify test
        verify(dateTimeBox.datePickerPanel, times(2)).show();
        verify(dateTimeBox.datePickerPanel).hide();
        verify(dateTimeBox.datePickerPanel).setPopupPosition(0, 0);
        verifyNoMoreInteractions(dateTimeBox.datePickerPanel);
        verifyNoMoreInteractions(dateTimeBox.textBox);
        verifyNoMoreInteractions(dateTimeBox.datePicker);
    }

    @Test
    public void datePickerClicked_call_ok() {
        // Test preparation
        DateTimeBox dateTimeBox = new DateTimeBox();
        when(mockedValueChangeEvent.getValue()).thenReturn(new Date(1220227200L * 1000));

        // Activate subject under test
        dateTimeBox.datePickerClicked(mockedValueChangeEvent);

        // Verify test
        verify(dateTimeBox.datePickerPanel, times(1)).show();
        verify(dateTimeBox.datePickerPanel, times(2)).hide();
        verifyNoMoreInteractions(dateTimeBox.datePickerPanel);
        verify(dateTimeBox.textBox).setValue("2008-09-01 02:00:00", true);
        verifyNoMoreInteractions(dateTimeBox.textBox);
        verifyNoMoreInteractions(dateTimeBox.datePicker);
    }

    @Test
    public void rightJustify_AllCombinations_ok() {
        assertThat(DateTimeBox.rightJustify("1234", ""), is("1234"));
        assertThat(DateTimeBox.rightJustify("1234", "5"), is("1235"));
        assertThat(DateTimeBox.rightJustify("1234", "56"), is("1256"));
        assertThat(DateTimeBox.rightJustify("1234", "567"), is("1567"));
        assertThat(DateTimeBox.rightJustify("1234", "5678"), is("5678"));
        assertThat(DateTimeBox.rightJustify("1234", "56789"), is("5678"));
        assertThat(DateTimeBox.rightJustify("1234", "567891011"), is("5678"));
    }

    @Test
    public void normalizeStringDate_AllCombinations_ok() {
        assertThat(DateTimeBox.normalizeStringDate(""), is(""));
        assertThat(DateTimeBox.normalizeStringDate("1"), is("20010101000000"));
        assertThat(DateTimeBox.normalizeStringDate("12"), is("20120101000000"));
        assertThat(DateTimeBox.normalizeStringDate("123"), is("21230101000000"));
        assertThat(DateTimeBox.normalizeStringDate("1234"), is("12340101000000"));
        assertThat(DateTimeBox.normalizeStringDate("12345"), is("12340101000000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-"), is("20150101000000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-0"), is("20150001000000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-12"), is("20151201000000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-0"), is("20151000000000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31"), is("20151031000000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31 "), is("20151031000000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31 1"), is("20151031010000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31 14"), is("20151031140000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31 14:"), is("20151031140000"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31 14:2"), is("20151031140200"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31 14:23"), is("20151031142300"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31 14:23:"), is("20151031142300"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31 14:23:5"), is("20151031142305"));
        assertThat(DateTimeBox.normalizeStringDate("2015-10-31 14:23:56"), is("20151031142356"));
        assertThat(DateTimeBox.normalizeStringDate("+aAå1Å´4@"), is("20140101000000"));
    }

    @Test
    public void formatStringDate_AllCombinations_ok() {
        assertThat(DateTimeBox.formatStringDate(""), is(""));
        assertThat(DateTimeBox.formatStringDate("2"), is("2002-01-01 00:00:00"));
        assertThat(DateTimeBox.formatStringDate("20"), is("2020-01-01 00:00:00"));
        assertThat(DateTimeBox.formatStringDate("201"), is("2201-01-01 00:00:00"));
        assertThat(DateTimeBox.formatStringDate("2015"), is("2015-01-01 00:00:00"));
        assertThat(DateTimeBox.formatStringDate("20152"), is("2015-02-01 00:00:00"));
        assertThat(DateTimeBox.formatStringDate("201510"), is("2015-10-01 00:00:00"));
        assertThat(DateTimeBox.formatStringDate("2015103"), is("2015-10-03 00:00:00"));
        assertThat(DateTimeBox.formatStringDate("20151031"), is("2015-10-31 00:00:00"));
        assertThat(DateTimeBox.formatStringDate("201510311"), is("2015-10-31 01:00:00"));
        assertThat(DateTimeBox.formatStringDate("2015103114"), is("2015-10-31 14:00:00"));
        assertThat(DateTimeBox.formatStringDate("20151031142"), is("2015-10-31 14:02:00"));
        assertThat(DateTimeBox.formatStringDate("201510311423"), is("2015-10-31 14:23:00"));
        assertThat(DateTimeBox.formatStringDate("2015103114235"), is("2015-10-31 14:23:05"));
        assertThat(DateTimeBox.formatStringDate("20151031142356"), is("2015-10-31 14:23:56"));
        assertThat(DateTimeBox.formatStringDate("201510311423567"), is("2015-10-31 14:23:56"));
        assertThat(DateTimeBox.formatStringDate("+aAå1Å´4@:- "), is("2014-01-01 00:00:00"));
    }

}
