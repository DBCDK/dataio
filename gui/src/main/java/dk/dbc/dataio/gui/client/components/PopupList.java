package dk.dbc.dataio.gui.client.components;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;

/**
 * Popup list for displaying lists of elements vertically in a popup window
 */
public class PopupList extends Composite implements HasWidgets {
    interface PopupListUiBinder extends UiBinder<HTMLPanel, PopupList> {}
    private static PopupListUiBinder ourUiBinder = GWT.create(PopupListUiBinder.class);


    /**
     * Ui Fields
     */
    @UiField DialogBox dialogBox;
    @UiField VerticalPanel content;
    @UiField Button okButton;


    /**
     * Constructor
     */
    @UiConstructor
    public PopupList(String dialogTitle, String okButtonText) {
        initWidget(ourUiBinder.createAndBindUi(this));
        dialogBox.setText(dialogTitle);
        okButton.setText(okButtonText);
        show();  // First show the DialogBox in order to add it to the DOM
        hide();  // ... but we don't want it shown upon startup - so hide it again
    }

    /**
     * Ui Handler Methods
     */

    /**
     * Ok button Click Handler
     * @param clickEvent The click event for the OK Button
     */
    @UiHandler("okButton")
    public void okClickHandler(ClickEvent clickEvent) {
        dialogBox.hide();
    }


    /*
     * Public methods
     */

    /**
     * Shows the popup and attach it to the page. It must have a child widget before this method is called.
     */
    public void show() {
        dialogBox.center();
        dialogBox.show();
    }

    /**
     * Hides the popup and detaches it from the page. This has no effect if it is not currently showing.
     */
    public void hide() {
        dialogBox.hide();
    }

    /**
     * Adds a text to the popup panel
     * @param text Text to be added
     */
    public void add(String text) {
        content.add(new Label(text));
    }


    /*
     * HasWidget overrides
     */

    /**
     * Adds a widget to the popup panel
     * @param widget Widget to be added
     */
    @Override
    public void add(Widget widget) {
        content.add(widget);
    }

    /**
     * Clears all elements from the popup panel
     */
    @Override
    public void clear() {
        content.clear();
    }

    /**
     * Fetches the iterator from the popup panel
     * @return Iterator for the popup panel
     */
    @Override
    public Iterator<Widget> iterator() {
        return content.iterator();
    }



    /**
     * Removes a widget from the popup panel
     * @param widget Widget to be removed from the popup panel
     * @return Boolean: True - removed, False - not removed
     */
    @Override
    public boolean remove(Widget widget) {
        return content.remove(widget);
    }

}