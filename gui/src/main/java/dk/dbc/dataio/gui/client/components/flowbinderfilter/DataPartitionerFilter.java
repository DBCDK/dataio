package dk.dbc.dataio.gui.client.components.flowbinderfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.gui.client.querylanguage.GwtStringClause;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Collections;
import java.util.List;

public class DataPartitionerFilter extends BaseFlowBinderFilter {

    interface DataPartitionerFilterUiBinder extends UiBinder<HTMLPanel, DataPartitionerFilter> {
    }

    private static DataPartitionerFilterUiBinder ourUiBinder = GWT.create(DataPartitionerFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public DataPartitionerFilter() {
        this("", false);
    }

    DataPartitionerFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    DataPartitionerFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        RecordSplitterConstants.getRecordSplitters()
                .forEach(value -> dataPartitionerList.addAvailableItem(value.name()));
        dataPartitionerList.setEnabled(true);
        setParameter(parameter);
    }

    @UiField
    PromptedList dataPartitionerList;

    /**
     * Event handler for handling changes in the data partitioner selection
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("dataPartitionerList")
    @SuppressWarnings("unused")
    void dataPartitionerSelectionChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    @Override
    public String getName() {
        return texts.dataPartitionerFilter_name();
    }

    @Override
    public List<GwtQueryClause> getValue() {
        final String value = getParameter();
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        final GwtStringClause recordSplitterClause = new GwtStringClause()
                .withIdentifier("flow_binders:content.recordSplitter")
                .withOperator(GwtStringClause.BiOperator.JSON_LEFT_CONTAINS)
                .withValue(value)
                .withNegated(isInvertFilter());

        return Collections.singletonList(recordSplitterClause);
    }

    @Override
    public void localSetParameter(String parameter) {
        dataPartitionerList.setSelectedValue(parameter);
    }

    @Override
    public String getParameter() {
        return dataPartitionerList.getSelectedKey();
    }

    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return dataPartitionerList.addChangeHandler(changeHandler);
    }
}
