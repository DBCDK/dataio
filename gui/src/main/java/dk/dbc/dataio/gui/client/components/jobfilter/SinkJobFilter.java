package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.components.PromptedList;

/**
 * This is the Sink Job Filter
 */
public class SinkJobFilter extends BaseJobFilter {
    interface SinkJobFilterUiBinder extends UiBinder<HTMLPanel, SinkJobFilter> {
    }

    private static SinkJobFilterUiBinder ourUiBinder = GWT.create(SinkJobFilterUiBinder.class);

    public SinkJobFilter() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiField PromptedList sinkList;

    /**
     * Fetches the name of this filter
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.sinkFilter_name();
    }

}