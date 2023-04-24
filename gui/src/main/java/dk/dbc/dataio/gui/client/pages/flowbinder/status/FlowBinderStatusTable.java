package dk.dbc.dataio.gui.client.pages.flowbinder.status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.FlowBinderUsage;

import java.util.List;
import java.util.logging.Logger;

public class FlowBinderStatusTable extends CellTable {
    public static Logger logger = Logger.getLogger(FlowBinderUsage.class.getName());
    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    Presenter presenter;
    ListDataProvider<FlowBinderUsage> dataProvider;
    SingleSelectionModel<FlowBinderUsage> selectionModel = new SingleSelectionModel<>();

    public FlowBinderStatusTable() {
        logger.info("Creatng FlowBinderStatusTable");
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);
        addColumn(constructNameColumn(), texts.columnHeader_Name());
        addColumn(constructLastUsedColumn(), texts.columnHeader_LastUsed());
        setSelectionModel(selectionModel);
        addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());

    }

    private DoubleClickHandler getDoubleClickHandler() {
        return doubleClickEvent -> {
            FlowBinderUsage selected = selectionModel.getSelectedObject();
            if (selected != null) {
                presenter.showFlowBinder(selected.getFlowBinderId());
            }
        };
    }

    private Column constructNameColumn() {
        return new TextColumn<FlowBinderUsage>() {
            @Override
            public String getValue(FlowBinderUsage flowBinderUsage) {
                return flowBinderUsage.getName();
            }
        };

    }

    private Column constructLastUsedColumn() {
        return new TextColumn<FlowBinderUsage>() {
            @Override
            public String getValue(FlowBinderUsage flowBinderUsage) {
                return flowBinderUsage.getLastUsed();
            }
        };

    }

    public void setFlowBinderStatusData(Presenter presenter, List<FlowBinderUsage> flowBinderUsageModelList) {
        this.presenter = presenter;
        List<FlowBinderUsage> flowBinderUsages = dataProvider.getList();
        flowBinderUsages.clear();
        if (flowBinderUsageModelList != null && !flowBinderUsageModelList.isEmpty()) {
            for (FlowBinderUsage flowBinderUsageModel : flowBinderUsageModelList) {
                flowBinderUsages.add(flowBinderUsageModel);
            }
        }
    }
}
