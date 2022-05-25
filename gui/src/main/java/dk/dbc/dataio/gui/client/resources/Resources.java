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

        @ClassName("main-panel-image")
        String mainPanelImage();

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

        @ClassName("deleteUpButton")
        String deleteUpButton();

        @ClassName("deleteDownButton")
        String deleteDownButton();

        @ClassName("plusUpButton")
        String plusUpButton();

        @ClassName("plusDownButton")
        String plusDownButton();

        @ClassName("minusUpButton")
        String minusUpButton();

        @ClassName("minusDownButton")
        String minusDownButton();

        @ClassName("jobsShowTable")
        String jobsShowTable();

        @ClassName("gwt-TextBox")
        String gwtTextBox();

        @ClassName("gwt-DialogBox")
        String gwtDialogBox();

        String invisible();

        @ClassName("gray-box-container")
        String grayBoxContainer();

        @ClassName("dio-Prompted")
        String dioPrompted();

        @ClassName("dio-Prompted-Prompt")
        String dioPromptedPrompt();

        @ClassName("hide-cell")
        String hideCell();

        @ClassName("gwt-PushButton-up")
        String gwtPushButtonUp();

        String multilistremovebutton();

        @ClassName("gwt-ListBox")
        String gwtListBox();

        String tooltiptext();

        @ClassName("test-jobs-table")
        String testJobsTable();

        @ClassName("tooltip-class")
        String tooltipClass();

        @ClassName("gwt-PushButton-up-hovering")
        String gwtPushButtonUpHovering();

        @ClassName("button-cell")
        String buttonCell();

        @ClassName("gwt-PushButton-down-hovering")
        String gwtPushButtonDownHovering();

        @ClassName("filter-list")
        String filterList();

        @ClassName("gray-box-content")
        String grayBoxContent();

        @ClassName("gwt-TabPanelBottom")
        String gwtTabPanelBottom();

        @ClassName("debug-info")
        String debugInfo();

        @ClassName("gwt-RadioButton")
        String gwtRadioButton();

        @ClassName("gwt-Label")
        String gwtLabel();

        @ClassName("dio-Prompted-Data")
        String dioPromptedData();

        String center();

        String Caption();

        @ClassName("show-cell")
        String showCell();

        @ClassName("acctest-jobs-table")
        String acctestJobsTable();
    }

    @Source("css/dataio.css")
    DataIoCss css();


    /*
     * Image Resources
     */

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/dbclogo.gif")
    ImageResource dbcLogo();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/blue_twirl_background.png")
    ImageResource blue_twirl();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/blue-ocean-background.png")
    ImageResource blue_ocean();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/rose-petals-background.png")
    ImageResource rose_petals();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/lamp_gray.png")
    ImageResource gray();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/lamp_green.png")
    ImageResource green();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/lamp_red.png")
    ImageResource red();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/lamp_yellow.png")
    ImageResource yellow();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/add-button.gif")
    ImageResource addButton();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/remove-button.gif")
    ImageResource removeButton();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/delete-up-button.gif")
    ImageResource deleteUpButton();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/delete-down-button.gif")
    ImageResource deleteDownButton();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/plus-up-button.gif")
    ImageResource plusUpButton();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/plus-down-button.gif")
    ImageResource plusDownButton();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/minus-up-button.gif")
    ImageResource minusUpButton();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/minus-down-button.gif")
    ImageResource minusDownButton();

    @ImageResource.ImageOptions(repeatStyle = ImageResource.RepeatStyle.Vertical)
    @Source("img/navigationbg.gif")
    ImageResource navigationBackground();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/calendar-icon.png")
    ImageResource calendarIcon();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/recycle.png")
    ImageResource recycleIcon();

    @ImageResource.ImageOptions(preventInlining = true)
    @Source("img/empty.png")
    ImageResource emptyIcon();

}
