package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.gui.client.components.EnterButton;
import dk.dbc.dataio.gui.client.components.popup.PopupBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.List;


public class View extends ContentPanel<Presenter> implements IsWidget {
    interface UiTrafficBinder extends UiBinder<HTMLPanel, View> {
    }

    private static UiTrafficBinder uiBinder = GWT.create(UiTrafficBinder.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }

    @UiField
    PromptedTextBox submitter;
    @UiField
    PromptedTextBox packaging;
    @UiField
    PromptedTextBox format;
    @UiField
    PromptedTextBox destination;
    @UiField
    EnterButton addButton;
    @UiField(provided = true)
    GatekeepersTable gatekeepersTable;
    @UiField
    PopupBox<Label> confirmation;
    long gateKeeperDestinationToBeDeleted = 0;


    public View() {
        super("");
        gatekeepersTable = new GatekeepersTable(this);
        add(uiBinder.createAndBindUi(this));
    }

    @Override
    public void init() {
        gatekeepersTable.setPresenter(presenter);
    }

    @UiHandler("submitter")
    void submitterChanged(ValueChangeEvent<String> event) {
        presenter.submitterChanged(submitter.getText());
    }

    @UiHandler("packaging")
    void packagingChanged(ValueChangeEvent<String> event) {
        presenter.packagingChanged(packaging.getText());
    }

    @UiHandler("format")
    void formatChanged(ValueChangeEvent<String> event) {
        presenter.formatChanged(format.getText());
    }

    @UiHandler("destination")
    void destinationChanged(ValueChangeEvent<String> event) {
        presenter.destinationChanged(destination.getText());
    }

    @UiHandler("addButton")
    void addButtonPressed(ClickEvent event) {
        presenter.addButtonPressed();
    }

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON && gateKeeperDestinationToBeDeleted != 0) {
            presenter.deleteButtonPressed(gateKeeperDestinationToBeDeleted);
            gateKeeperDestinationToBeDeleted = 0;
        }
    }

    /**
     * Displays a warning to the user
     *
     * @param warning The warning to display
     */
    public void displayWarning(String warning) {
        Window.alert(warning);
    }

    /**
     * This method is used to put data into the view
     *
     * @param gatekeepers The list of gatekeepers to put into the view
     */
    public void setGatekeepers(List<GatekeeperDestination> gatekeepers) {
        gatekeepersTable.setGatekeepers(gatekeepers);
    }

}

