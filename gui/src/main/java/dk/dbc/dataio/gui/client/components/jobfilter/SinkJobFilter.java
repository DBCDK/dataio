package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Inject;
import dk.dbc.dataio.gui.client.components.PromptedList;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.List;

/**
 * This is the Sink Job Filter
 */
public class SinkJobFilter extends BaseJobFilter {
    interface SinkJobFilterUiBinder extends UiBinder<HTMLPanel, SinkJobFilter> {
    }

    private static SinkJobFilterUiBinder ourUiBinder = GWT.create(SinkJobFilterUiBinder.class);

    FlowStoreProxyAsync flowStoreProxy;
    ValueChangeHandler<JobListCriteriaModel> sinkJobValueChangeHandler = null;
    HandlerRegistration sinkListHandlerRegistration = null;


    @UiConstructor
    public SinkJobFilter() {
        this((Texts) GWT.create(Texts.class), (Resources) GWT.create(Resources.class), (FlowStoreProxyAsync) GWT.create(FlowStoreProxy.class));
    }

    @Inject
    public SinkJobFilter(Texts texts, Resources resources, FlowStoreProxyAsync flowStoreProxy) {
        super(texts, resources);
        this.flowStoreProxy = flowStoreProxy;
        initWidget(ourUiBinder.createAndBindUi(this));
        flowStoreProxy.findAllSinks(new FetchSinksCallback());
    }

    @UiField PromptedList sinkList;

    /**
     * Event handler for handling changes in the selected sink
     * @param event The ValueChangeEvent
     */
    @UiHandler("sinkList")
    void filterSelectionChanged(ValueChangeEvent<String> event) {
        jobListCriteriaModel.setSinkId(sinkList.getSelectedKey());
    }

    /**
     * Fetches the name of this filter
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.sinkFilter_name();
    }


    /*
     * Override HasValueChangeHandlers Interface Methods
     */

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<JobListCriteriaModel> valueChangeHandler) {
        sinkJobValueChangeHandler = valueChangeHandler;
        sinkListHandlerRegistration = sinkList.addValueChangeHandler(new SinkJobFilterValueChangeHandler());
        return sinkListHandlerRegistration;
    }

    /*
     * Override HasChangeHandlers Interface Methods
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return sinkList.addChangeHandler(changeHandler);
    }


    /*
     * Private classes
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
            for (SinkModel model: models) {
                sinkList.addAvailableItem(model.getSinkName(), String.valueOf(model.getId()));
            }
            sinkList.setEnabled(true);
        }
    }

    /*
     * This class is the ValueChangeHandler for the SinkJobFilter
     */
    class SinkJobFilterValueChangeHandler implements ValueChangeHandler<String> {
        @Override
        public void onValueChange(ValueChangeEvent<String> valueChangeEvent) {
            if (sinkJobValueChangeHandler != null) {
                JobListCriteriaModel model = new JobListCriteriaModel();
                model.setSinkId(valueChangeEvent.getValue());
                sinkJobValueChangeHandler.onValueChange(new SinkJobFilterValueChangeEvent(model));
            }
        }
    }

    /*
     * This class is the ValueChangeEvent for the SinkJobFilter
     */
    class SinkJobFilterValueChangeEvent extends ValueChangeEvent<JobListCriteriaModel> {
        protected SinkJobFilterValueChangeEvent(JobListCriteriaModel value) {
            super(value);
        }
    }
}