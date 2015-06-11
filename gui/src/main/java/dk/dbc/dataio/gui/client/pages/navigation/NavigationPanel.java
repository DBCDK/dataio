package dk.dbc.dataio.gui.client.pages.navigation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

public class NavigationPanel extends FlowPanel {
    interface NavigationBinder extends UiBinder<HTMLPanel, NavigationPanel> {}
    private static NavigationBinder uiBinder = GWT.create(NavigationBinder.class);

    private final PlaceController placeController;

    @UiField Tree menu;
    @UiField TreeItem jobs;
    @UiField TreeItem submitters;
    @UiField TreeItem submitterCreation;
    @UiField TreeItem flows;
    @UiField TreeItem flowCreation;
    @UiField TreeItem flowComponentCreation;
    @UiField TreeItem flowComponents;
    @UiField TreeItem flowBinders;
    @UiField TreeItem sinks;
    @UiField TreeItem sinkCreation;


    /**
     * Constructor for the NavigationPanel
     *
     * @param placeController The placecontroller to use, when navigating
     */
    public NavigationPanel(PlaceController placeController) {
        super();
        this.placeController = placeController;
        add(uiBinder.createAndBindUi(this));
        jobs.setUserObject(new dk.dbc.dataio.gui.client.pages.job.show.Place());
        submitters.setUserObject(new dk.dbc.dataio.gui.client.pages.submitter.show.Place());
        submitterCreation.setUserObject(new dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace());
        flows.setUserObject(new dk.dbc.dataio.gui.client.pages.flow.show.Place());
        flowCreation.setUserObject(new dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace());
        flowComponentCreation.setUserObject(new dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace());
        flowComponents.setUserObject(new dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place());
        flowBinders.setUserObject(new dk.dbc.dataio.gui.client.pages.flowbinder.show.Place());
        sinks.setUserObject(new dk.dbc.dataio.gui.client.pages.sink.show.Place());
        sinkCreation.setUserObject(new dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace());
    }

    /**
     * Event handler for menu navigation events
     *
     * @param event The triggering event
     */
    @UiHandler("menu")
    void halloPressed(SelectionEvent<TreeItem> event) {
        Place place = (Place) event.getSelectedItem().getUserObject();
        if (placeController != null && place != null) {
            placeController.goTo(place);
        }
    }

}
