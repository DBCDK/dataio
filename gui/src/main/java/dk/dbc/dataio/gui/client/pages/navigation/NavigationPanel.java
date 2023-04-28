package dk.dbc.dataio.gui.client.pages.navigation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import dk.dbc.dataio.commons.types.config.ConfigConstants;
import dk.dbc.dataio.gui.client.pages.job.show.ShowAcctestJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowPeriodicJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowTestJobsPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

public class NavigationPanel extends DockLayoutPanel {
    interface NavigationBinder extends UiBinder<DockLayoutPanel, NavigationPanel> {
    }

    private static NavigationBinder uiBinder = GWT.create(NavigationBinder.class);

    private final PlaceController placeController;
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    @UiField
    Tree menu;
    @UiField
    TreeItem jobs;
    @UiField
    TreeItem periodicJobs;
    @UiField
    TreeItem testJobs;
    @UiField
    TreeItem acctestJobs;
    @UiField
    TreeItem submitters;
    @UiField
    TreeItem flows;
    @UiField
    TreeItem flowComponents;
    @UiField
    TreeItem flowBinders;
    @UiField
    TreeItem sinks;
    @UiField
    TreeItem sinkStatus;
    @UiField
    TreeItem harvesters;
    @UiField
    TreeItem tickleHarvesters;
    @UiField
    TreeItem rrHarvesters;
    @UiField
    TreeItem coRepoHarvesters;
    @UiField
    TreeItem httpFtpFetchHarvesters;
    @UiField
    TreeItem infomediaHarvesters;
    @UiField
    TreeItem periodicJobsHarvesters;
    @UiField
    TreeItem promatHarvester;
    @UiField
    TreeItem dmatHarvester;
    @UiField
    TreeItem gatekeeper;
    @UiField
    TreeItem ioTraffic;
    @UiField
    TreeItem ftp;
    @UiField
    TreeItem failedFtps;
    @UiField
    TreeItem baseMaintenance;
    @UiField
    TreeItem jobPurge;
    @UiField
    TreeItem flowBinderStatus;
    @UiField
    Label debugInfo;


    /**
     * Constructor for the NavigationPanel
     *
     * @param placeController The placecontroller to use, when navigating
     */
    public NavigationPanel(PlaceController placeController) {
        super(Style.Unit.EM);
        this.placeController = placeController;
        add(uiBinder.createAndBindUi(this));
        commonInjector.getConfigProxyAsync().getConfigResource(ConfigConstants.SATURN_URL, new GetSaturnUrlCallback());

        jobs.setUserObject(ShowJobsPlace.class);
        periodicJobs.setUserObject(ShowPeriodicJobsPlace.class);
        testJobs.setUserObject(ShowTestJobsPlace.class);
        acctestJobs.setUserObject(ShowAcctestJobsPlace.class);
        flowBinders.setUserObject(dk.dbc.dataio.gui.client.pages.flowbinder.show.Place.class);
        flows.setUserObject(dk.dbc.dataio.gui.client.pages.flow.show.Place.class);
        flowComponents.setUserObject(dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place.class);
        harvesters.setUserObject(rrHarvesters);
        tickleHarvesters.setUserObject(dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show.Place.class);
        rrHarvesters.setUserObject(dk.dbc.dataio.gui.client.pages.harvester.rr.show.Place.class);
        coRepoHarvesters.setUserObject(dk.dbc.dataio.gui.client.pages.harvester.corepo.show.Place.class);
        infomediaHarvesters.setUserObject(dk.dbc.dataio.gui.client.pages.harvester.infomedia.show.Place.class);
        periodicJobsHarvesters.setUserObject(dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show.Place.class);
        promatHarvester.setUserObject(dk.dbc.dataio.gui.client.pages.harvester.promat.show.Place.class);
        dmatHarvester.setUserObject(dk.dbc.dataio.gui.client.pages.harvester.dmat.show.Place.class);
        submitters.setUserObject(dk.dbc.dataio.gui.client.pages.submitter.show.Place.class);
        sinks.setUserObject(dk.dbc.dataio.gui.client.pages.sink.show.Place.class);
        sinkStatus.setUserObject(dk.dbc.dataio.gui.client.pages.sink.status.Place.class);
        flowBinderStatus.setUserObject(dk.dbc.dataio.gui.client.pages.flowbinder.status.Place.class);
        gatekeeper.setUserObject(ioTraffic);
        ioTraffic.setUserObject(dk.dbc.dataio.gui.client.pages.iotraffic.Place.class);
        ftp.setUserObject(dk.dbc.dataio.gui.client.pages.gatekeeper.ftp.show.Place.class);
        failedFtps.setUserObject(dk.dbc.dataio.gui.client.pages.failedftps.show.Place.class);
        baseMaintenance.setUserObject(dk.dbc.dataio.gui.client.pages.basemaintenance.Place.class);
        jobPurge.setUserObject(dk.dbc.dataio.gui.client.pages.job.purge.Place.class);
    }


