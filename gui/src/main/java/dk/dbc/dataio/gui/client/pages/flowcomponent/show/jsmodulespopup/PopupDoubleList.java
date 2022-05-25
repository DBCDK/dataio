package dk.dbc.dataio.gui.client.pages.flowcomponent.show.jsmodulespopup;

import com.google.gwt.uibinder.client.UiConstructor;
import dk.dbc.dataio.gui.client.components.popup.PopupValueBox;

import java.util.List;


/**
 * Popup box for showing a double list in a popup window
 */
public class PopupDoubleList extends PopupValueBox<DoubleList, PopupDoubleList.DoubleListData> {


    /**
     * Constructor
     *
     * @param dialogTitle  The title text to display on the Dialog Box (mandatory)
     * @param okButtonText The text to be displayed in the OK Button (mandatory)
     */
    @UiConstructor
    public PopupDoubleList(String dialogTitle, String okButtonText) {
        super(new DoubleList(), dialogTitle, okButtonText);
    }

    public PopupDoubleList() {
        this("", "");
    }


    /*
     * Local classes
     */

    public class DoubleListData {
        String headerLeft;
        List<String> bodyLeft;
        String headerRight;
        List<String> bodyRight;

        public DoubleListData(String headerLeft, List<String> bodyLeft, String headerRight, List<String> bodyRight) {
            this.headerLeft = headerLeft;
            this.bodyLeft = bodyLeft;
            this.headerRight = headerRight;
            this.bodyRight = bodyRight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DoubleListData)) return false;
            DoubleListData that = (DoubleListData) o;
            return this.headerLeft.equals(that.headerLeft) &&
                    this.bodyLeft.equals(that.bodyLeft) &&
                    this.headerRight.equals(that.headerRight) &&
                    this.bodyRight.equals(that.bodyRight);
        }

        @Override
        public int hashCode() {
            int result = headerLeft != null ? headerLeft.hashCode() : 0;
            result = 31 * result + (bodyLeft != null ? bodyLeft.hashCode() : 0);
            result = 31 * result + (headerRight != null ? headerRight.hashCode() : 0);
            result = 31 * result + (bodyRight != null ? bodyRight.hashCode() : 0);
            return result;
        }
    }

}
