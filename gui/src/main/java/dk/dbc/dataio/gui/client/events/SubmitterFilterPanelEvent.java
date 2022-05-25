package dk.dbc.dataio.gui.client.events;

import com.google.gwt.event.dom.client.DomEvent;

public class SubmitterFilterPanelEvent extends DomEvent<SubmitterFilterPanelHandler> {
    private static final Type<SubmitterFilterPanelHandler> TYPE =
            new Type("submitter-filter-panel", new SubmitterFilterPanelEvent());

    public enum FilterPanelButton {REMOVE_BUTTON, PLUS_BUTTON, MINUS_BUTTON}

    @Override
    public Type<SubmitterFilterPanelHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(SubmitterFilterPanelHandler handler) {
        handler.onSubmitterFilterPanelButtonClick(this);
    }

    public FilterPanelButton getFilterPanelButton() {
        return null;
    }
}
