package dk.dbc.dataio.gui.client.pages.gatekeeper.ftp.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.model.FtpFileModel;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.List;

public class View extends ContentPanel<Presenter> implements IsWidget {
    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, View> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    // UI Fields
    @UiField
    Button refreshButton;

    @UiField(provided = true)
    FtpShowTable ftpShowTable;

    /**
     * Default empty constructor
     */
    public View() {
        this("");
    }

    /**
     * Default constructor
     *
     * @param header header
     */
    public View(String header) {
        super(header);
        ftpShowTable = new FtpShowTable();
        add(uiBinder.createAndBindUi(this));
    }


    /*
     * Ui Handlers
     */
    @UiHandler("refreshButton")
    void getFtpOverview(ClickEvent event) {
        presenter.getFtpOverview();
    }

    /*
     * Public methods
     */
    public void setFtpShowTable(List<FtpFileModel> ftpFiles) {
        ftpShowTable.setFtpFilesData(presenter, ftpFiles);
    }

}
