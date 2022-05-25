package dk.dbc.dataio.gui.client.events;

import com.google.gwt.event.dom.client.DomEvent;

public class DialogEvent extends DomEvent<DialogHandler> {
    private static final Type<DialogHandler> TYPE = new DomEvent.Type("dialog", new DialogEvent());

    public enum DialogButton {OK_BUTTON, CANCEL_BUTTON, EXTRA_BUTTON}

    @Override
    public Type<DialogHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(DialogHandler handler) {
        handler.onDialogButtonClick(this);
    }

    public DialogButton getDialogButton() {
        return null;
    }
}
