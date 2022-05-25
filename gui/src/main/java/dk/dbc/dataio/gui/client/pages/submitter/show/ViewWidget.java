package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.popup.PopupListBox;
import dk.dbc.dataio.gui.client.components.submitterfilter.SubmitterFilter;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public abstract class ViewWidget extends ContentPanel<Presenter> implements IsWidget {

    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, ViewWidget> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    // UI Fields
    @UiField
    Button createButton;
    @UiField
    CellTable submittersTable;
    @UiField
    PopupListBox popupList;
    @UiField
    SubmitterFilter submitterFilter;

    /**
     * Default constructor
     *
     * @param header Header text
     */
    public ViewWidget(String header) {
        super(header);
        setHeader(commonInjector.getMenuTexts().menu_Submitters());
        add(uiBinder.createAndBindUi(this));
    }

    /**
     * Ui Handler to catch click events on the create button
     *
     * @param event Clicked event
     */
    @UiHandler("createButton")
    void backButtonPressed(ClickEvent event) {
        presenter.createSubmitter();
    }

    @UiHandler("popupList")
    void setPopupListButtonPressed(DialogEvent event) {
        if (event != null && event.getDialogButton() == DialogEvent.DialogButton.EXTRA_BUTTON) {
            // Assure, that all items in listBox are selected - only selected are returned in the call to getValue()
            ListBox listBox = popupList.getContentWidget();
            listBox.setMultipleSelect(true);
            int listBoxItems = listBox.getItemCount();
            for (int index = 0; index < listBoxItems; index++) {
                listBox.setItemSelected(index, true);
            }
            presenter.copyFlowBinderListToClipboard(popupList.getValue());
        }
    }

    @UiHandler("submitterFilter")
    @SuppressWarnings("unused")
    void flowBinderFilterChanged(ChangeEvent event) {
    }

    protected Texts getTexts() {
        return this.viewInjector.getTexts();
    }
}
