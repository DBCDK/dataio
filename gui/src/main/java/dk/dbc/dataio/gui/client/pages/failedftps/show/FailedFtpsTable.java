package dk.dbc.dataio.gui.client.pages.failedftps.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.Notification;

import java.util.List;

/**
 * Gatekeepers Table for the Failed Ftps View
 */
public class FailedFtpsTable extends CellTable {
    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    View view;
    Presenter presenter = null;
    ListDataProvider<Notification> dataProvider;

    /**
     * Constructor
     *
     * @param view The owner view for this Gatekeeper Table
     */
    public FailedFtpsTable(View view) {
        this.view = view;
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        addColumn(constructDateColumn(), texts.label_HeaderDate());
        addColumn(constructTransFileColumn(), texts.label_HeaderTransFile());
        addColumn(constructMailColumn(), texts.label_HeaderMail());

        setSelectionModel(new SingleSelectionModel<Notification>());
        addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }


    /**
     * Sets the presenter to allow communication back to the presenter
     *
     * @param presenter The presenter to set
     */
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }


    /**
     * Puts data into the view
     *
     * @param notifications The list of Failed FTP Notifications to put into the view
     */
    public void setNotifications(List<Notification> notifications) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(notifications);
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     *
     * @return the double click handler
     */
    DoubleClickHandler getDoubleClickHandler() {
        return doubleClickEvent -> {
            showTransFileContent(((SingleSelectionModel<Notification>) getSelectionModel()).getSelectedObject());
        };
    }

    /**
     * This method activates the show transfile content page
     *
     * @param notification The notification to show
     */
    private void showTransFileContent(Notification notification) {
        if (presenter != null && notification != null) {
            presenter.showTransFileContent(notification);
        }
    }


    /*
     * Local methods
     */

    private Column constructDateColumn() {
        return new TextColumn<Notification>() {
            @Override
            public String getValue(Notification notification) {
                return Format.formatLongDate(notification.getTimeOfCreation());
            }
        };
    }

    private Column constructTransFileColumn() {
        return new TextColumn<Notification>() {
            @Override
            public String getValue(Notification notification) {
                InvalidTransfileNotificationContext context = (InvalidTransfileNotificationContext) notification.getContext();
                return context.getTransfileName();
            }
        };
    }

    private Column constructMailColumn() {
        return new TextColumn<Notification>() {
            @Override
            public String getValue(Notification notification) {
                return Format.capitalize(notification.getStatus().toString());
            }
        };
    }

}
