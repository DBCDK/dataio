package dk.dbc.dataio.gui.client.events;

import com.google.gwt.event.dom.client.DomEvent;

public class FlowBinderFilterPanelEvent extends DomEvent<FlowBinderFilterPanelHandler> {
    private static final Type<FlowBinderFilterPanelHandler> TYPE =
            new Type("flow-binder-filter-panel", new FlowBinderFilterPanelEvent());

    public enum FilterPanelButton {REMOVE_BUTTON, PLUS_BUTTON, MINUS_BUTTON}

    @Override
    public Type<FlowBinderFilterPanelHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FlowBinderFilterPanelHandler handler) {
        handler.onFlowBinderFilterPanelButtonClick(this);
    }

    public FilterPanelButton getFilterPanelButton() {
        return null;
    }
}