    /**
     * Sets the debug info text to be displayed in a small font at the lower left corner of the view
     *
     * @param text The text to display as debug info
     */
    public void setDebugInfo(String text) {
        debugInfo.setText(text);
    }


    /**
     * Event handler for menu navigation events
     *
     * @param event The triggering event
     */
    @UiHandler("menu")
    void menuPressed(SelectionEvent<TreeItem> event) {
        doSelect(event.getSelectedItem());
    }

    /**
     * Make a selection
     * An action is activated, based on the type of the user object, embedded in the TreeItem passed as a parameter in the call to the method
     * If the user object is Place type, activate the Place
     * If the user object is a TreeItem, the tree item does not have a direct action
     * Instead, select the first item in the sub list (if any), and do the selection on this item instead - meaning
     * call this method recursively with the new object as parameter
     *
     * @param item The item to do the selection upon
     */
    private void doSelect(TreeItem item) {
        clearAllSelected(menu);
        Object object = item.getUserObject();
        if (placeController != null && object != null) {
            if (object instanceof TreeItem) {
                doSelect((TreeItem) object);
            } else if (object instanceof String) {
                Window.open((String) object, "_blank", "");
            } else {
                placeController.goTo(getNewInstance(object));
                setSelection(item);
            }
        }
    }

    private static Place getNewInstance(Object object) {
//        return object.getClass().newInstance();  // It would be lovely to do like this, however - newInstance is not accessible from the client code of GWT, so do the following instead:
        if (object == ShowJobsPlace.class) {
            return new ShowJobsPlace();
        }
        if (object == ShowPeriodicJobsPlace.class) {
            return new ShowPeriodicJobsPlace();
        }
        if (object == ShowTestJobsPlace.class) {
            return new ShowTestJobsPlace();
        }
        if (object == ShowAcctestJobsPlace.class) {
            return new ShowAcctestJobsPlace();
        }
        if (object == dk.dbc.dataio.gui.client.pages.flowbinder.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.flowbinder.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.flow.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.flow.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.flowcomponent.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.harvester.rr.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.harvester.rr.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.harvester.corepo.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.harvester.corepo.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.harvester.infomedia.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.harvester.infomedia.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.harvester.promat.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.harvester.promat.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.harvester.dmat.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.harvester.dmat.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.submitter.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.submitter.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.sink.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.sink.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.sink.status.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.sink.status.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.flowbinder.status.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.flowbinder.status.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.iotraffic.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.iotraffic.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.gatekeeper.ftp.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.gatekeeper.ftp.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.failedftps.show.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.failedftps.show.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.basemaintenance.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.basemaintenance.Place();
        }
        if (object == dk.dbc.dataio.gui.client.pages.job.purge.Place.class) {
            return new dk.dbc.dataio.gui.client.pages.job.purge.Place();
        }
        return null;
    }

    /**
     * Traverses through all tree items in the tree, and clears the selection on each of them
     *
     * @param tree The tree, containing the tree items
     */
    private void clearAllSelected(Tree tree) {
        int count = tree.getItemCount();
        for (int i = 0; i < count; i++) {
            clearTreeItemSelection(tree.getItem(i));
        }
    }

    /**
     * Clears the selection. If the item contains children, each one of them are cleared also (using recursion)
     *
     * @param item The tree item to clear the selection
     */
    private void clearTreeItemSelection(TreeItem item) {
        item.setSelected(false);
        int count = item.getChildCount();
        for (int i = 0; i < count; i++) {
            clearTreeItemSelection(item.getChild(i));
        }
    }

    /**
     * Sets the selection on the item, passed as a parameter
     * If the item has parents, it is checked, whether the item is displayed
     *
     * @param item The item to set as selected
     */
    private void setSelection(TreeItem item) {
        item.setSelected(true);
        setParentUncovered(item);
    }

    /**
     * Assure that the item is visible - ie. its parent is not folded
     *
     * @param item The item to check for visibility
     */
    private void setParentUncovered(TreeItem item) {
        if (item != null) {
            TreeItem parent = item.getParentItem();
            if (parent != null) {
                parent.setState(true);
                setParentUncovered(parent);  // Assure that the grand-parent is also uncovered
            }
        }
    }


    /**
     * Callback class for getting Saturn Url
     */
    public class GetSaturnUrlCallback implements AsyncCallback<String> {
        @Override
        public void onFailure(Throwable caught) {
            Window.alert(commonInjector.getMenuTexts().error_SystemPropertyCouldNotBeRead());
        }

        @Override
        public void onSuccess(String result) {
            httpFtpFetchHarvesters.setUserObject(result);
        }
    }

}
