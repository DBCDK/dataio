package dk.dbc.dataio.gui.client.resources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface Resources extends ClientBundle {
    Resources INSTANCE = GWT.create(Resources.class);


    /*
     * CSS Resources
     */

    interface DataIoCss extends CssResource {
        @ClassName("dual-list-additem-class")
        String dualListAdditemClass();

        @ClassName("dio-DataEntry-PromptLabelClass")
        String dioDataEntryPromptLabelClass();

        @ClassName("dio-DualList")
        String dioDualList();

        @ClassName("dio-flow-element")
        String dioFlowElement();

        @ClassName("dio-DualPanesPanel-WidgetRightClass")
        String dioDualPanesPanelWidgetRightClass();

        @ClassName("status-popup")
        String statusPopup();

        @ClassName("dio-DataEntry")
        String dioDataEntry();

        @ClassName("fixed-empty-listbox-width")
        String fixedEmptyListboxWidth();

        @ClassName("dual-list-removeitem-class")
        String dualListRemoveitemClass();

        @ClassName("gwt-Button")
        String gwtButton();

        @ClassName("dual-list-selection-buttons-pane-class")
        String dualListSelectionButtonsPaneClass();

        @ClassName("dio-SaveButton-ResultLabel")
        String dioSaveButtonResultLabel();

        @ClassName("dio-DualPanesPanel-WidgetLeftClass")
        String dioDualPanesPanelWidgetLeftClass();

        @ClassName("navigation-panel-image")
        String navigationPanelImage();

        @ClassName("navigation-panel-class")
        String navigationPanelClass();

        @ClassName("gwt-Tree")
        String gwtTree();

        @ClassName("gwt-TreeItem")
        String gwtTreeItem();
    }

    @Source("css/dataio.css")
    DataIoCss css();


    /*
     * Image Resources
     */

    @ImageResource.ImageOptions(preventInlining=true)
    @Source("img/dbclogo.gif")
    ImageResource dbcLogo();

    @ImageResource.ImageOptions(preventInlining=true)
    @Source("img/lamp_gray.png")
    ImageResource gray();

    @ImageResource.ImageOptions(preventInlining=true)
    @Source("img/lamp_green.png")
    ImageResource green();

    @ImageResource.ImageOptions(preventInlining=true)
    @Source("img/lamp_red.png")
    ImageResource red();

    @ImageResource.ImageOptions(repeatStyle = ImageResource.RepeatStyle.Vertical)
    @Source("img/navigationbg.gif")
    ImageResource navigationBackground();

}
