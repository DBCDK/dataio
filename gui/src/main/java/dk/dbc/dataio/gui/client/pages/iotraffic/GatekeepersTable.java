package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.commons.types.GatekeeperDestination;

import java.util.Comparator;
import java.util.List;

/**
 * Gatekeepers Table for the IoTraffic View
 */
public class GatekeepersTable extends CellTable {
    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    View view;
    Presenter presenter = null;
    ListDataProvider<GatekeeperDestination> dataProvider;

    /**
     * Constructor
     *
     * @param view The owner view for this Gatekeeper Table
     */
    public GatekeepersTable(View view) {
        this.view = view;
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        Column submitterColumn = constructSubmitterColumn();
        addColumn(submitterColumn, texts.label_Submitter());
        addColumn(constructPackagingColumn(), texts.label_Packaging());
        addColumn(constructFormatColumn(), texts.label_Format());
        addColumn(constructDestinationColumn(), texts.label_Destination());
        addColumn(constructActivityColumn(), texts.label_Action());

        addColumnSortHandler(constructSubmitterSortHandler(submitterColumn));
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
     * @param gatekeepers The list of gatekeepers to put into the view
     */
    public void setGatekeepers(List<GatekeeperDestination> gatekeepers) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(gatekeepers);
    }

    /*
     * Local methods
     */


    private Column constructSubmitterColumn() {
        return new TextColumn<GatekeeperDestination>() {
            @Override
            public String getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.getSubmitterNumber();
            }
        };
    }

    private Column constructPackagingColumn() {
        return new TextColumn<GatekeeperDestination>() {
            @Override
            public String getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.getPackaging();
            }
        };
    }

    private Column constructFormatColumn() {
        return new TextColumn<GatekeeperDestination>() {
            @Override
            public String getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.getFormat();
            }
        };
    }

    private Column constructDestinationColumn() {
        return new TextColumn<GatekeeperDestination>() {
            @Override
            public String getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.getDestination();
            }
        };
    }

    private Column constructActivityColumn() {
        Column column = new Column<GatekeeperDestination, String>(new ButtonCell()) {
            @Override
            public String getValue(GatekeeperDestination object) {
                return texts.button_Delete();
            }
        };
        column.setFieldUpdater((index, gatekeeper, buttonText) -> {
            view.gateKeeperDestinationToBeDeleted = ((GatekeeperDestination) gatekeeper).getId();
            view.confirmation.show();
        });
        return column;
    }

    ColumnSortEvent.ListHandler constructSubmitterSortHandler(Column column) {
        ColumnSortEvent.ListHandler<GatekeeperDestination> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        columnSortHandler.setComparator(column, Comparator.comparing((p) -> Long.valueOf(p.getSubmitterNumber())));
        column.setSortable(true);
        getColumnSortList().push(column);  // Default sorting is chosen here
        return columnSortHandler;
    }

    ColumnSortEvent.ListHandler constructCopySortHandler(Column column) {
        ColumnSortEvent.ListHandler<GatekeeperDestination> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        columnSortHandler.setComparator(column, Comparator.comparing((gatekeeperDestination) -> true));
        column.setSortable(true);
        return columnSortHandler;
    }

}
