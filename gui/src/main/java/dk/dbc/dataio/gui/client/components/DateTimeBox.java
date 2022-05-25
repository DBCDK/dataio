package dk.dbc.dataio.gui.client.components;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DatePicker;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.Date;


public class DateTimeBox extends Composite implements HasValue<String> {
    final static char DATE_SEPARATOR = '-';
    final static char TIME_SEPARATOR = ':';
    final static char DATE_TIME_SEPARATOR = ' ';
    final static String SEPARATORS = new String(new char[]{DATE_SEPARATOR, TIME_SEPARATOR, DATE_TIME_SEPARATOR});
    final static String DEFAULT_FORMATTED_EMPTY_YEAR = "2000";
    final static String DEFAULT_FORMATTED_EMPTY_DAY_MONTH = "01";
    final static String DEFAULT_FORMATTED_EMPTY_TIME = "00";


    interface DateTimeBoxUiBinder extends UiBinder<HTMLPanel, DateTimeBox> {
    }

    private static DateTimeBoxUiBinder ourUiBinder = GWT.create(DateTimeBoxUiBinder.class);

    public DateTimeBox() {
        initWidget(ourUiBinder.createAndBindUi(this));
        datePickerPanel.show();  // Is needed, because PopupPanel works unlike others: This call inserts the PopupPanel at the very bottom of the DOM
        datePickerPanel.hide();  // ... and this call removes the PopupPanel again (for some reason, both calls are necessary to assure correct position in DOM)
    }

    // UI Fields
    @UiField
    TextBox textBox;
    @UiField
    PopupPanel datePickerPanel;
    @UiField
    DatePicker datePicker;


