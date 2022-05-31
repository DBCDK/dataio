package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;

public class Tooltip extends PopupPanel implements MouseOverHandler, MouseOutHandler {

    private final FocusWidget widget;

    public Tooltip(FocusWidget widget, String text) {
        super(true);
        getElement().addClassName("tooltip-class");
        Label l = new Label(text);
        setWidget(l);
        this.widget = widget;
        widget.addMouseOverHandler(this);
        widget.addMouseOutHandler(this);
    }

    @Override
    public void onMouseOver(MouseOverEvent event) {
        int x = widget.getElement().getAbsoluteRight();
        int y = widget.getElement().getAbsoluteTop();
        setPopupPosition(x, y);
        show();
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        hide();
    }
}
