/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.TabBar;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.gui.client.components.JobNotificationPanel;
import dk.dbc.dataio.gui.client.components.PromptedAnchor;
import dk.dbc.dataio.gui.client.components.PromptedLabel;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PresenterImpl<P extends Place> extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    private final Map<String, Integer> tabIndexes = new HashMap<>(0);
    private static final String EMPTY = "";
    private static final String MARC2_FORMAT = "marc2";
    private static final String PATH_PARAM_PLACEHOLDER = "{jobId}";
    private static final String CHUNK_ITEM_TYPE_BYTES = "BYTES";
    private static final String CHUNK_ITEM_TYPE_DANMARC2LINEFORMAT = "DANMARC2LINEFORMAT";
    private static final String TRACE_ID = "TRACEID";

    protected PlaceController placeController;
    View globalItemsView;
    protected String jobId;
    protected int allItemCounter;
    protected int failedItemCounter;
    protected int ignoredItemCounter;
    protected WorkflowNoteModel workflowNoteModel;
    protected JobModel.Type type;
    protected ItemListCriteria.Field itemSearchType;
    private String header;
    private String endpoint;
    protected String urlDataioFilestoreRs = null;
    private String urlElk = null;

    /*
     * Default constructor
     */
    public PresenterImpl(P place, PlaceController placeController, View globalItemsView, String header) {
        this.placeController = placeController;
        this.globalItemsView = globalItemsView;
        this.jobId = place.getJobId();
        this.header = header;
        commonInjector.getJndiProxyAsync().getJndiResource(
                JndiConstants.URL_RESOURCE_JOBSTORE_RS,
                new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        viewInjector.getView().setErrorText(viewInjector.getTexts().error_JndiFtpFetchError());
                    }
                    @Override
                    public void onSuccess(String jndiUrl) {
                        endpoint = jndiUrl.replace(".dbc.dk", "");
                    }
                });
        commonInjector.getJndiProxyAsync().getJndiResource(
                JndiConstants.URL_RESOURCE_FILESTORE_RS,
                new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        viewInjector.getView().setErrorText(viewInjector.getTexts().error_JndiFileStoreFetchError());
                    }
                    @Override
                    public void onSuccess(String jndiUrl) {
                        urlDataioFilestoreRs = jndiUrl;
                    }
                });
        commonInjector.getJndiProxyAsync().getJndiResource(
                JndiConstants.URL_RESOURCE_ELK,
                new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        viewInjector.getView().setErrorText(viewInjector.getTexts().error_JndiElkUrlFetchError());
                    }
                    @Override
                    public void onSuccess(String jndiUrl) {
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
        getView().setPresenter(this);
        getView().setHeader(this.header);
        containerWidget.setWidget(getView().asWidget());
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
        final ListFilter jobIdEqualsCondition = new ListFilter<>(ItemListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, Long.valueOf(jobId).intValue());
        final ItemListCriteria itemListCriteria = new ItemListCriteria().where(jobIdEqualsCondition);
        listItems(itemSearchType, itemListCriteria, getView().allItemsList);
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
        listItems(itemSearchType, itemListCriteria, getView().failedItemsList);
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
        listItems(itemSearchType, itemListCriteria, getView().ignoredItemsList);
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
        View view = getView();
        view.workflowNoteTabContent.note.setFocus(true);
        view.workflowNoteTabContent.note.setCursorPos(0);
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

    /**
     * Sets the view according to the supplied item model list
     * @param itemModels The list of item models, containing the data to display in the view
     */
    @Override
    public void setItemModels(ItemsListView listView, List<ItemModel> itemModels) {
        getView().setSelectionEnabled(true);
        if(itemModels.size() > 0) {
            listView.itemsTable.getSelectionModel().setSelected(itemModels.get(0), true);
        }
    }

    /**
     * Opens a new tab in the browser, containing a search in ELK for the item with trackingId as supplied
     * @param trackingId The tracking ID to search for
     */
    @Override
    public void traceItem(String trackingId) {
        if (urlElk != null) {
            Window.open(Format.macro(urlElk, TRACE_ID, trackingId), "_blank", "");
        }
    }


    /*
     * Private methods
     */

    /**
     * Sets the view according to the supplied job model
     */
    private void initializeView() {
        View view = getView();
        view.allItemsList.itemsTable.setRowCount(0); //clear table on startup
        view.failedItemsList.itemsTable.setRowCount(0); //clear table on startup
        view.ignoredItemsList.itemsTable.setRowCount(0); //clear table on startup
        view.jobDiagnosticTabContent.jobDiagnosticTable.setRowCount(0); // clear table on startup
        hideJobTabs();
    }

    /**
     * This method fetches the Job Model with the given jobId, and instantiates a callback class to take further action
     * @param jobId The id for the job model to fetch
     */
    private void listJobs(String jobId) {
        final JobListCriteria jobListCriteria = new JobListCriteria().where(
                new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));

        commonInjector.getJobStoreProxyAsync().listJobs(jobListCriteria, new JobsCallback());
    }

    /**
     * This method fetches all Job Notifications associated with the job id given as a parameter in the call to the
     * method. The callback takes care of further processing.
     * @param jobId Job Id
     */
    private void listNotifications(String jobId) {
        commonInjector.getJobStoreProxyAsync().listJobNotificationsForJob(Integer.parseInt(jobId), new JobNotificationsCallback());
    }

    /**
     * This method fetches items from the job store, and instantiates a callback class to take further action
     * @param itemListCriteria The search criteria
     * @param listView The list view in question
     */
    private void listItems(ItemListCriteria.Field searchType, ItemListCriteria itemListCriteria, ItemsListView listView) {
        getView().setSelectionEnabled(false);
        listView.detailedTabs.clear();
        listView.detailedTabs.setVisible(false);
        getView().dataProvider.setBaseCriteria(searchType, listView, itemListCriteria);
    }

    /**
     * Sets the view according to the supplied job model
     * @param jobModel containing the data to display in the view
     */
    private void setJobModel(JobModel jobModel) {
        allItemCounter = (int) jobModel.getItemCounter();
        failedItemCounter = (int) jobModel.getFailedCounter();
        ignoredItemCounter = (int) jobModel.getIgnoredCounter();
        workflowNoteModel = jobModel.getWorkflowNoteModel();
        type = jobModel.getType();
        getView().jobHeader.setText(constructJobHeaderText(jobModel));
        setDiagnosticModels(jobModel);
        selectJobTab(jobModel);
        setJobInfoTab(jobModel);
        setWorkflowNoteTab();
        selectJobTabVisibility();
    }

    /**
     * Sets the view according to the supplied job notifications
     * @param jobNotifications The list of Job Notifications to view
     */
    private void setJobNotifications(List<JobNotification> jobNotifications) {
        getView().jobNotificationsTabContent.clear();
        for (JobNotification notification: jobNotifications) {
            JobNotificationPanel panel = new JobNotificationPanel();
            panel.setJobId(String.valueOf(notification.getJobId()));
            panel.setDestination(notification.getDestination());
            panel.setTimeOfCreation(Format.formatLongDate(notification.getTimeOfCreation()));
            panel.setTimeOfLastModification(Format.formatLongDate(notification.getTimeOfLastModification()));
            panel.setType(formatType(notification.getType()));
            panel.setStatus(formatStatus(notification.getStatus()));
            panel.setStatusMessage(notification.getStatusMessage());
            panel.setContent(notification.getContent());
            getView().jobNotificationsTabContent.add(panel);
        }
        selectJobTabVisibility();
    }

    private String formatType(JobNotification.Type type) {
        switch (type) {
            case JOB_COMPLETED: return getTexts().typeJobCompleted();
            case JOB_CREATED:   return getTexts().typeJobCreated();
            default: return EMPTY;
        }
    }

    private String formatStatus(JobNotification.Status status) {
        switch (status) {
            case COMPLETED: return getTexts().statusCompleted();
            case FAILED:    return getTexts().statusFailed();
            case WAITING:   return getTexts().statusWaiting();
            default: return EMPTY;
        }
    }

    /*
     * ============= > Methods used for displaying job data and showing/hiding/selecting job tabs < =============
     */

    View getView(){
        return this.globalItemsView;
    }
    Texts getTexts(){
        return  viewInjector.getTexts();
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
     * @param jobModel containing the job data
     * @return The resulting Job Header Text
     */
    private String constructJobHeaderText(JobModel jobModel) {
        return constructJobHeaderText(jobModel.getJobId(), jobModel.getSubmitterNumber(), jobModel.getSinkName());
    }

    /**
     * This method constructs a Job Header Text from a Job Id, a Submitter Number and a Sink Name
     * @param jobId the job id
     * @param submitterNumber the submitter number
     * @param sinkName the sink name
     * @return The resulting Job Header Text
     */
    private String constructJobHeaderText(String jobId, String submitterNumber, String sinkName) {
        return getTexts().text_JobId() + " " + jobId + ", "
                + getTexts().text_Submitter() + " " + submitterNumber + ", "
                + getTexts().text_Sink() + " " + sinkName;
    }

    /**
     * Deciphers which tabs should be visible
     */
    private void selectJobTabVisibility() {
        setJobTabVisibility(ViewWidget.JOB_INFO_TAB_CONTENT, true);

        // Show diagnostic tab if one or more diagnostics exists
        if (getView().jobDiagnosticTabContent.jobDiagnosticTable.getRowCount() > 0) {
            setJobTabVisibility(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT, true);
        }
        // Show notification tab if one or more notifications exists
        if (getView().jobNotificationsTabContent.getNotificationsCount() > 0) {
            setJobTabVisibility(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT, true);
        }
        // Show item information if one or more items exist
        if (allItemCounter != 0) {
            setJobTabVisibility(ViewWidget.ALL_ITEMS_TAB_INDEX, true);
            if (failedItemCounter != 0) {
                setJobTabVisibility(ViewWidget.FAILED_ITEMS_TAB_INDEX, true);
            }
            if (ignoredItemCounter != 0) {
                setJobTabVisibility(ViewWidget.IGNORED_ITEMS_TAB_INDEX, true);
            }
        }
        if(workflowNoteModel != null && !workflowNoteModel.getAssignee().isEmpty()) {
            setJobTabVisibility(ViewWidget.WORKFLOW_NOTE_TAB_CONTENT, true);
        }
    }

    /**
     * Deciphers which tab should have focus, when the view initially is presented to the user
     * @param jobModel containing the job data
     */
    private void selectJobTab(JobModel jobModel) {
        if(jobModel.isDiagnosticFatal()) {
            getView().tabPanel.selectTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        } else {
            if (failedItemCounter != 0) {
                getView().tabPanel.selectTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
            } else if (ignoredItemCounter != 0) {
                getView().tabPanel.selectTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
            } else {
                getView().tabPanel.selectTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
            }
        }
    }

    /**
     * Hide or Show the Tab. Can't call setVisible directly on Tab because it
     * is an interface. Need to cast to the underlying Composite and then
     * call setVisible on it.
     *
     * @param tabIndex the index as defined in the ViewWidget constants
     * @param showTab whether to show or hide the tab.
     */
    private void setJobTabVisibility(int tabIndex, boolean showTab) {
        TabBar.Tab tabObject = getView().tabPanel.getTabBar().getTab(tabIndex);
        if (tabObject == null) {
            return;
        }
        if (tabObject instanceof Composite) {
            ((Composite) tabObject).setVisible(showTab);
        }
    }

    /**
     * Sets the Job Info tab according to the supplied Job Model
     * @param jobModel The Job Model, where the Job Info data is taken
     */
    private void setJobInfoTab(JobModel jobModel) {
        View view = getView();
        hideExportLinks(view);
        view.jobInfoTabContent.packaging.setText(jobModel.getPackaging());
        view.jobInfoTabContent.format.setText(jobModel.getFormat());
        view.jobInfoTabContent.charset.setText(jobModel.getCharset());
        view.jobInfoTabContent.destination.setText(jobModel.getDestination());
        view.jobInfoTabContent.mailForNotificationAboutVerification.setText(jobModel.getMailForNotificationAboutVerification());
        view.jobInfoTabContent.mailForNotificationAboutProcessing.setText(jobModel.getMailForNotificationAboutProcessing());
        view.jobInfoTabContent.resultMailInitials.setText(jobModel.getResultmailInitials());
        view.jobInfoTabContent.type.setText(jobModel.getType().name());
        view.jobInfoTabContent.jobCreationTime.setText(jobModel.getJobCreationTime());
        view.jobInfoTabContent.jobCompletionTime.setText(jobModel.getJobCompletionTime());
        String previousJobId = jobModel.getPreviousJobIdAncestry();
        if (previousJobId == null || previousJobId.isEmpty() || previousJobId.equals("0")) {
            view.jobInfoTabContent.previousJobId.setVisible(false);
        } else {
            view.jobInfoTabContent.previousJobId.setText(previousJobId);
            view.jobInfoTabContent.previousJobId.setTargetHistoryToken(Place.TOKEN + ":" + previousJobId);
            view.jobInfoTabContent.previousJobId.setVisible(true);
        }

        setFileStoreUrl(view.jobInfoTabContent.fileStore, jobModel);

        if(jobModel.getFormat().toLowerCase().equals(MARC2_FORMAT) && jobModel.getFailedCounter() > 0) {
            view.jobInfoTabContent.exportLinksHeader.setVisible(true);
            if (jobModel.getPartitioningFailedCounter() > 0) {
                String exportUrl = buildExportUrl(JobStoreServiceConstants.EXPORT_ITEMS_PARTITIONED_FAILED, CHUNK_ITEM_TYPE_BYTES);
                view.jobInfoTabContent.exportLinkItemsFailedInPartitioning.setText(exportUrl);
                view.jobInfoTabContent.exportLinkItemsFailedInPartitioning.setVisible(true);
            }
            if (jobModel.getProcessingFailedCounter() > 0) {
                String exportUrl = buildExportUrl(JobStoreServiceConstants.EXPORT_ITEMS_PROCESSED_FAILED, CHUNK_ITEM_TYPE_DANMARC2LINEFORMAT);
                view.jobInfoTabContent.exportLinkItemsFailedInProcessing.setText(exportUrl);
                view.jobInfoTabContent.exportLinkItemsFailedInProcessing.setVisible(true);
            }
            if (jobModel.getDeliveringFailedCounter() > 0) {
                String exportUrl = buildExportUrl(JobStoreServiceConstants.EXPORT_ITEMS_DELIVERED_FAILED, CHUNK_ITEM_TYPE_DANMARC2LINEFORMAT);
                view.jobInfoTabContent.exportLinkItemsFailedInDelivering.setText(exportUrl);
                view.jobInfoTabContent.exportLinkItemsFailedInDelivering.setVisible(true);
            }
        }
        if (jobModel.getDetailsAncestry() == null || jobModel.getDetailsAncestry().isEmpty()) {
            view.jobInfoTabContent.ancestrySection.setVisible(false);
        } else {
            setAncestryView(jobModel.getTransFileAncestry(), view, view.jobInfoTabContent.ancestryTransFile);
            setAncestryView(jobModel.getDataFileAncestry(), view, view.jobInfoTabContent.ancestryDataFile);
            setAncestryView(jobModel.getBatchIdAncestry(), view, view.jobInfoTabContent.ancestryBatchId);
            setAncestryView(jobModel.getDetailsAncestry(), view, view.jobInfoTabContent.ancestryContent);
        }
    }

    void setFileStoreUrl(PromptedAnchor anchor, JobModel model) {
        if (anchor == null) {
            return;
        }
        if (model == null || urlDataioFilestoreRs == null || model.getDataFile() == null || model.getDataFile().isEmpty()) {
            anchor.setVisible(false);
        } else {
            final String[] numbers = model.getDataFile().split(":");
            if (numbers.length > 1) {
                anchor.setHrefAndText(urlDataioFilestoreRs + "/files/" + numbers[2]);
                anchor.setVisible(true);
            } else {
                anchor.setVisible(false);
            }
        }
    }

    private void setAncestryView(String value, View view, PromptedLabel widget) {
        if (value == null || value.isEmpty()) {
            widget.setVisible(false);
        } else {
            widget.setText(value);
            widget.setVisible(true);
            view.jobInfoTabContent.ancestrySection.setVisible(true);
        }
    }
    private void setAncestryView(String value, View view, InlineHTML widget) {
        if (value == null || value.isEmpty()) {
            widget.setVisible(false);
        } else {
            widget.setText(value);
            widget.setVisible(true);
            view.jobInfoTabContent.ancestrySection.setVisible(true);
        }
    }

    /**
     * Hides the jobInfoTabContent fields used for displaying export links
     * @param view The view in question
     */
    private void hideExportLinks(View view) {
        view.jobInfoTabContent.exportLinksHeader.setVisible(false);
        view.jobInfoTabContent.exportLinkItemsFailedInPartitioning.setVisible(false);
        view.jobInfoTabContent.exportLinkItemsFailedInProcessing.setVisible(false);
        view.jobInfoTabContent.exportLinkItemsFailedInDelivering.setVisible(false);
    }

    /**
     * Builds the url string used for exporting failed items
     * @param path to the REST service
     * @param queryParam for the REST service
     * @return url as string
     */
    private String buildExportUrl(String path, String queryParam) {
        String parametrizedPath = path.replace(PATH_PARAM_PLACEHOLDER, jobId);
        return endpoint + "/" + parametrizedPath + "?" + JobStoreServiceConstants.QUERY_PARAM_FORMAT + "=" + queryParam;
    }

    /**
     * Sets the Job Diagnostic tab according to the supplied Job Model
     * @param jobModel The Job Model, where the list of Diagnostic data is taken
     */
    private void setDiagnosticModels(JobModel jobModel) {
        getView().jobDiagnosticTabContent.jobDiagnosticTable.setRowData(0, jobModel.getDiagnosticModels());
    }

    /**
     * Sets the note tab according to the supplied Job Model
     */
    private void setWorkflowNoteTab() {
        if(workflowNoteModel != null) {
            getView().workflowNoteTabContent.note.setText(workflowNoteModel.getDescription());
            getView().fixedColumnVisible = !workflowNoteModel.getAssignee().isEmpty();
        }
    }

    /*
     * =================== > Methods used for displaying item data and selecting item tabs < ===================
     */

    /**
     * This method deciphers which tab indexes should be available.
     * If a fatal diagnostic exists, we are only interested in viewing the item diagnostic tab.
     * If the job type is not ACCTEST, we are not interesting i viewing the next output post tab.
     * The tab index assigned will therefore vary depending on the item data input.
     *
     * @param itemModel The model containing the item data
     */
    private void populateItemTabIndexes(ItemModel itemModel) {
        tabIndexes.clear();
        if(itemModel.isDiagnosticFatal()) {
            tabIndexes.put(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT, 0);
        } else {
            tabIndexes.put(ItemsListView.JAVASCRIPT_LOG_TAB_CONTENT, 0);
            tabIndexes.put(ItemsListView.INPUT_POST_TAB_CONTENT, 1);
            tabIndexes.put(ItemsListView.OUTPUT_POST_TAB_CONTENT, 2);
            if(type.equals(JobModel.Type.ACCTEST)) {
                tabIndexes.put(ItemsListView.NEXT_OUTPUT_POST_TAB_CONTENT, 3);
                tabIndexes.put(ItemsListView.SINK_RESULT_TAB_CONTENT, 4);
                if(!itemModel.getDiagnosticModels().isEmpty()) {
                    tabIndexes.put(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT, 5);
                }
            } else {
                tabIndexes.put(ItemsListView.SINK_RESULT_TAB_CONTENT, 3);
                if(!itemModel.getDiagnosticModels().isEmpty()) {
                    tabIndexes.put(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT, 4);
                }
            }
        }
    }

    /**
     * This method adds item tabs to the list view.
     * @param listView The list view in question
     * @param itemModel The model containing the item data
     */
    private void addItemTabs(ItemsListView listView, ItemModel itemModel) {
        Texts texts = getTexts();
        if(tabIndexes.containsKey(ItemsListView.JAVASCRIPT_LOG_TAB_CONTENT)) {
            listView.detailedTabs.add(new JavascriptLogTabContent(texts, commonInjector.getLogStoreProxyAsync(), itemModel), texts.tab_JavascriptLog());
        }
        if(tabIndexes.containsKey(ItemsListView.INPUT_POST_TAB_CONTENT)) {
            listView.detailedTabs.add(new ItemTabContent(texts, commonInjector.getJobStoreProxyAsync(), itemModel, ItemModel.LifeCycle.PARTITIONING), texts.tab_PartitioningPost());
        }
        if(tabIndexes.containsKey(ItemsListView.OUTPUT_POST_TAB_CONTENT)) {
            listView.detailedTabs.add(new ItemTabContent(texts, commonInjector.getJobStoreProxyAsync(), itemModel, ItemModel.LifeCycle.PROCESSING), texts.tab_ProcessingPost());
        }
        if(tabIndexes.containsKey(ItemsListView.NEXT_OUTPUT_POST_TAB_CONTENT)) {
            listView.detailedTabs.add(new NextTabContent(texts, commonInjector.getJobStoreProxyAsync(), itemModel), texts.tab_NextOutputPost());
        }
        if(tabIndexes.containsKey(ItemsListView.SINK_RESULT_TAB_CONTENT)) {
            listView.detailedTabs.add(new ItemTabContent(texts, commonInjector.getJobStoreProxyAsync(), itemModel, ItemModel.LifeCycle.DELIVERING), texts.tab_DeliveringPost());
        }
        if(tabIndexes.containsKey(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT)) {
            setDiagnosticModels(listView, itemModel);
            listView.detailedTabs.add(listView.itemDiagnosticTabContent.itemDiagnosticTable, texts.tab_ItemDiagnostic());
        }
    }

    /**
     * This method decides which detailed tab should be default selected
     * @param listView The list view in question
     * @param itemModel The model containing the item data
     */
    private void selectItemTab(ItemsListView listView, ItemModel itemModel) {
        ItemModel.LifeCycle status = itemModel.getStatus();
        if(itemModel.isDiagnosticFatal()) {
            listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.ITEM_DIAGNOSTIC_TAB_CONTENT));
        } else {

            // Acceptance test job: Show sink result
            if(type == JobModel.Type.ACCTEST) {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.SINK_RESULT_TAB_CONTENT));
            }
            // Item failed in delivering: Show sink result
            else if (itemSearchType == ItemListCriteria.Field.STATE_FAILED && status == ItemModel.LifeCycle.DELIVERING) {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.SINK_RESULT_TAB_CONTENT));
            }
            // Item failed in processing: Show output post
            else if (itemSearchType == ItemListCriteria.Field.STATE_FAILED && status == ItemModel.LifeCycle.PROCESSING) {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.OUTPUT_POST_TAB_CONTENT));
            }
            // Item ignored in processing or delivering: Show output post
            else if (itemSearchType == ItemListCriteria.Field.STATE_IGNORED && (status == ItemModel.LifeCycle.PROCESSING || status == ItemModel.LifeCycle.DELIVERING)) {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.OUTPUT_POST_TAB_CONTENT));
            } else {
                listView.detailedTabs.selectTab(tabIndexes.get(ItemsListView.JAVASCRIPT_LOG_TAB_CONTENT));
            }
        }
        listView.detailedTabs.setVisible(true);
    }

    /**
     * Sets the Item Diagnostic tab according to the supplied Job Model
     * @param itemModel The Item Model, where the list of Diagnostic data is taken
     */
    private void setDiagnosticModels(ItemsListView listView, ItemModel itemModel) {
        listView.itemDiagnosticTabContent.itemDiagnosticTable.setRowData(0, itemModel.getDiagnosticModels());
    }

    /**
     * Prepares the view for display in case of no jobs returned from the underlying data-store.
     * 1) The method builds a new header containing the id of the sought out job.
     * 2) The method displays an empty job-info tab.
     * 3) The method sets an error message for the user.
     */
    private void prepareViewForJobNotFoundDisplay() {
        View view = getView();
        view.jobHeader.setText(constructJobHeaderText(jobId, EMPTY, EMPTY));
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
            getView().setErrorText(getTexts().error_CouldNotFetchJob());
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

    class JobNotificationsCallback implements AsyncCallback<List<JobNotification>> {
        @Override
        public void onFailure(Throwable throwable) {
            getView().setErrorText(getTexts().error_CouldNotFetchJobNotifications());
        }
        @Override
        public void onSuccess(List<JobNotification> jobNotifications) {
            if (jobNotifications != null) {
                setJobNotifications(jobNotifications);
            }
        }
    }

    protected class SetJobWorkflowNoteCallback extends FilteredAsyncCallback<JobModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(JobModel jobModel) {
            selectJobTab(jobModel);
        }
    }

    protected class SetItemWorkflowNoteCallback extends FilteredAsyncCallback<ItemModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(ItemModel itemModel) {}
    }

}