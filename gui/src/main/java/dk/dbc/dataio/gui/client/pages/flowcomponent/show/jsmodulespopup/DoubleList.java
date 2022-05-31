package dk.dbc.dataio.gui.client.pages.flowcomponent.show.jsmodulespopup;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.Arrays;

/**
 * Class for displaying JS Modules for the current SVN Revision, and for Next Revision
 * This display is divided in to columns, with the SVN Revision modules to the left, and the Next Revision modules to the right
 */
public class DoubleList extends Composite implements HasValue<PopupDoubleList.DoubleListData>, Focusable {
    final static String SPLIT_BY_COMMA = "\\s*,\\s*";

    interface JSModulesListUiBinder extends UiBinder<HTMLPanel, DoubleList> {
    }

    private static JSModulesListUiBinder ourUiBinder = GWT.create(JSModulesListUiBinder.class);


    /*
     * UI Fields
     */

    @UiField
    Label headerLeft;
    @UiField
    Label bodyLeft;
    @UiField
    Label headerRight;
    @UiField
    Label bodyRight;


    /**
     * Empty Constructor
     */
    public DoubleList() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }



    /*
     * Implementations of Focusable methods
     * No Focus is needed - therefore the implementation is empty
     */

    /**
     * Get tab index - no implementation is needed
     *
     * @return Zero always
     */
    @Override
    public int getTabIndex() {
        return 0;
    }

    /**
     * Set accesskey - no implementation is needed
     *
     * @param c Access key
     */
    @Override
    public void setAccessKey(char c) {
    }

    /**
     * Set focus - no implementation is needed
     *
     * @param setFocus Boolean to determine if focus should be set
     */
    @Override
    public void setFocus(boolean setFocus) {
    }

    /**
     * Set Tab index - no implementation is needed
     *
     * @param tabIndex The tab index to set - no implementation is needed
     */
    @Override
    public void setTabIndex(int tabIndex) {
    }


    /*
     * Implementations of HasValue methods
     */

    /**
     * Gets the value of the double list
     *
     * @return The value of the double list
     */
    @Override
    public PopupDoubleList.DoubleListData getValue() {
        return new PopupDoubleList().new DoubleListData(
                headerLeft.getText(),
                Arrays.asList(bodyLeft.getText().split(SPLIT_BY_COMMA)),
                headerRight.getText(),
                Arrays.asList(bodyRight.getText().split(SPLIT_BY_COMMA))
        );
    }

    /**
     * Sets the value of the Double List
     *
     * @param doubleListData The value of the Double List
     */
    @Override
    public void setValue(PopupDoubleList.DoubleListData doubleListData) {
        setValue(doubleListData, false);
    }

    /**
     * Sets the value of the Double List
     *
     * @param doubleListData The value of the Double List
     * @param fireEvent      This is not implemented, since widget data is readonly
     */
    @Override
    public void setValue(PopupDoubleList.DoubleListData doubleListData, boolean fireEvent) {
        if (doubleListData != null) {
            headerLeft.setText(doubleListData.headerLeft);
            bodyLeft.setText(Format.commaSeparate(doubleListData.bodyLeft));
            headerRight.setText(doubleListData.headerRight);
            bodyRight.setText(Format.commaSeparate(doubleListData.bodyRight));
        }
        // Here is no handling of value change events, since the widget data is readonly
    }

    /**
     * Adds a value change handler - No implementation, since widget data is readonly
     *
     * @param valueChangeHandler The new changehandler to set
     * @return A Handler registration object - always null, due to empty implementation
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<PopupDoubleList.DoubleListData> valueChangeHandler) {
        return null;
    }

}
