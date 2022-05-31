package dk.dbc.dataio.gui.client.pages.sink.status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.Date;
import java.util.List;


/**
 * Sink Status Table for the Sink Status View
 */
public class SinkStatusTable extends CellTable {

    public static class SinkStatusModel implements IsSerializable {
        long sinkId;
        String sinkType;
        String name;
        int outstandingJobs;
        int outstandingChunks;
        Date latestMovement;

        public SinkStatusModel(long sinkId, String sinkType, String name, int outstandingJobs, int outstandingChunks, Date latestMovement) {
            this.sinkId = sinkId;
            this.sinkType = sinkType;
            this.name = name;
            this.outstandingJobs = outstandingJobs;
            this.outstandingChunks = outstandingChunks;
            this.latestMovement = latestMovement == null ? null : new Date(latestMovement.getTime());
        }

        public SinkStatusModel() {
        }

        public long getSinkId() {
            return sinkId;
        }

        public SinkStatusModel withSinkId(long sinkId) {
            this.sinkId = sinkId;
            return this;
        }

        public String getSinkType() {
            return sinkType;
        }

        public SinkStatusModel withSinkType(String type) {
            this.sinkType = type;
            return this;
        }

        public String getName() {
            return name;
        }

        public SinkStatusModel withName(String name) {
            this.name = name;
            return this;
        }

        public int getOutstandingJobs() {
            return outstandingJobs;
        }

        public SinkStatusModel withOutstandingJobs(int outstandingJobs) {
            this.outstandingJobs = outstandingJobs;
            return this;
        }

        public int getOutstandingChunks() {
            return outstandingChunks;
        }

        public SinkStatusModel withOutstandingChunks(int outstandingChunks) {
            this.outstandingChunks = outstandingChunks;
            return this;
        }

        public Date getLatestMovement() {
            return latestMovement;
        }

        public SinkStatusModel withLatestMovement(Date latestMovement) {
            this.latestMovement = latestMovement == null ? null : new Date(latestMovement.getTime());
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SinkStatusModel)) return false;

            SinkStatusModel that = (SinkStatusModel) o;

            if (sinkId != that.sinkId) return false;
            if (outstandingJobs != that.outstandingJobs) return false;
            if (outstandingChunks != that.outstandingChunks) return false;
            if (sinkType != null ? !sinkType.equals(that.sinkType) : that.sinkType != null) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            return latestMovement != null ? latestMovement.equals(that.latestMovement) : that.latestMovement == null;
        }

        @Override
        public int hashCode() {
            int result = (int) (sinkId ^ (sinkId >>> 32));
            result = 31 * result + (sinkType != null ? sinkType.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + outstandingJobs;
            result = 31 * result + outstandingChunks;
            result = 31 * result + (latestMovement != null ? latestMovement.hashCode() : 0);
            return result;
        }
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
        addDomHandler(doubleClickEvent -> presenter.showJobsFilteredBySink(selectionModel.getSelectedObject().getSinkId()), DoubleClickEvent.getType());
    }


    /**
     * This method sets the sink status data for the table
     *
     * @param presenter        The presenter
     * @param sinkStatusModels The sink status data
     */
    public void setSinkStatusData(Presenter presenter, List<SinkStatusModel> sinkStatusModels) {
        List<SinkStatusModel> sinkStatus = dataProvider.getList();
        this.presenter = presenter;
        sinkStatus.clear();
        if (sinkStatusModels != null && !sinkStatusModels.isEmpty()) {
            for (SinkStatusModel model : sinkStatusModels) {
                sinkStatus.add(model);
            }
        }
    }


    /*
     * Local methods
     */

    /**
     * This method constructs the JobId column
     *
     * @return the constructed JobId column
     */
    private Column constructTypeColumn() {
        return new TextColumn<SinkStatusModel>() {
            @Override
            public String getValue(SinkStatusModel model) {
                return model.getSinkType();
            }
        };
    }

    /**
     * This method constructs the Name column
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
     *
     * @return the constructed OutstandingItemsChunks column
     */
    private Column constructOutstandingItemsChunksColumn() {
        return new TextColumn<SinkStatusModel>() {
            @Override
            public String getValue(SinkStatusModel model) {
                return String.valueOf(model.getOutstandingChunks());
            }
        };
    }

    /**
     * This method constructs the LatestMovement column
     *
     * @return the constructed LatestMovement column
     */
    private Column constructLatestMovementColumn() {
        return new TextColumn<SinkStatusModel>() {
            @Override
            public String getValue(SinkStatusModel model) {
                // FIXME: 25/11/16 Temporary solution until an actual date is provided. Should be replaced with: Format.formatLongDate(model.getLatestMovement())
                return "NA";
            }
        };
    }

}
