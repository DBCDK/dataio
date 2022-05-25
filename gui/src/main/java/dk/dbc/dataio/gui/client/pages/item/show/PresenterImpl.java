package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.TabBar;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.gui.client.components.JobNotificationPanel;
import dk.dbc.dataio.gui.client.components.prompted.PromptedAnchor;
import dk.dbc.dataio.gui.client.components.prompted.PromptedLabel;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.Notification;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PresenterImpl<P extends Place> extends AbstractActivity implements Presenter {
    private static final String JNDI_ERROR_MESSAGE = "Der er sket en uventet fejl som beskrevet i Bug #20623.\nDet er nu vigtigt, at du tager et screenshot af denne besked, og sender til Steen, så vil han tage affære.\nVariabelnavn: ";

    private final Map<String, Integer> tabIndexes = new HashMap<>(0);
    private static final String EMPTY = "";
    private static final String PATH_PARAM_PLACEHOLDER = "{jobId}";
    private static final String TRACE_ID = "TRACEID";

    private String recordId;

    private String header;
    private String endpoint;
    String urlDataioFilestoreRs = null;
    private String urlElk = null;

    /* Class scoped due to test */
    View view;
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    PlaceController placeController;
    String jobId;

    WorkflowNoteModel workflowNoteModel;
    JobSpecification.Type type;
    ItemListCriteria.Field itemSearchType;
    JobModel jobModel;

    /*
     * Default constructor
     */
    public PresenterImpl(P place, PlaceController placeController, View globalItemsView, String header) {
        this.placeController = placeController;
        this.view = globalItemsView;
        this.jobId = place.getParameter(Place.JOB_ID);
        this.recordId = place.getParameter(Place.RECORD_ID);
        if (recordId == null) {
            view.recordIdInputField.setText("");
        } else {
            view.recordIdInputField.setText(recordId);
        }
        this.header = header;
        commonInjector.getUrlResolverProxyAsync().getUrl("JOBSTORE_URL",
                new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        globalItemsView.setErrorText(viewInjector.getTexts().error_JndiFtpFetchError());
                    }

                    @Override
                    public void onSuccess(String jndiUrl) {
                        if (jndiUrl == null) {
                            Window.alert(JNDI_ERROR_MESSAGE + "endpoint");
                        }
                        endpoint = jndiUrl;
                    }
                });
        commonInjector.getUrlResolverProxyAsync().getUrl("FILESTORE_URL",
                new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        globalItemsView.setErrorText(viewInjector.getTexts().error_JndiFileStoreFetchError());
                    }

                    @Override
                    public void onSuccess(String jndiUrl) {
                        if (jndiUrl == null) {
                            Window.alert(JNDI_ERROR_MESSAGE + "urlDataioFilestoreRs");
                        }
                        urlDataioFilestoreRs = jndiUrl;
                    }
                });
        commonInjector.getUrlResolverProxyAsync().getUrl("ELK_URL",
                new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        globalItemsView.setErrorText(viewInjector.getTexts().error_JndiElkUrlFetchError());
                    }

                    @Override
                    public void onSuccess(String jndiUrl) {
                        if (jndiUrl == null) {
                            Window.alert(JNDI_ERROR_MESSAGE + "urlElk");
                        }
                        urlElk = jndiUrl;
                    }
                });
    }

    /*
     * Overridden methods
     */

    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view.setPresenter(this);
        view.setHeader(this.header);
        containerWidget.setWidget(view.asWidget());
        initializeView();
        listJobs(jobId);
        listNotifications(jobId);
    }

    /**
     * This method is called by the view, whenever the All Items tab has been selected
     * and defines the search criteria for locating all items within a job
     */
    @Override
    public void allItemsTabSelected() {
        itemSearchType = ItemListCriteria.Field.JOB_ID;
        final ItemListCriteria itemListCriteria = new ItemListCriteria().where(new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, Long.valueOf(jobId).intValue()));
        if (recordId != null) {
            itemListCriteria.and(new ListFilter<>(ItemListCriteria.Field.RECORD_ID, ListFilter.Op.EQUAL, recordId));
        }
        search(itemListCriteria, view.allItemsListTab);
    }

    /**
     * This method is called by the view, whenever the Failed Items tab has been selected
     * and defines the search criteria for locating failed items within a job
     */
    @Override
    public void failedItemsTabSelected() {
        itemSearchType = ItemListCriteria.Field.STATE_FAILED;
        final ListFilter jobIdEqualsCondition = new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, Long.valueOf(jobId).intValue());
        final ListFilter itemStatus = new ListFilter<>(ItemListCriteria.Field.STATE_FAILED);
        final ItemListCriteria itemListCriteria = new ItemListCriteria().where(jobIdEqualsCondition).and(itemStatus);
        search(itemListCriteria, view.failedItemsListTab);
    }

    /**
     * This method is called by the view, whenever the Ignored Items tab has been selected
     * and defines the search criteria for locating ignored items within a job
     */
    @Override
    public void ignoredItemsTabSelected() {
        itemSearchType = ItemListCriteria.Field.STATE_IGNORED;
        final ListFilter jobIdEqualsCondition = new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, Long.valueOf(jobId).intValue());
        final ListFilter itemStatus = new ListFilter<>(ItemListCriteria.Field.STATE_IGNORED);
        final ItemListCriteria itemListCriteria = new ItemListCriteria().where(jobIdEqualsCondition).and(itemStatus);
        search(itemListCriteria, view.ignoredItemsListTab);
    }

    /**
     * An indication from the view, that an item has been selected
     *
     * @param listView  The list view in question
     * @param itemModel The model for the selected item
     */
    @Override
    public void itemSelected(ItemsListView listView, ItemModel itemModel) {
        listView.detailedTabs.clear();
        populateItemTabIndexes(itemModel);
        addItemTabs(listView, itemModel);
        selectItemTab(listView, itemModel);
    }

    /**
     * This method is called by the view, whenever the Note tab has been selected
     * and sets the focus within the TextArea at cursor position 0
     */
    @Override
    public void noteTabSelected() {
        view.itemsListView.detailedTabs.setVisible(false);
        view.workflowNoteTabContent.note.setFocus(true);
        view.workflowNoteTabContent.note.setCursorPos(0);
    }

    @Override
    public void hideDetailedTabs() {
        setJobHeader();
        view.itemsListView.setVisible(false);
    }

    /**
     * This method is called after a click on the save button is captured. It saves the description
     * given as input on the workflowNoteModel
     *
     * @param description the description to save
     */
    @Override
    public void setWorkflowNoteModel(String description) {
        workflowNoteModel.setDescription(description);
        commonInjector.getJobStoreProxyAsync().setWorkflowNote(workflowNoteModel, Long.valueOf(jobId).intValue(), new SetJobWorkflowNoteCallback());
    }

    @Override
    public void setWorkflowNoteModel(ItemModel itemModel, boolean isProcessed) {
        WorkflowNoteModel itemModelWorkflowNoteModel = itemModel.getWorkflowNoteModel();
        if (itemModelWorkflowNoteModel.getAssignee().isEmpty()) {
            itemModelWorkflowNoteModel.setAssignee(workflowNoteModel.getAssignee());
        }
        itemModelWorkflowNoteModel.setProcessed(isProcessed);
        commonInjector.getJobStoreProxyAsync().setWorkflowNote(
                itemModelWorkflowNoteModel,
                Long.valueOf(itemModel.getJobId()).intValue(),
                Long.valueOf(itemModel.getChunkId()).intValue(),
                Long.valueOf(itemModel.getItemId()).shortValue(),
                new SetItemWorkflowNoteCallback());
    }

    @Override
    public void setItemModels(ItemsListView listView, List<ItemModel> itemModels) {
        if (itemModels.size() > 0) {
            listView.itemsTable.getSelectionModel().setSelected(itemModels.get(0), true);
        }
    }

    /**
     * Opens a new tab in the browser, containing a search in ELK for the item with trackingId as supplied
     *
     * @param trackingId The tracking ID to search for
     */
    @Override
    public void traceItem(String trackingId) {
        if (urlElk != null) {
            Window.open(Format.macro(urlElk, TRACE_ID, trackingId), "_blank", "");
        }
    }


    /**
     * Shows items with selected record id in the items show view
     */
    @Override
    public void recordSearch() {
        if (view.recordIdInputField.getText().isEmpty()) {
            recordId = null;
            selectJobTabVisibility();
            selectJobTab();
        } else {
            recordId = view.recordIdInputField.getText().trim();
            setJobTabVisibility(ViewWidget.FAILED_ITEMS_TAB_INDEX, false);
            setJobTabVisibility(ViewWidget.IGNORED_ITEMS_TAB_INDEX, false);
            view.tabPanel.selectTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        }
    }


    /*
     * Private methods
     */

    /**
     * Sets the view according to the supplied job model
     */
    private void initializeView() {
        view.itemsListView.itemsTable.setRowCount(0);
        view.jobDiagnosticTabContent.jobDiagnosticTable.setRowCount(0); // clear table on startup
        hideJobTabs();
    }

    /**
     * This method fetches the Job Model with the given jobId, and instantiates a callback class to take further action
     *
     * @param jobId The id for the job model to fetch
     */
    private void listJobs(String jobId) {
        final JobListCriteria jobListCriteria = new JobListCriteria().where(
                new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));

        commonInjector.getJobStoreProxyAsync().listJobs(jobListCriteria, new JobsCallback());
    }

    private void search(ItemListCriteria itemListCriteria, HTMLPanel tab) {
        setJobHeader();
        listItems(itemSearchType, itemListCriteria);
        populateHtmlTabPanel(tab);
    }

    /**
     * This methods attaches the item table and item pager to the html panel given as input
     *
     * @param panel the html panel
     */
    private void populateHtmlTabPanel(HTMLPanel panel) {
        panel.add(view.itemsPager);
        panel.add(view.itemsListView.itemsTable);
    }

    /**
     * This method fetches all Job Notifications associated with the job id given as a parameter in the call to the
     * method. The callback takes care of further processing.
     *
     * @param jobId Job Id
     */
    private void listNotifications(String jobId) {
        commonInjector.getJobStoreProxyAsync().listJobNotificationsForJob(Integer.parseInt(jobId), new JobNotificationsCallback());
    }

    /**
     * This method fetches items from the job store, and instantiates a callback class to take further action
     *
     * @param searchType       The search type to use
     * @param itemListCriteria The search criteria
     */
    private void listItems(ItemListCriteria.Field searchType, ItemListCriteria itemListCriteria) {
        view.itemsListView.detailedTabs.clear();
        view.itemsListView.detailedTabs.setVisible(false);
        view.dataProvider.setBaseCriteria(searchType, itemListCriteria);
    }

    /**
     * Sets the view according to the supplied job model
     *
     * @param jobModel containing the data to display in the view
     */
    private void setJobModel(JobModel jobModel) {
        this.jobModel = jobModel;
        this.workflowNoteModel = jobModel.getWorkflowNoteModel();
        this.type = jobModel.getType();
        setDiagnosticModels();
        selectJobTab();
        setJobInfoTab(jobModel);
        setWorkflowNoteTab();
        selectJobTabVisibility();
    }

    /**
     * Sets the jobHeader according to the supplied job model
     */
    private void setJobHeader() {
        if (recordId != null) {
            view.jobHeader.getElement().getStyle().setBackgroundColor("rgb(208, 228, 246)");
        } else {
            view.jobHeader.getElement().getStyle().clearBackgroundColor();
        }
        view.jobHeader.setText(constructJobHeaderText(jobModel, recordId));
    }

    /**
     * Sets the view according to the supplied job notifications
     *
     * @param jobNotifications The list of Job Notifications to view
     */
    private void setJobNotifications(List<Notification> jobNotifications) {
        view.jobNotificationsTabContent.clear();
        for (Notification notification : jobNotifications) {
            JobNotificationPanel panel = new JobNotificationPanel();
            panel.setJobId(String.valueOf(notification.getJobId()));
            panel.setDestination(notification.getDestination());
            panel.setTimeOfCreation(Format.formatLongDate(notification.getTimeOfCreation()));
            panel.setTimeOfLastModification(Format.formatLongDate(notification.getTimeOfLastModification()));
            panel.setType(formatType(notification.getType()));
            panel.setStatus(formatStatus(notification.getStatus()));
            panel.setStatusMessage(notification.getStatusMessage());
            panel.setContent(notification.getContent());
            view.jobNotificationsTabContent.add(panel);
        }
        selectJobTabVisibility();
    }

    private String formatType(Notification.Type type) {
        switch (type) {
            case JOB_COMPLETED:
                return getTexts().typeJobCompleted();
            case JOB_CREATED:
                return getTexts().typeJobCreated();
            default:
                return EMPTY;
        }
    }

    private String formatStatus(Notification.Status status) {
        switch (status) {
            case COMPLETED:
                return getTexts().statusCompleted();
            case FAILED:
                return getTexts().statusFailed();
            case WAITING:
                return getTexts().statusWaiting();
            default:
                return EMPTY;
        }
    }

    /*
     * ============= > Methods used for displaying job data and showing/hiding/selecting job tabs < =============
     */


    Texts getTexts() {
        return viewInjector.getTexts();
    }

    /**
     * Hides job tabs
     */
    private void hideJobTabs() {
        setJobTabVisibility(ViewWidget.ALL_ITEMS_TAB_INDEX, false);
        setJobTabVisibility(ViewWidget.FAILED_ITEMS_TAB_INDEX, false);
        setJobTabVisibility(ViewWidget.IGNORED_ITEMS_TAB_INDEX, false);
        setJobTabVisibility(ViewWidget.JOB_INFO_TAB_CONTENT, false);
        setJobTabVisibility(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT, false);
        setJobTabVisibility(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT, false);
        setJobTabVisibility(ViewWidget.WORKFLOW_NOTE_TAB_CONTENT, false);
    }

    /**
     * This method constructs a Job Header Text from a job model
     *
     * @param jobModel containing the job data
     * @param recordId the record id
     * @return The resulting Job Header Text
     */
    private String constructJobHeaderText(JobModel jobModel, String recordId) {
        return constructJobHeaderText(jobModel.getJobId(), jobModel.getSubmitterNumber(), jobModel.getSinkName(), recordId);
    }

    /**
     * This method constructs a Job Header Text from a Job Id, a Submitter Number and a Sink Name
     *
     * @param jobId           the job id
     * @param submitterNumber the submitter number
     * @param sinkName        the sink name
     * @return The resulting Job Header Text
     */
    private String constructJobHeaderText(String jobId, String submitterNumber, String sinkName, String recordId) {
        final Texts texts = getTexts();
        StringBuilder jobHeaderBuilder = new StringBuilder().append(texts.text_JobId()).append(" ").append(jobId).append(", ")
                .append(texts.text_Submitter()).append(" ").append(submitterNumber).append(", ")
                .append(texts.text_Sink()).append(" ").append(sinkName);

        if (recordId != null) {
            jobHeaderBuilder.append(", ").append(texts.text_recordId()).append(" ").append(recordId);
        }
        return jobHeaderBuilder.toString();
    }

    /**
     * Deciphers which tabs should be visible
     */
    private void selectJobTabVisibility() {
        setJobTabVisibility(ViewWidget.JOB_INFO_TAB_CONTENT, true);

        // Show diagnostic tab if one or more diagnostics exists
        if (view.jobDiagnosticTabContent.jobDiagnosticTable.getRowCount() > 0) {
            setJobTabVisibility(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT, true);
        }
        // Show notification tab if one or more notifications exists
        if (view.jobNotificationsTabContent.getNotificationsCount() > 0) {
            setJobTabVisibility(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT, true);
        }
        if (workflowNoteModel != null && !workflowNoteModel.getAssignee().isEmpty()) {
            setJobTabVisibility(ViewWidget.WORKFLOW_NOTE_TAB_CONTENT, true);
        }

        if (recordId == null) {
            // Show item information if one or more items exist
            if (jobModel.getNumberOfItems() != 0) {
                setJobTabVisibility(ViewWidget.ALL_ITEMS_TAB_INDEX, true);
                if (jobModel.getStateModel().getFailedCounter() != 0) {
                    setJobTabVisibility(ViewWidget.FAILED_ITEMS_TAB_INDEX, true);
                }
                if (jobModel.getStateModel().getIgnoredCounter() != 0) {
                    setJobTabVisibility(ViewWidget.IGNORED_ITEMS_TAB_INDEX, true);
                }
            }
        } else {
            setJobTabVisibility(ViewWidget.ALL_ITEMS_TAB_INDEX, true);
        }
    }

    /**
     * Deciphers which tab should have focus, when the view initially is presented to the user
     */
    private void selectJobTab() {
        if (jobModel.isDiagnosticFatal()) {
            view.tabPanel.selectTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        } else if (recordId != null) {
            view.tabPanel.selectTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        } else {
            if (jobModel.getStateModel().getFailedCounter() != 0) {
                view.tabPanel.selectTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
            } else if (jobModel.getStateModel().getIgnoredCounter() != 0) {
                view.tabPanel.selectTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
            } else {
                view.tabPanel.selectTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
            }
        }
    }

    /* Class scoped due to test */
    void setJobInfoTabContent(View view, JobModel jobModel) {
        view.jobInfoTabContent.packaging.setText(jobModel.getPackaging());
        view.jobInfoTabContent.format.setText(jobModel.getFormat());
        view.jobInfoTabContent.charset.setText(jobModel.getCharset());
        view.jobInfoTabContent.destination.setText(jobModel.getDestination());
        view.jobInfoTabContent.mailForNotificationAboutVerification.setText(jobModel.getMailForNotificationAboutVerification());
        view.jobInfoTabContent.mailForNotificationAboutProcessing.setText(jobModel.getMailForNotificationAboutProcessing());
        view.jobInfoTabContent.resultMailInitials.setText(jobModel.getResultMailInitials());
        view.jobInfoTabContent.type.setText(jobModel.getType().name());
        view.jobInfoTabContent.jobCreationTime.setText(jobModel.getJobCreationTime());
        view.jobInfoTabContent.jobCompletionTime.setText(jobModel.getJobCompletionTime());
        view.jobInfoTabContent.previousJobId.setVisible(false);
        if (jobModel.getDetailsAncestry() == null || jobModel.getDetailsAncestry().isEmpty()) {
            view.jobInfoTabContent.ancestrySection.setVisible(false);
        } else {
            view.jobInfoTabContent.ancestrySection.setVisible(true);
            setAncestryView(jobModel.getTransFileAncestry(), view.jobInfoTabContent.ancestryTransFile);
            setAncestryView(jobModel.getDataFileAncestry(), view.jobInfoTabContent.ancestryDataFile);
            setAncestryView(jobModel.getBatchIdAncestry(), view.jobInfoTabContent.ancestryBatchId);
            setAncestryView(jobModel.getDetailsAncestry(), view.jobInfoTabContent.ancestryContent);
        }
    }

    /**
     * Hide or Show the Tab. Can't call setVisible directly on Tab because it
     * is an interface. Need to cast to the underlying Composite and then
     * call setVisible on it.
     *
     * @param tabIndex the index as defined in the ViewWidget constants
     * @param showTab  whether to show or hide the tab.
     */
    private void setJobTabVisibility(int tabIndex, boolean showTab) {
        TabBar.Tab tabObject = view.tabPanel.getTabBar().getTab(tabIndex);
        if (tabObject == null) {
            return;
        }
        if (tabObject instanceof Composite) {
            ((Composite) tabObject).setVisible(showTab);
        }
    }

    /**
     * Sets the Job Info tab according to the supplied Job Model
     */
    private void setJobInfoTab(JobModel jobModel) {
        hideExportLinks(view);
        setJobInfoTabContent(view, jobModel);
        setFileStoreUrl(view.jobInfoTabContent.fileStore, jobModel);
        final int previousJobId = jobModel.getPreviousJobIdAncestry();
        if (previousJobId != 0) {
            view.jobInfoTabContent.previousJobId.setText(String.valueOf(previousJobId));
            view.jobInfoTabContent.previousJobId.setTargetHistoryToken(Place.TOKEN + ":" + previousJobId);
            view.jobInfoTabContent.previousJobId.setVisible(true);
        }
        setExportLinks(view, jobModel);
    }

    /* Class scoped due to test */
    void setFileStoreUrl(PromptedAnchor anchor, JobModel model) {
        if (anchor == null) {
            return;
        }
        if (model == null || urlDataioFilestoreRs == null || model.getDataFile() == null || model.getDataFile().isEmpty()) {
            anchor.setVisible(false);
            if (model == null) {
                setDebugInfoText(anchor, "Filestore er usynlig fordi model er null");
            } else if (urlDataioFilestoreRs == null) {
                setDebugInfoText(anchor, "Filestore er usynlig fordi urlDataioFilestoreRs er null");
            } else if (model.getDataFile() == null) {
                setDebugInfoText(anchor, "Filestore er usynlig fordi model.getDataFile() er null");
            } else if (model.getDataFile().isEmpty()) {
                setDebugInfoText(anchor, "Filestore er usynlig fordi model.getDataFile().isEmpty()");
            } else {
                setDebugInfoText(anchor, "Filestore er usynlig fordi - øhhh....???");
            }
        } else {
            final String[] numbers = model.getDataFile().split(":");
            if (numbers.length > 1) {
                anchor.setHrefAndText(urlDataioFilestoreRs + "/files/" + numbers[2]);
                anchor.setVisible(true);
            } else {
                anchor.setVisible(false);
                setDebugInfoText(anchor, "Filestore er usynlig fordi nummeret ikke kunne findes i: '" + model.getDataFile() + "'");
            }
        }
    }

    /* Class scoped due to unit test */
    void setExportLinks(View view, JobModel jobModel) {
        view.jobInfoTabContent.exportLinksHeader.setVisible(true);
        view.jobInfoTabContent.exportLinkItemsPartitioned.setHrefAndText(
                buildExportUrl(JobStoreServiceConstants.EXPORT_ITEMS_PARTITIONED));
        view.jobInfoTabContent.exportLinkItemsPartitioned.setVisible(true);
        view.jobInfoTabContent.exportLinkItemsProcessed.setHrefAndText(
                buildExportUrl(JobStoreServiceConstants.EXPORT_ITEMS_PROCESSED));
        view.jobInfoTabContent.exportLinkItemsProcessed.setVisible(true);
        if (jobModel.getStateModel().getPartitioning().getFailed() > 0) {
            view.jobInfoTabContent.exportLinkItemsFailedInPartitioning.setHrefAndText(
                    buildExportUrl(JobStoreServiceConstants.EXPORT_ITEMS_PARTITIONED_FAILED));
            view.jobInfoTabContent.exportLinkItemsFailedInPartitioning.setVisible(true);
        }
        if (jobModel.getStateModel().getProcessing().getFailed() > 0) {
            view.jobInfoTabContent.exportLinkItemsFailedInProcessing.setHrefAndText(
                    buildExportUrl(JobStoreServiceConstants.EXPORT_ITEMS_PROCESSED_FAILED));
            view.jobInfoTabContent.exportLinkItemsFailedInProcessing.setVisible(true);
        }
        if (jobModel.getStateModel().getDelivering().getFailed() > 0) {
            view.jobInfoTabContent.exportLinkItemsFailedInDelivering.setHrefAndText(
                    buildExportUrl(JobStoreServiceConstants.EXPORT_ITEMS_DELIVERED_FAILED));
            view.jobInfoTabContent.exportLinkItemsFailedInDelivering.setVisible(true);
        }
    }

    private void setDebugInfoText(PromptedAnchor anchor, String text) {
        anchor.setVisible(true);
        anchor.setText("Debug Info (se bug #20623 - lav venligst et Screen Dump og send til metascrum): " + text);
        anchor.setHref("http://bugs.dbc.dk/show_bug.cgi?id=20623");
    }

    private void setAncestryView(String value, PromptedLabel widget) {
        if (value == null || value.isEmpty()) {
            widget.setVisible(false);
        } else {
            widget.setText(value);
            widget.setVisible(true);
        }
    }

    private void setAncestryView(String value, InlineHTML widget) {
        if (value == null || value.isEmpty()) {
            widget.setVisible(false);
        } else {
            widget.setText(value);
            widget.setVisible(true);
        }
    }

    /**
     * Hides the jobInfoTabContent fields used for displaying export links
     *
     * @param view The view in question
     */
    private void hideExportLinks(View view) {
        view.jobInfoTabContent.exportLinkItemsFailedInPartitioning.setVisible(false);
        view.jobInfoTabContent.exportLinkItemsFailedInProcessing.setVisible(false);
        view.jobInfoTabContent.exportLinkItemsFailedInDelivering.setVisible(false);
    }

    /**
     * Builds the url string used for exporting items
     *
     * @param path       to the REST service
     * @param queryParam for the REST service
     * @return url as string
     */
    private String buildExportUrl(String path) {
        return endpoint + "/" + path.replace(PATH_PARAM_PLACEHOLDER, jobId);
    }

    /**
     * Sets the Job Diagnostic tab according to the supplied Job Model
     */
    private void setDiagnosticModels() {
        view.jobDiagnosticTabContent.jobDiagnosticTable.setRowData(0, jobModel.getDiagnosticModels());
    }

    /**
     * Sets the note tab according to the supplied Job Model
     */
    private void setWorkflowNoteTab() {
        if (workflowNoteModel != null) {
            view.workflowNoteTabContent.note.setText(workflowNoteModel.getDescription());
            view.fixedColumnVisible = !workflowNoteModel.getAssignee().isEmpty();
        }
    }

    /*
     * =================== > Methods used for displaying item data and selecting item tabs < ===================
     */

    /**
     * This method deciphers which tab indexes should be available.
     * If a fatal diagnostic exists, we are only interested in viewing the item diagnostic tab.
     * If the job type is not ACCTEST, we are not interesting in viewing the next output post tab.
     * The tab index assigned will therefore vary depending on the item data input.
     *
     * @param itemModel The model containing the item data
     */
    private void populateItemTabIndexes(ItemModel itemModel) {
        tabIndexes.clear();
        if (itemModel.isDiagnosticFatal()) {
            tabIndexes.put(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT, 0);
        } else {
            tabIndexes.put(ItemsListView.JAVASCRIPT_LOG_TAB_CONTENT, 0);
            tabIndexes.put(ItemsListView.INPUT_POST_TAB_CONTENT, 1);
            tabIndexes.put(ItemsListView.OUTPUT_POST_TAB_CONTENT, 2);
            if (type == JobSpecification.Type.ACCTEST) {
                tabIndexes.put(ItemsListView.NEXT_OUTPUT_POST_TAB_CONTENT, 3);
                tabIndexes.put(ItemsListView.SINK_RESULT_TAB_CONTENT, 4);
                if (!itemModel.getDiagnosticModels().isEmpty()) {
                    tabIndexes.put(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT, 5);
                }
            } else {
                tabIndexes.put(ItemsListView.SINK_RESULT_TAB_CONTENT, 3);
                if (!itemModel.getDiagnosticModels().isEmpty()) {
                    tabIndexes.put(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT, 4);
                }
            }
        }
    }

    /**
     * This method adds item tabs to the list view.
     *
     * @param listView  The list view in question
     * @param itemModel The model containing the item data
     */
    private void addItemTabs(ItemsListView listView, ItemModel itemModel) {
        Texts texts = getTexts();
        if (tabIndexes.containsKey(ItemsListView.JAVASCRIPT_LOG_TAB_CONTENT)) {
            listView.detailedTabs.add(new JavascriptLogTabContent(texts, commonInjector.getLogStoreProxyAsync(), itemModel), texts.tab_JavascriptLog());
        }
        if (tabIndexes.containsKey(ItemsListView.INPUT_POST_TAB_CONTENT)) {
            listView.detailedTabs.add(new ItemTabContent(texts, commonInjector.getJobStoreProxyAsync(), itemModel, ItemModel.LifeCycle.PARTITIONING), texts.tab_PartitioningPost());
        }
        if (tabIndexes.containsKey(ItemsListView.OUTPUT_POST_TAB_CONTENT)) {
            listView.detailedTabs.add(new ItemTabContent(texts, commonInjector.getJobStoreProxyAsync(), itemModel, ItemModel.LifeCycle.PROCESSING), texts.tab_ProcessingPost());
        }
        if (tabIndexes.containsKey(ItemsListView.NEXT_OUTPUT_POST_TAB_CONTENT)) {
            listView.detailedTabs.add(new NextTabContent(texts, commonInjector.getJobStoreProxyAsync(), itemModel), texts.tab_NextOutputPost());
        }
        if (tabIndexes.containsKey(ItemsListView.SINK_RESULT_TAB_CONTENT)) {
            listView.detailedTabs.add(new ItemTabContent(texts, commonInjector.getJobStoreProxyAsync(), itemModel, ItemModel.LifeCycle.DELIVERING), texts.tab_DeliveringPost());
        }
        if (tabIndexes.containsKey(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT)) {
            setDiagnosticModels(listView, itemModel);
            listView.detailedTabs.add(listView.itemDiagnosticTabContent.itemDiagnosticTable, texts.tab_ItemDiagnostic());
            listView.detailedTabs.add(listView.itemDiagnosticTabContent.stacktraceTable, texts.tab_Stacktrace());
        }
    }

    /**
     * This method decides which detailed tab should be default selected
     *
     * @param listView  The list view in question
     * @param itemModel The model containing the item data
     */
    private void selectItemTab(ItemsListView listView, ItemModel itemModel) {
        ItemModel.LifeCycle status = itemModel.getStatus();
        if (itemModel.isDiagnosticFatal()) {
            listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT));
        } else {
            // Acceptance test job: Show sink result
            if (type == JobSpecification.Type.ACCTEST) {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.SINK_RESULT_TAB_CONTENT));
            }
            // Item failed or ignored in delivering: Show sink result
            else if (itemSearchType == ItemListCriteria.Field.STATE_FAILED && status == ItemModel.LifeCycle.DELIVERING_FAILED
                    || itemSearchType == ItemListCriteria.Field.STATE_IGNORED && status == ItemModel.LifeCycle.DELIVERING_IGNORED) {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.SINK_RESULT_TAB_CONTENT));
            }
            // Item failed in processing: Show output post
            else if (itemSearchType == ItemListCriteria.Field.STATE_FAILED && status == ItemModel.LifeCycle.PROCESSING_FAILED) {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.OUTPUT_POST_TAB_CONTENT));
            }
            // Item ignored in processing: Show output post
            else if (itemSearchType == ItemListCriteria.Field.STATE_IGNORED && status == ItemModel.LifeCycle.PROCESSING_IGNORED) {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.OUTPUT_POST_TAB_CONTENT));
            } else {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.JAVASCRIPT_LOG_TAB_CONTENT));
            }
        }
        listView.detailedTabs.setVisible(true);
    }

    /**
     * Sets the Item Diagnostic tab according to the supplied Job Model
     *
     * @param itemModel The Item Model, where the list of Diagnostic data is taken
     */
    private void setDiagnosticModels(ItemsListView listView, ItemModel itemModel) {
        listView.itemDiagnosticTabContent.itemDiagnosticTable.setRowData(0, itemModel.getDiagnosticModels());
        listView.itemDiagnosticTabContent.stacktraceTable.setRowData(0, itemModel.getDiagnosticModels());
    }

    /**
     * Prepares the view for display in case of no jobs returned from the underlying data-store.
     * 1) The method builds a new header containing the id of the sought out job.
     * 2) The method displays an empty job-info tab.
     * 3) The method sets an error message for the user.
     */
    private void prepareViewForJobNotFoundDisplay() {
        view.jobHeader.setText(constructJobHeaderText(jobId, EMPTY, EMPTY, null));
        setJobTabVisibility(ViewWidget.JOB_INFO_TAB_CONTENT, true);
        view.tabPanel.selectTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        view.setErrorText(getTexts().error_CouldNotFindJob());
    }

    /*
     * ================================ > Private async callback classes < =====================================
     */

    /*
     * Callback class for fetching Jobs
     */
    class JobsCallback implements AsyncCallback<List<JobModel>> {
        @Override
        public void onFailure(Throwable throwable) {
            view.setErrorText(getTexts().error_CouldNotFetchJob());
        }

        @Override
        public void onSuccess(List<JobModel> jobModels) {
            if (jobModels != null && jobModels.size() > 0) {
                setJobModel(jobModels.get(0));
            } else {
                prepareViewForJobNotFoundDisplay();
            }
        }
    }

    class JobNotificationsCallback implements AsyncCallback<List<Notification>> {
        @Override
        public void onFailure(Throwable throwable) {
            view.setErrorText(getTexts().error_CouldNotFetchJobNotifications());
        }

        @Override
        public void onSuccess(List<Notification> jobNotifications) {
            if (jobNotifications != null) {
                setJobNotifications(jobNotifications);
            }
        }
    }

    protected class SetJobWorkflowNoteCallback extends FilteredAsyncCallback<JobModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(JobModel jobModel) {
            selectJobTab();
        }
    }

    protected class SetItemWorkflowNoteCallback extends FilteredAsyncCallback<ItemModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(ItemModel itemModel) {
        }
    }
}
