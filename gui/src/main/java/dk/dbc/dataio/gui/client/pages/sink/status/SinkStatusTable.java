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

package dk.dbc.dataio.gui.client.pages.sink.status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.List;


/**
 * Sink Status Table for the Sink Status View
 */
public class SinkStatusTable extends CellTable {

    static class SinkStatusModel {
        String type;
        String name;
        long outstandingJobs;
        long outstandingItemsChunks;
        long latestMovement;

        public SinkStatusModel(String type, String name, long outstandingJobs, long outstandingItemsChunks, long latestMovement) {
            this.type = type;
            this.name = name;
            this.outstandingJobs = outstandingJobs;
            this.outstandingItemsChunks = outstandingItemsChunks;
            this.latestMovement = latestMovement;
        }

        public String getType() {return type;}
        public String getName() {return name;}
        public long getOutstandingJobs() {return outstandingJobs;}
        public long getOutstandingItemsChunks() {return outstandingItemsChunks;}
        public long getLatestMovement() {return latestMovement;}
    }

    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    Presenter presenter;
    ListDataProvider<SinkStatusModel> dataProvider;
    SingleSelectionModel<SinkStatusModel> selectionModel = new SingleSelectionModel<>();

    /**
     * Constructor
     */
    public SinkStatusTable() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        addColumn(constructTypeColumn(), texts.columnHeader_Type());
        addColumn(constructNameColumn(), texts.columnHeader_Name());
        addColumn(constructOutstandingJobsColumn(), texts.columnHeader_OutstandingJobs());
        addColumn(constructOutstandingItemsChunksColumn(), texts.columnHeader_OutstandingItemsChunks());
        addColumn(constructLatestMovementColumn(), texts.columnHeader_LatestMovement());

        setSelectionModel(selectionModel);
    }


    /**
     * This method sets the sink status data for the table
     * @param presenter The presenter
     * @param sinkStatusModels The sink status data
     */
    public void setSinkStatusData(Presenter presenter, List<SinkStatusModel> sinkStatusModels) {
        List<SinkStatusModel> sinkStatus = dataProvider.getList();
        this.presenter = presenter;
        sinkStatus.clear();
        if (sinkStatusModels != null && !sinkStatusModels.isEmpty()) {
            for (SinkStatusModel model: sinkStatusModels ) {
                sinkStatus.add(model);
            }
        }
    }


    /*
     * Local methods
     * /

    /**
     * This method constructs the JobId column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed JobId column
     */
    private Column constructTypeColumn() {
        return new TextColumn<SinkStatusModel>() {
            @Override
            public String getValue(SinkStatusModel model) {
                return model.getType();
            }
        };
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    private Column constructNameColumn() {
        return new TextColumn<SinkStatusModel>() {
            @Override
            public String getValue(SinkStatusModel model) {
                return model.getName();
            }
        };
    }

    /**
     * This method constructs the OutstandingJobs column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed OutstandingJobs column
     */
    private Column constructOutstandingJobsColumn() {
        return new TextColumn<SinkStatusModel>() {
            @Override
            public String getValue(SinkStatusModel model) {
                return String.valueOf(model.getOutstandingJobs());
            }
        };
    }

    /**
     * This method constructs the OutstandingItemsChunks column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed OutstandingItemsChunks column
     */
    private Column constructOutstandingItemsChunksColumn() {
        return new TextColumn<SinkStatusModel>() {
            @Override
            public String getValue(SinkStatusModel model) {
                return String.valueOf(model.getOutstandingItemsChunks());
            }
        };
    }

    /**
     * This method constructs the LatestMovement column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed LatestMovement column
     */
    private Column constructLatestMovementColumn() {
        return new TextColumn<SinkStatusModel>() {
            @Override
            public String getValue(SinkStatusModel model) {
                return Format.formatLongDate(model.getLatestMovement());
            }
        };
    }

}
