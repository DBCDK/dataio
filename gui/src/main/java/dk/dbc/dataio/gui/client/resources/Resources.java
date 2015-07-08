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

        @ClassName("dio-PromptedData-PromptClass")
        String dioDataEntryPromptLabelClass();

        @ClassName("dio-DualList")
        String dioDualList();

        @ClassName("dio-flow-element")
        String dioFlowElement();

        @ClassName("dio-DualPanesPanel-WidgetRightClass")
        String dioDualPanesPanelWidgetRightClass();

        @ClassName("status-popup")
        String statusPopup();

        @ClassName("dio-PromptedData")
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

        @ClassName("dio-PromptedData-DataClass")
        String dioDataEntryInputBoxClass();

        @ClassName("sortable-widget-entry-deselected")
        String sortableWidgetEntryDeselected();

        @ClassName("dio-SortableList")
        String dioSortableList();

        @ClassName("sortable-widget-entry-disabled")
        String sortableWidgetEntryDisabled();

        @ClassName("sortable-widget-entry-selected")
        String sortableWidgetEntrySelected();

        @ClassName("gwtQuery-draggable-dragging")
        String gwtQueryDraggableDragging();

        @ClassName("dio-MultiProgressBar")
        String dioMultiProgressBar();

        @ClassName("deleteButton")
        String deleteButton();

        @ClassName("jobsShowTable")
        String jobsShowTable();
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

    @ImageResource.ImageOptions(preventInlining=true)
    @Source("img/add-button.gif")
    ImageResource addButton();

    @ImageResource.ImageOptions(preventInlining=true)
    @Source("img/remove-button.gif")
    ImageResource removeButton();

    @ImageResource.ImageOptions(preventInlining=true)
    @Source("img/delete-button.gif")
    ImageResource deleteButton();

    @ImageResource.ImageOptions(repeatStyle = ImageResource.RepeatStyle.Vertical)
    @Source("img/navigationbg.gif")
    ImageResource navigationBackground();

}
