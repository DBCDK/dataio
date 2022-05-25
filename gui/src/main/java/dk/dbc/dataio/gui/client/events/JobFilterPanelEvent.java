package dk.dbc.dataio.gui.client.events;

import com.google.gwt.event.dom.client.DomEvent;

public class JobFilterPanelEvent extends DomEvent<JobFilterPanelHandler> {
    private static final Type<JobFilterPanelHandler> TYPE = new DomEvent.Type("job-filter-panel", new JobFilterPanelEvent());

    public enum JobFilterPanelButton {REMOVE_BUTTON, PLUS_BUTTON, MINUS_BUTTON}

    @Override
    public Type<JobFilterPanelHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(JobFilterPanelHandler handler) {
        handler.onJobFilterPanelButtonClick(this);
    }

    public JobFilterPanelButton getJobFilterPanelButton() {
        return null;
    }
}
