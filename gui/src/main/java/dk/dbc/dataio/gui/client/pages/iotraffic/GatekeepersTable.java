/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Event;
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
        Column copyColumn = constructCopyColumn();
        addColumn(copyColumn, texts.label_Copy());
        addColumn(constructNotifyColumn(), texts.label_Notify());
        addColumn(constructActivityColumn(), texts.label_Action());

        addColumnSortHandler(constructSubmitterSortHandler(submitterColumn));
        addColumnSortHandler(constructCopySortHandler(copyColumn));
    }


    /**
     * Sets the presenter to allow communication back to the presenter
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

    private Column constructCopyColumn() {
        CheckboxCell checkboxCell = new CheckboxCell(true, false);
        return new Column<GatekeeperDestination, Boolean>(checkboxCell) {
            @Override
            public Boolean getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.isCopyToPosthus();
            }
            @Override
            public void onBrowserEvent(Cell.Context context, Element elem, GatekeeperDestination gatekeeper, NativeEvent event) {
                if (Event.as(event).getTypeInt() == Event.ONCHANGE) {
                    presenter.updateGatekeeperDestination(gatekeeper.withCopyToPosthus(((InputElement) elem.getFirstChild()).isChecked()));
                }
                super.onBrowserEvent(context, elem, gatekeeper, event);
            }
            @Override
            public String getCellStyleNames(Cell.Context context, GatekeeperDestination model) {
                return "center";
            }
        };
    }

    private Column constructNotifyColumn() {
        CheckboxCell checkboxCell = new CheckboxCell(true, false);
        return new Column<GatekeeperDestination, Boolean>(checkboxCell) {
            @Override
            public Boolean getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.isNotifyFromPosthus();
            }
            @Override
            public void onBrowserEvent(Cell.Context context, Element elem, GatekeeperDestination gatekeeper, NativeEvent event) {
                if (Event.as(event).getTypeInt() == Event.ONCHANGE) {
                    presenter.updateGatekeeperDestination(gatekeeper.withNotifyFromPosthus(((InputElement) elem.getFirstChild()).isChecked()));
                }
                super.onBrowserEvent(context, elem, gatekeeper, event);
            }
            @Override
            public String getCellStyleNames(Cell.Context context, GatekeeperDestination model) {
                return "center";
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
        columnSortHandler.setComparator(column, Comparator.comparing((gatekeeperDestination) -> !gatekeeperDestination.isCopyToPosthus()));
        column.setSortable(true);
        return columnSortHandler;
    }

}
