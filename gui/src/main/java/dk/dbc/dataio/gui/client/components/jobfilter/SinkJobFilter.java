package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.List;

/**
 * This is the Sink Job Filter
 */
public class SinkJobFilter extends BaseJobFilter {

    interface SinkJobFilterUiBinder extends UiBinder<HTMLPanel, SinkJobFilter> {
    }

    private static SinkJobFilterUiBinder ourUiBinder = GWT.create(SinkJobFilterUiBinder.class);

    private String filterParameter = "";  // This variable is used while the list of available sinks is being built up - whenever it has been fetched in the callback class, it is not used anymore...
    private boolean invertFilter = false;  // As with filterParameter - see the comment above
    FlowStoreProxyAsync flowStoreProxy;


    @SuppressWarnings("unused")
    @UiConstructor
    public SinkJobFilter() {
        this("", false);
    }

    SinkJobFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, GWT.create(FlowStoreProxy.class), invertFilter);
    }


    SinkJobFilter(Texts texts, Resources resources, String parameter, FlowStoreProxyAsync flowStoreProxy, boolean invertFilter) {
        super(texts, resources, invertFilter);
        this.flowStoreProxy = flowStoreProxy;
        initWidget(ourUiBinder.createAndBindUi(this));
        flowStoreProxy.findAllSinks(new FetchSinksCallback());
        this.filterParameter = parameter;
        this.invertFilter = invertFilter;
    }

    @UiField
    PromptedList sinkList;


    /**
     * Event handler for handling changes in the sink selection
     *
     * @param event The ValueChangeEvent
     */
    @UiHandler("sinkList")
    @SuppressWarnings("unused")
    void sinkSelectionChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }


    /**
     * Fetches the name of this filter
     *
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.sinkFilter_name();
    }


    /**
     * Gets the value of this job filter, which is the JobListCriteria to be used in the filter search
     *
     * @return The JobListCriteria constructed by this job filter
     */
    @Override
    public JobListCriteria getValue() {
        String selectedKey = getParameter();
        if (selectedKey == null || selectedKey.isEmpty() || selectedKey.equals("0")) return new JobListCriteria();

        return new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, selectedKey));
    }

    /**
     * Sets the selection according to the key value, setup in the parameter attribute<br>
     * The value is given in url as a plain integer, as an index to the sink
     *
     * @param parameter The filter parameter to be used by this job filter
     */
    @Override
    public void localSetParameter(String parameter) {
        if (filterParameter != null) {  // List of actual Sinks has not yet been found
            filterParameter = parameter;  // Replace current temporary sink value parameter
        }
        sinkList.setSelectedValue(parameter);
    }

    /**
     * Gets the parameter value for the filter
     *
     * @return The stored filter parameter for the specific job filter
     */
    @Override
    public String getParameter() {
        return filterParameter != null ? filterParameter : sinkList.getSelectedKey();
    }

    /*
     * Override HasChangeHandlers Interface Methods
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return sinkList.addChangeHandler(changeHandler);
    }

    /*
     * Private
     */

    /**
     * This class is the callback class for the findAllSinks method in the Flow Store
     */
    class FetchSinksCallback extends FilteredAsyncCallback<List<SinkModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
        }

        @Override
        public void onSuccess(List<SinkModel> models) {
            String NO_SINK_ID_SELECTED = "0";
            sinkList.addAvailableItem(texts.sinkFilter_ChooseASinkName(), NO_SINK_ID_SELECTED);
            models.forEach(model -> sinkList.addAvailableItem(model.getSinkName(), String.valueOf(model.getId())));
            sinkList.setEnabled(true);
            setParameter(invertFilter, filterParameter);
            filterParameter = null;
        }
    }

}