    @UiHandler("textBox")
    void keyPressedInTextBox(KeyPressEvent event) {
        // Do only allow numeric characters, therefore do filter non-numeric characters
        if (!event.isMetaKeyDown() && !event.isControlKeyDown()) {
            if (event.getCharCode() != 0) {
                switch (event.getCharCode()) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                    case DATE_SEPARATOR:
                    case TIME_SEPARATOR:
                    case DATE_TIME_SEPARATOR:
                        // These characters are all legal, and shall therefore not be cancelled
                        break;
                    default:
                        // All other characters are not to be used, and shall be cancelled
                        textBox.cancelKey();
                }
            } else if (event.getNativeEvent().getKeyCode() != 0) {
                switch (event.getNativeEvent().getKeyCode()) {
                    case KeyCodes.KEY_BACKSPACE:
                    case KeyCodes.KEY_DELETE:
                    case KeyCodes.KEY_RIGHT:
                    case KeyCodes.KEY_LEFT:
                    case KeyCodes.KEY_TAB:
                        // These characters are all legal, and shall therefore not be cancelled
                        break;
                    case KeyCodes.KEY_ENTER:
                        textBox.setFocus(false);
                        break;
                    default:
                        // All other characters are not to be used, and shall be cancelled
                        textBox.cancelKey();
                }
            }
        }
    }

    @UiHandler("textBox")
    void textBoxGotFocus(FocusEvent event) {
        textBox.setValue(normalizeStringDate(textBox.getValue()));
    }

    @UiHandler("textBox")
    void textBoxLostFocus(BlurEvent event) {
        acceptEnteredData();
    }

    @UiHandler("textBox")
    void textBoxChanged(ChangeEvent event) {
        acceptEnteredData();
    }

    @UiHandler("calendarIcon")
    void calendarIconClicked(ClickEvent event) {
        datePickerPanel.show();
        int x = this.asWidget().getElement().getAbsoluteRight();
        int y = this.asWidget().getElement().getAbsoluteTop();
        datePickerPanel.setPopupPosition(x, y);
    }

    @UiHandler("datePicker")
    void datePickerClicked(ValueChangeEvent<Date> event) {
        datePickerPanel.hide();
        textBox.setValue(Format.formatLongDate(event.getValue()), true);
    }


    // HasValue interface implementation

    @Override
    public String getValue() {
        return formatStringDate(textBox.getValue());
    }

    @Override
    public void setValue(String value) {
        textBox.setValue(value);
    }

    @Override
    public void setValue(String value, boolean fireEvents) {
        textBox.setValue(value, fireEvents);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> valueChangeHandler) {
        return textBox.addValueChangeHandler(valueChangeHandler);
    }


    // Widget Overrides

    @Override
    protected void onLoad() {
        acceptEnteredData();  // The textBoxChanged method does not work during startup - therefore we need to accept data entered during startup here
    }


    // Private methods

    /**
     * Input format: "YYYY-MM-DD HH:MM:SS"
     * Output format: "YYYYMMDDHHMMSS"
     * <p>
     * Missing data is created according to the following rules:
     * Year must be given
     * 1 char Year is prepended with "200"
     * 2 char Year is prepended with "20"
     * 3 char Year is prepended with "2"
     * Default Month when missing is "01"
     * Default Day when missing is "01"
     * Default Hour when missing is "00"
     * Default Minutes when missing is "00"
     * Default Seconds when missing is "00"
     * Single digit Month, Day, Hours, Minutes and Second is prepended by "0"
     *
     * @param input Input date according to formats above
     * @return Output date according to formats above
     */
    static String normalizeStringDate(String input) {
        String[] dateAndTime;
        String[] dates;
        String[] times;
        String date = "";
        String time = "";
        String year = "";
        String month = "";
        String day = "";
        String hour = "";
        String minute = "";
        String second = "";
        String result = "";

        if (input.isEmpty()) {
            return input;
        }

        // Filter out illegal characters
        input = input.replaceAll("[^0-9" + SEPARATORS + "]", "");
        dateAndTime = input.split(String.valueOf(DATE_TIME_SEPARATOR), 2);

        // Normalize date
        if (dateAndTime.length > 0) {
            // There is a date in dateAndTime[0]
            date = dateAndTime[0];
        }
        dates = date.split(String.valueOf(DATE_SEPARATOR), 3);
        if (dates.length > 0) {
            // There is a Year in dates[0]
            year = dates[0];
        }
        switch (year.length()) {
            case 0:
                result += "2000";
                break;
            case 1:
                result += "200" + year;
                break;
            case 2:
                result += "20" + year;
                break;
            case 3:
                result += "2" + year;
                break;
            case 4:
                result += year;
                break;
            default:
                result += year.substring(0, 4);
                break;
        }
        if (dates.length > 1) {
            // There is a Month in dates[1]
            month = dates[1];
        }
        switch (month.length()) {
            case 0:
                result += "01";
                break;
            case 1:
                result += "0" + month;
                break;
            case 2:
                result += month;
                break;
            default:
                result += month.substring(0, 2);
                break;
        }
        if (dates.length > 2) {
            // There is a Day in dates[2]
            day = dates[2];
        }
        switch (day.length()) {
            case 0:
                result += "01";
                break;
            case 1:
                result += "0" + day;
                break;
            case 2:
                result += day;
                break;
            default:
                result += day.substring(0, 2);
                break;
        }

        // Normalize time
        if (dateAndTime.length > 1) {
            // There is a time in dateAndTime[1]
            time = dateAndTime[1];
        }
        times = time.split(String.valueOf(TIME_SEPARATOR), 3);
        if (times.length > 0) {
            // There is an Hour in times[0]
            hour = times[0];
        }
        switch (hour.length()) {
            case 0:
                result += "00";
                break;
            case 1:
                result += "0" + hour;
                break;
            case 2:
                result += hour;
                break;
            default:
                result += hour.substring(0, 2);
                break;
        }
        if (times.length > 1) {
            // There is a Minute in times[1]
            minute = times[1];
        }
        switch (minute.length()) {
            case 0:
                result += "00";
                break;
            case 1:
                result += "0" + minute;
                break;
            case 2:
                result += minute;
                break;
            default:
                result += minute.substring(0, 2);
                break;
        }
        if (times.length > 2) {
            // There is a Second in times[2]
            second = times[2];
        }
        switch (second.length()) {
            case 0:
                result += "00";
                break;
            case 1:
                result += "0" + second;
                break;
            case 2:
                result += second;
                break;
            default:
                result += second.substring(0, 2);
                break;
        }
        return result;
    }

    /**
     * Input format: "YYYMMDDHHMMSS"
     * Output format: "YYYY-MM-DD HH:MM:SS"
     *
     * @param input Input date according to rules above
     * @return Output date according to rules above
     */
    static String formatStringDate(String input) {
        String year;
        String month = DEFAULT_FORMATTED_EMPTY_DAY_MONTH;
        String day = DEFAULT_FORMATTED_EMPTY_DAY_MONTH;
        String hour = DEFAULT_FORMATTED_EMPTY_TIME;
        String minute = DEFAULT_FORMATTED_EMPTY_TIME;
        String second = DEFAULT_FORMATTED_EMPTY_TIME;
        int size;

        if (input.isEmpty()) {
            return input;
        }
        // Filter out illegal characters
        input = input.replaceAll("[^0-9]", "");
        size = input.length();

        // Year
        if (size > 4) {
            year = input.substring(0, 4);
        } else {
            year = rightJustify(DEFAULT_FORMATTED_EMPTY_YEAR, input.substring(0, size));
        }
        // Month
        if (size > 6) {
            month = input.substring(4, 6);
        } else {
            if (size > 4) {
                month = rightJustify(DEFAULT_FORMATTED_EMPTY_DAY_MONTH, input.substring(4, size));
            }
        }
        // Day
        if (size > 8) {
            day = input.substring(6, 8);
        } else {
            if (size > 6) {
                day = rightJustify(DEFAULT_FORMATTED_EMPTY_DAY_MONTH, input.substring(6, size));
            }
        }
        // Hour
        if (size > 10) {
            hour = input.substring(8, 10);
        } else {
            if (size > 8) {
                hour = rightJustify(DEFAULT_FORMATTED_EMPTY_TIME, input.substring(8, size));
            }
        }
        // Minute
        if (size > 12) {
            minute = input.substring(10, 12);
        } else {
            if (size > 10) {
                minute = rightJustify(DEFAULT_FORMATTED_EMPTY_TIME, input.substring(10, size));
            }
        }
        // Second
        if (size > 14) {
            second = input.substring(12, 14);
        } else {
            if (size > 12) {
                second = rightJustify(DEFAULT_FORMATTED_EMPTY_TIME, input.substring(12, size));
            }
        }

        return year + DATE_SEPARATOR + month + DATE_SEPARATOR + day + DATE_TIME_SEPARATOR
                + hour + TIME_SEPARATOR + minute + TIME_SEPARATOR + second;
    }

    /**
     * Right justifies a date in a pre-filled text string (the text overwrites the existing default text)
     *
     * @param defaultString The pre filled text to use as a default string
     * @param text          The text to right justify, and overwrite the default string
     * @return The resulting string
     */
    static String rightJustify(String defaultString, String text) {
        if (text.length() > defaultString.length()) {
            return text.substring(0, defaultString.length());  // Cutoff to size of defaultString
        } else {
            return defaultString.substring(0, defaultString.length() - text.length()) + text;
        }
    }

    /**
     * Accepts the data entered in the textbox field
     */
    private void acceptEnteredData() {
        String formattedData = formatStringDate(textBox.getValue());
        textBox.setValue(formattedData, true);
        if (!formattedData.isEmpty()) {
            datePicker.setValue(Format.parseLongDateAsDate(formattedData), true);
        }
    }

}
