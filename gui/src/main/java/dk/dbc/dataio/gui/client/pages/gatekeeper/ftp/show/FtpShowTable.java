package dk.dbc.dataio.gui.client.pages.gatekeeper.ftp.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.model.FtpFileModel;

import java.util.List;
import java.util.logging.Logger;

public class FtpShowTable extends CellTable {
    public static Logger logger = Logger.getLogger(FtpShowTable.class.getName());
    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    Presenter presenter;
    ListDataProvider<FtpFileModel> dataProvider;
    public FtpShowTable() {
        logger.info("Creating FtpShowTable");
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);
        addColumn(constructSizeColumn(), texts.columnHeader_Size());
        addColumn(constructDateColumn(), texts.columnHeader_Date());
        addColumn(constructNameColumn(),  texts.columnHeader_Name());
    }
    private Column constructNameColumn() {
        return new TextColumn<FtpFileModel>() {
            @Override
            public String  getValue(FtpFileModel ftpFileModel) {
                return ftpFileModel.getName();
            }
        };
    }
    private Column constructDateColumn() {
        return new TextColumn<FtpFileModel>() {
            @Override
            public String  getValue(FtpFileModel ftpFileModel) {
                return ftpFileModel.getFileDate();
            }

        };
    }
    private Column constructSizeColumn() {
        TextColumn t = new TextColumn<FtpFileModel>() {
            @Override
            public String  getValue(FtpFileModel ftpFileModel) {
                return ftpFileModel.getFileSize();
            }
        };
        t.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        return t;
    }

    public void setFtpFilesData(Presenter presenter, List<FtpFileModel> ftpFileModels) {
        this.presenter = presenter;
        List<FtpFileModel> ftpFiles = dataProvider.getList();
        ftpFiles.clear();
        if (ftpFileModels != null && !ftpFileModels.isEmpty()) {
            for (FtpFileModel ftpFileModel : ftpFileModels) {
                ftpFiles.add(ftpFileModel);
            }
        }
    }
}
