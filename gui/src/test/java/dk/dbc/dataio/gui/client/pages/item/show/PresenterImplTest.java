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


import com.google.gwt.dom.client.Style;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.Range;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.JobNotificationPanel;
import dk.dbc.dataio.gui.client.components.prompted.PromptedAnchor;
import dk.dbc.dataio.gui.client.components.prompted.PromptedHyperlink;
import dk.dbc.dataio.gui.client.components.prompted.PromptedLabel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.StateModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.modelBuilders.DiagnosticModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.ItemModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.WorkflowNoteModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.jobstore.test.types.JobNotificationBuilder;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.StateElement;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    @Mock View mockedView;
    @Mock ItemsListView mockedItemsListView;
    @Mock Widget mockedViewWidget;
    @Mock Throwable mockedException;
    @Mock Place mockedPlace;

    @Mock SimplePager mockedItemsPager;
    @Mock Texts mockedText;
    @Mock Label mockedJobHeader;
    @Mock CellTable mockedItemsTable;
    @Mock CellTable mockedJobDiagnosticTable;
    @Mock CellTable mockedItemDiagnosticTable;
    @Mock CellTable mockedStacktraceTable;
    @Mock HTMLPanel mockedAllItemsListTab;
    @Mock HTMLPanel mockedFailedItemsListTab;
    @Mock HTMLPanel mockedIgnoredItemsListTab;
    @Mock DecoratedTabPanel mockedDecoratedTabPanel;
    @Mock DecoratedTabPanel mockedDetailedTabs;
    @Mock JobInfoTabContent mockedJobInfoTabContent;
    @Mock JobDiagnosticTabContent mockedJobDiagnosticTabContent;
    @Mock JobNotificationsTabContent mockedJobNotificationTabContent;
    @Mock ItemDiagnosticTabContent mockedItemDiagnosticTabContent;
    @Mock WorkflowNoteTabContent mockedWorkflowNoteTabContent;
    @Mock TextArea mockedTextArea;
    @Mock PromptedLabel mockedPackaging;
    @Mock PromptedLabel mockedFormat;
    @Mock PromptedLabel mockedCharset;
    @Mock PromptedLabel mockedDestination;
    @Mock PromptedLabel mockedMailForNotificationAboutVerification;
    @Mock PromptedLabel mockedMailForNotificationAboutProcessing;
    @Mock PromptedLabel mockedResultMailInitials;
    @Mock PromptedLabel mockedType;
    @Mock PromptedLabel mockedJobCreationTime;
    @Mock PromptedLabel mockedJobCompletionTime;
    @Mock PromptedHyperlink mockedPreviousJobId;
    @Mock Label mockedExportLinksHeader;
    @Mock PromptedLabel mockedExportLinkItemFailedInPartitioning;
    @Mock PromptedLabel mockedExportLinkItemFailedInProcessing;
    @Mock PromptedLabel mockedExportLinkItemFailedInDelivering;
    @Mock PromptedAnchor mockedFileStore;
    @Mock HTMLPanel mockedAncestrySection;
    @Mock PromptedLabel mockedAncestryTransFile;
    @Mock PromptedLabel mockedAncestryDataFile;
    @Mock PromptedLabel mockedAncestryBatchId;
    @Mock InlineHTML mockedAncestryContent;
    @Mock TabBar mockedTabBar;
    @Mock AsyncItemViewDataProvider mockedDataProvider;
    @Mock JobStoreProxyAsync mockedJobStoreProxy;
    @Mock LogStoreProxyAsync mockedLogStoreProxy;
    @Mock ViewGinjector mockedViewInjector;
    @Mock Element mockedElement;
    @Mock Style mockedStyle;

    private final static int OFFSET = 0;
    private final static int ROW_COUNT = 4;
    private final static String MOCKED_TEXT_JOBID = "Mocked Job Id:";
    private final static String MOCKED_TEXT_SUBMITTER = "Mocked Submitter:";
    private final static String MOCKED_TEXT_SINK = "Mocked Sink:";
    private final static String MOCKED_TAB_JAVASCRIPTLOG = "Mocked Javascript log";
    private final static String MOCKED_TAB_PARTITIONINGPOST = "Input post";
    private final static String MOCKED_TAB_PROCESSINGPOST = "Output post";
    private final static String MOCKED_TAB_DELIVERINGPOST = "Sinkresultat";
    private final static String MOCKED_TAB_NEXT_OUTPUTPOST = "Mocked next output post";
    private final static String MOCKED_TAB_ITEM_DIAGNOSTIC = "Mocked item diagnostic result";

    @Before
    public void setupMockedTexts() {
        when(mockedText.text_JobId()).thenReturn(MOCKED_TEXT_JOBID);
        when(mockedText.text_Submitter()).thenReturn(MOCKED_TEXT_SUBMITTER);
        when(mockedText.text_Sink()).thenReturn(MOCKED_TEXT_SINK);
        when(mockedText.tab_JavascriptLog()).thenReturn(MOCKED_TAB_JAVASCRIPTLOG);
        when(mockedText.tab_PartitioningPost()).thenReturn(MOCKED_TAB_PARTITIONINGPOST);
        when(mockedText.tab_ProcessingPost()).thenReturn(MOCKED_TAB_PROCESSINGPOST);
        when(mockedText.tab_DeliveringPost()).thenReturn(MOCKED_TAB_DELIVERINGPOST);
        when(mockedText.tab_NextOutputPost()).thenReturn(MOCKED_TAB_NEXT_OUTPUTPOST);
        when(mockedText.tab_ItemDiagnostic()).thenReturn(MOCKED_TAB_ITEM_DIAGNOSTIC);
    }

    @Before
    public void setupMockedData() {
        when(mockedCommonGinjector.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
        when(mockedViewInjector.getView()).thenReturn(mockedView);
        when(mockedCommonGinjector.getLogStoreProxyAsync()).thenReturn(mockedLogStoreProxy);

        mockedView.jobHeader = mockedJobHeader;
        when(mockedJobHeader.getElement()).thenReturn(mockedElement);
        when(mockedElement.getStyle()).thenReturn(mockedStyle);
        mockedItemsListView.itemsTable = mockedItemsTable;
        mockedView.jobDiagnosticTabContent = mockedJobDiagnosticTabContent;
        mockedView.jobNotificationsTabContent = mockedJobNotificationTabContent;
        mockedView.workflowNoteTabContent = mockedWorkflowNoteTabContent;
        mockedView.workflowNoteTabContent.note = mockedTextArea;

        mockedView.itemsPager = mockedItemsPager;
        mockedView.allItemsListTab = mockedAllItemsListTab;
        mockedView.failedItemsListTab = mockedFailedItemsListTab;
        mockedView.ignoredItemsListTab = mockedIgnoredItemsListTab;
        mockedItemsListView.itemDiagnosticTabContent = mockedItemDiagnosticTabContent;
        mockedItemsListView.itemDiagnosticTabContent.itemDiagnosticTable = mockedItemDiagnosticTable;
        mockedItemsListView.itemDiagnosticTabContent.stacktraceTable = mockedStacktraceTable;

        mockedView.jobDiagnosticTabContent.jobDiagnosticTable = mockedJobDiagnosticTable;
        mockedItemsListView.detailedTabs = mockedDetailedTabs;

        mockedView.itemsListView = mockedItemsListView;

        mockedView.tabPanel = mockedDecoratedTabPanel;
        when(mockedDecoratedTabPanel.getTabBar()).thenReturn(mockedTabBar);
        when(mockedDetailedTabs.getTabBar()).thenReturn(mockedTabBar);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedView.itemsListView.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));

        mockedView.jobInfoTabContent = mockedJobInfoTabContent;
        mockedView.jobInfoTabContent.packaging = mockedPackaging;
        mockedView.jobInfoTabContent.format = mockedFormat;
        mockedView.jobInfoTabContent.charset = mockedCharset;
        mockedView.jobInfoTabContent.destination = mockedDestination;
        mockedView.jobInfoTabContent.mailForNotificationAboutVerification = mockedMailForNotificationAboutVerification;
        mockedView.jobInfoTabContent.mailForNotificationAboutProcessing = mockedMailForNotificationAboutProcessing;
        mockedView.jobInfoTabContent.resultMailInitials = mockedResultMailInitials;
        mockedView.jobInfoTabContent.type = mockedType;
        mockedView.jobInfoTabContent.jobCreationTime = mockedJobCreationTime;
        mockedView.jobInfoTabContent.jobCompletionTime = mockedJobCompletionTime;
        mockedView.jobInfoTabContent.previousJobId = mockedPreviousJobId;
        mockedView.jobInfoTabContent.exportLinksHeader = mockedExportLinksHeader;
        mockedView.jobInfoTabContent.exportLinkItemsFailedInPartitioning = mockedExportLinkItemFailedInPartitioning;
        mockedView.jobInfoTabContent.exportLinkItemsFailedInProcessing = mockedExportLinkItemFailedInProcessing;
        mockedView.jobInfoTabContent.exportLinkItemsFailedInDelivering = mockedExportLinkItemFailedInDelivering;
        mockedView.jobInfoTabContent.ancestrySection = mockedAncestrySection;
        mockedView.jobInfoTabContent.fileStore = mockedFileStore;
        mockedView.jobInfoTabContent.ancestryTransFile = mockedAncestryTransFile;
        mockedView.jobInfoTabContent.ancestryDataFile = mockedAncestryDataFile;
        mockedView.jobInfoTabContent.ancestryBatchId = mockedAncestryBatchId;
        mockedView.jobInfoTabContent.ancestryContent = mockedAncestryContent;
        mockedView.dataProvider = mockedDataProvider;
    }

    // Subject Under Test
    private PresenterImplConcrete presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        ItemsListView itemsListView;
        JobsCallback getJobsCallback;
        JobNotificationsCallback getJobNotificationCallback;
        SetJobWorkflowNoteCallback getSetJobWorkflowNoteCallback;
        SetItemWorkflowNoteCallback getSetItemWorkflowNoteCallback;

        public PresenterImplConcrete(Place place, PlaceController placeController, ItemsListView itemsListView) {
            super(place, placeController, mockedView, "");
            this.itemsListView = itemsListView;
            this.getJobsCallback = new JobsCallback();
            this.getJobNotificationCallback = new JobNotificationsCallback();
            this.getSetJobWorkflowNoteCallback = new SetJobWorkflowNoteCallback();
            this.getSetItemWorkflowNoteCallback = new SetItemWorkflowNoteCallback();
        }

        public PresenterImplConcrete(Place place, PlaceController placeController, String header) {
            super(place, placeController, mockedView, header);

        }

        @Override
        Texts getTexts() {
            return mockedText;
        }

        void setUrlDataioFilestoreRs() {
            urlDataioFilestoreRs = "url";
        }
    }

    // Test Data
    private ItemModel testModelDelivering = new ItemModelBuilder().setItemNumber("11").setItemId("1001").setChunkId("1111")
            .setLifeCycle(ItemModel.LifeCycle.DELIVERING).setDiagnosticModels(Collections.singletonList(new DiagnosticModelBuilder().build())).build();

    private ItemModel testModelProcessing = new ItemModelBuilder().setItemNumber("14").setItemId("1004").setChunkId("1114")
            .setLifeCycle(ItemModel.LifeCycle.PROCESSING).setDiagnosticModels(Collections.singletonList(new DiagnosticModelBuilder().build())).build();

    private ItemModel testModelFatalError = new ItemModelBuilder().setHasDiagnosticFatal(true)
            .setDiagnosticModels(Collections.singletonList(new DiagnosticModelBuilder().setLevel("FATAL").build())).build();


    private JobModel testJobModelSucceeded = new JobModel()
            .withJobId("JobSucceeded")
            .withType(JobSpecification.Type.TRANSIENT)
            .withWorkflowNoteModel(new WorkflowNoteModelBuilder().build())
            .withAncestry(new JobSpecification.Ancestry().withTransfile("transfile").withDetails("details".getBytes()).withDatafile("datafile").withBatchId("batchid").withHarvesterToken("harvesterToken"));

    private JobModel testJobModelFailed = new JobModel()
            .withJobId("JobFailed")
            .withNumberOfItems(2)
            .withStateModel(new StateModel().withProcessing(new StateElement().withFailed(1).withSucceeded(1)))
            .withType(JobSpecification.Type.TEST);

    private JobModel testJobModelIgnored = new JobModel()
            .withJobId("JobIgnored2")
            .withNumberOfItems(2)
            .withStateModel(new StateModel().withPartitioning(new StateElement().withIgnored(1)))
            .withType(JobSpecification.Type.ACCTEST).withWorkflowNoteModel(new WorkflowNoteModelBuilder().build());

    private JobNotification testJobNotificationCompleted = new JobNotificationBuilder().setType(JobNotification.Type.JOB_COMPLETED).
            setStatus(JobNotification.Status.COMPLETED).build();

    private JobNotification testJobNotificationFailed = new JobNotificationBuilder().setType(JobNotification.Type.JOB_CREATED).
            setStatus(JobNotification.Status.FAILED).build();

    // Tests start here

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        setupPresenterImpl();

        // Verify Test
        verify(mockedPlace).getParameter(Place.JOB_ID);
        verify(mockedPlace).getParameter(Place.RECORD_ID);
        verifyNoMoreInteractions(mockedPlace);
    }

    @Test
    public void start_callStart_ok() {
        setupPresenterImpl();

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verifyNoMoreInteractions(mockedContainerWidget);
        verify(mockedItemsTable).setRowCount(0);
        verifyNoMoreInteractions(mockedItemsTable);
        verify(mockedJobDiagnosticTable).setRowCount(0);
        verifyNoMoreInteractions(mockedJobDiagnosticTable);
        verify(mockedTabBar).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedTabBar).getTab(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT);
        verify(mockedTabBar).getTab(ViewWidget.WORKFLOW_NOTE_TAB_CONTENT);
        verifyNoMoreInteractions(mockedTabBar);
        verify(mockedJobStoreProxy).listJobs(any(JobListCriteria.class), any(PresenterImpl.JobsCallback.class));
        verify(mockedJobStoreProxy).listJobNotificationsForJob(any(Integer.class), any(PresenterImpl.JobNotificationsCallback.class));
        verifyNoMoreInteractions(mockedJobStoreProxy);
    }

    @Test
    public void allItemsTabSelected_callAllItemsTabSelected_allItemsRequested() {
        setupPresenterImpl();

        // Subject under test
        presenterImpl.allItemsTabSelected();

        // Verification
        verify(mockedView.dataProvider).setBaseCriteria(eq(ItemListCriteria.Field.JOB_ID), any(ItemListCriteria.class));
        verify(mockedDetailedTabs).clear();
        verify(mockedDetailedTabs).setVisible(false);
        verifyNoMoreInteractions(mockedDetailedTabs);

    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedItemsTabSelected_callFailedItemsTabSelected_failedItemsRequested() {
        setupPresenterImpl();

        // Subject under test
        presenterImpl.failedItemsTabSelected();

        // Verification
        verify(mockedView.dataProvider).setBaseCriteria(eq(ItemListCriteria.Field.STATE_FAILED), any(ItemListCriteria.class));
        verify(mockedDetailedTabs).clear();
        verify(mockedDetailedTabs).setVisible(false);
        verifyNoMoreInteractions(mockedDetailedTabs);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ignoredItemsTabSelected_callIgnoredItemsTabSelected_ignoredItemsRequested() {
        setupPresenterImpl();

        // Subject under test
        presenterImpl.ignoredItemsTabSelected();

        // Verification
        verify(mockedView.dataProvider).setBaseCriteria(eq(ItemListCriteria.Field.STATE_IGNORED), any(ItemListCriteria.class));
        verify(mockedDetailedTabs).clear();
        verify(mockedDetailedTabs).setVisible(false);
        verifyNoMoreInteractions(mockedDetailedTabs);
    }

    @Test
    public void noteTabSelectedForExistingNote_callNoteTabSelected_ok() {
        setupPresenterImpl();
        presenterImpl.workflowNoteModel = new WorkflowNoteModelBuilder().build();

        // Subject under test
        presenterImpl.noteTabSelected();

        // Verify Test
        verify(mockedWorkflowNoteTabContent.note).setFocus(true);
        verify(mockedWorkflowNoteTabContent.note).setCursorPos(0);
    }

    @Test
    public void itemSelected_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.itemSearchType = ItemListCriteria.Field.JOB_ID;
        presenterImpl.type = JobSpecification.Type.PERSISTENT;

        // Subject under test
        presenterImpl.itemSelected(mockedItemsListView, testModelDelivering);

        // Verify Test
        // Default tab index for jobs is: javascript log
        genericMockedDetailedTabsAssert(false, true, false, 0);
    }

    @Test
    public void itemSelected_itemFailedWithFatalDiagnostic_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;

        // Subject under test
        presenterImpl.itemSelected(mockedItemsListView, testModelFatalError);

        // Verify Test
        // Expected tab index for jobs that have fatal diagnostics is: item diagnostic
        genericMockedDetailedTabsAssert(true, true, false, 0);
    }

    @Test
    public void itemSelected_itemFailedInDelivering_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;

        // Subject under test
        presenterImpl.itemSelected(mockedItemsListView, testModelDelivering);

        // Verify Test
        // Expected tab index for jobs that are failed in delivering is: sink result
        genericMockedDetailedTabsAssert(false, true, false, 3);
    }

    @Test
    public void itemSelected_itemFailedInProcessing_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;

        // Subject under test
        presenterImpl.itemSelected(mockedItemsListView, testModelProcessing);

        // Verify Test
        // Expected tab index for jobs that are failed in processing is: output post
        genericMockedDetailedTabsAssert(false, true, false, 2);
    }

    @Test
    public void itemSelected_itemIgnoredInProcessing_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_IGNORED;

        // Subject under test
        presenterImpl.itemSelected(mockedItemsListView, testModelProcessing);

        // Verify Test
        // Expected tab index for jobs that are ignored in processing is: output post
        genericMockedDetailedTabsAssert(false, true, false, 2);
    }

    @Test
    public void itemSelected_itemIgnoredInDelivering_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_IGNORED;

        // Subject under test
        presenterImpl.itemSelected(mockedItemsListView, testModelDelivering);

        // Verify Test
        // Expected tab index for jobs that are ignored in delivering is: output post
        genericMockedDetailedTabsAssert(false, true, false, 2);
    }

    @Test
    public void itemSelected_callItemSelectedForAcceptanceTestJob_ok() {
        setupPresenterImpl();
        presenterImpl.itemSearchType = ItemListCriteria.Field.JOB_ID;
        presenterImpl.type = JobSpecification.Type.ACCTEST;

        // Subject under test
        presenterImpl.itemSelected(mockedItemsListView, testModelDelivering);

        // Verify Test
        // Default tab index for acceptance-test jobs is: sink result
        genericMockedDetailedTabsAssert(false, true, true, 4);
    }

    private void genericMockedDetailedTabsAssert(boolean isFatal, boolean hasDiagnostics, boolean isAccTest, int selectedTabIndex) {
        verify(mockedDetailedTabs).clear();
        if(isFatal) {
            verify(mockedDetailedTabs).add(any(ItemDiagnosticTabContent.class), eq(MOCKED_TAB_ITEM_DIAGNOSTIC));
            verify(mockedDetailedTabs, times(0)).add(any(JavascriptLogTabContent.class), eq(MOCKED_TAB_JAVASCRIPTLOG));
            verify(mockedDetailedTabs, times(0)).add(any(ItemTabContent.class), eq(MOCKED_TAB_PARTITIONINGPOST));
            verify(mockedDetailedTabs, times(0)).add(any(ItemTabContent.class), eq(MOCKED_TAB_PROCESSINGPOST));
            verify(mockedDetailedTabs, times(0)).add(any(NextTabContent.class), eq(MOCKED_TAB_NEXT_OUTPUTPOST));
            verify(mockedDetailedTabs, times(0)).add(any(ItemTabContent.class), eq(MOCKED_TAB_DELIVERINGPOST));

        } else {
            verify(mockedDetailedTabs).add(any(JavascriptLogTabContent.class), eq(MOCKED_TAB_JAVASCRIPTLOG));
            verify(mockedDetailedTabs).add(any(ItemTabContent.class), eq(MOCKED_TAB_PARTITIONINGPOST));
            verify(mockedDetailedTabs).add(any(ItemTabContent.class), eq(MOCKED_TAB_PROCESSINGPOST));
            if(isAccTest) {
                verify(mockedDetailedTabs).add(any(NextTabContent.class), eq(MOCKED_TAB_NEXT_OUTPUTPOST));
            } else {
                verify(mockedDetailedTabs, times(0)).add(any(NextTabContent.class), eq(MOCKED_TAB_NEXT_OUTPUTPOST));
            }
            verify(mockedDetailedTabs).add(any(ItemTabContent.class), eq(MOCKED_TAB_DELIVERINGPOST));
            if(hasDiagnostics){
                verify(mockedDetailedTabs).add(any(ItemDiagnosticTabContent.class), eq(MOCKED_TAB_ITEM_DIAGNOSTIC));
            } else {
                verify(mockedDetailedTabs, times(0)).add(any(ItemDiagnosticTabContent.class), eq(MOCKED_TAB_ITEM_DIAGNOSTIC));
            }
        }
        verify(mockedDetailedTabs).selectTab(selectedTabIndex);
        verify(mockedDetailedTabs).setVisible(true);
    }

    // Test JobsCallback
    @Test
    public void getJob_callbackWithError_errorMessageInView() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void getJob_callbackWithSuccessAndFailedJobs_jobFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.allItemCounter = 2;

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(Collections.singletonList(testJobModelFailed));

        // Verify Test
        verify(mockedDecoratedTabPanel).selectTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);

        // 2 times because all are being hidden through hideJobTabs() and only the relevant tabs are enabled again
        verify(mockedTabBar, times(2)).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.WORKFLOW_NOTE_TAB_CONTENT);
    }

    @Test
    public void setJobInfoTabContent_withDetailsAncestry_jobInfoTabContentPopulated() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.setJobInfoTabContent(mockedView, testJobModelSucceeded);

        verify(mockedPackaging).setText(testJobModelSucceeded.getPackaging());
        verify(mockedFormat).setText(testJobModelSucceeded.getFormat());
        verify(mockedCharset).setText(testJobModelSucceeded.getCharset());
        verify(mockedDestination).setText(testJobModelSucceeded.getDestination());
        verify(mockedMailForNotificationAboutVerification).setText(testJobModelSucceeded.getMailForNotificationAboutVerification());
        verify(mockedMailForNotificationAboutProcessing).setText(testJobModelSucceeded.getMailForNotificationAboutProcessing());
        verify(mockedResultMailInitials).setText(testJobModelSucceeded.getResultMailInitials());
        verify(mockedType).setText(testJobModelSucceeded.getType().name());
        verify(mockedJobCreationTime).setText(testJobModelSucceeded.getJobCreationTime());
        verify(mockedJobCompletionTime).setText(testJobModelSucceeded.getJobCompletionTime());
        verify(mockedPreviousJobId).setVisible(false);
        verify(mockedAncestrySection).setVisible(true);
    }

    @Test
    public void setExportLinks_expectedExportLinksDisplayed() {
        final JobModel jobModel = new JobModel().withFormat("marc2").withStateModel(new StateModel()
                .withPartitioning(new StateElement().withFailed(1))
                .withDelivering(new StateElement().withFailed(1)));

        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.setExportLinks(mockedView, jobModel);

        // Verify Test
        verify(mockedExportLinksHeader).setVisible(true);
        verify(mockedExportLinkItemFailedInPartitioning).setVisible(true);
        verify(mockedExportLinkItemFailedInPartitioning).setText(anyString());
        verifyNoMoreInteractions(mockedExportLinkItemFailedInPartitioning);

        verifyNoMoreInteractions(mockedExportLinkItemFailedInProcessing);

        verify(mockedExportLinkItemFailedInDelivering).setVisible(true);
        verify(mockedExportLinkItemFailedInDelivering).setText(anyString());
        verifyNoMoreInteractions(mockedExportLinkItemFailedInDelivering);
    }

    @Test
    public void getJob_callbackWithSuccessAndIgnoredJobs_jobFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(Collections.singletonList(testJobModelIgnored));

        // Verify Test
        verify(mockedDecoratedTabPanel).selectTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);

        // 2 times because all are being hidden through hideJobTabs() and only the relevant tabs are enabled again
        verify(mockedTabBar, times(2)).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.WORKFLOW_NOTE_TAB_CONTENT);

        verify(mockedTabBar, times(1)).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT);
    }

    @Test
    public void getJob_onSuccess_jobHeaderSetCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(Collections.singletonList(testJobModelSucceeded));

        // Verification
        verify(mockedView.jobHeader).setText(buildHeaderText(testJobModelSucceeded.getJobId(), testJobModelSucceeded.getSubmitterNumber(), testJobModelSucceeded.getSinkName()));
    }

    @Test
    public void getJob_onSuccess_ancestryDataIsSetCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(Collections.singletonList(testJobModelSucceeded));

        // Verification
        verify(mockedAncestrySection, times(1)).setVisible(true);
        verifyNoMoreInteractions(mockedAncestrySection);
        verify(mockedAncestryTransFile).setText(testJobModelSucceeded.getTransFileAncestry());
        verify(mockedAncestryTransFile).setVisible(true);
        verifyNoMoreInteractions(mockedAncestryTransFile);
        verify(mockedAncestryDataFile).setText(testJobModelSucceeded.getDataFileAncestry());
        verify(mockedAncestryDataFile).setVisible(true);
        verifyNoMoreInteractions(mockedAncestryDataFile);
        verify(mockedAncestryBatchId).setText(testJobModelSucceeded.getBatchIdAncestry());
        verify(mockedAncestryBatchId).setVisible(true);
        verifyNoMoreInteractions(mockedAncestryBatchId);
        verify(mockedAncestryContent).setText(testJobModelSucceeded.getDetailsAncestry());
        verify(mockedAncestryContent).setVisible(true);
        verifyNoMoreInteractions(mockedAncestryContent);
    }

    @Test
    public void getJob_onSuccess_exportLinksHidden() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(Collections.singletonList(testJobModelSucceeded));

        // Verification
        verify(mockedExportLinksHeader).setVisible(false);
        verify(mockedExportLinkItemFailedInPartitioning).setVisible(false);
        verify(mockedExportLinkItemFailedInProcessing).setVisible(false);
        verify(mockedExportLinkItemFailedInDelivering).setVisible(false);
    }

    @Test
    public void getJob_callbackWithSuccessAndSuccessfulJobs_jobFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(Collections.singletonList(testJobModelSucceeded));

        // Verification
        verify(mockedDecoratedTabPanel).selectTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedView.jobHeader).setText(contains("Mocked Job Id: " + testJobModelSucceeded.getJobId()));
    }

    @Test
    public void getJob_callbackWithSuccessAndMultipleJobs_firstJobFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(Arrays.asList(testJobModelIgnored, testJobModelSucceeded));

        // Verification
        verify(mockedView.jobHeader).setText(contains("Mocked Job Id: " + testJobModelIgnored.getJobId()));

    }

    @Test
    public void getJob_callbackWithSuccessAndNoJobs_noJobFetched() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(Collections.emptyList());

        // Verify Test
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void getJobNotifications_callbackWithError_errorMessageInView() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobNotificationCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void getJobNotifications_callbackWithSuccessAndNoNotifications_noJobNotificationsFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobNotificationCallback.onSuccess(Collections.emptyList());

        // Verify Test
        verify(mockedJobNotificationTabContent, times(1)).clear();
        verify(mockedJobNotificationTabContent, times(1)).getNotificationsCount();
        verifyNoMoreInteractions(mockedJobNotificationTabContent);
    }

    @Test
    public void getJobNotifications_callbackWithSuccessAndTwoNotifications_twoJobNotificationsFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobNotificationCallback.onSuccess(Arrays.asList(testJobNotificationCompleted, testJobNotificationFailed));

        // Verify Test
        verify(mockedJobNotificationTabContent, times(1)).clear();
        verify(mockedJobNotificationTabContent, times(2)).add(any(JobNotificationPanel.class));
        verify(mockedJobNotificationTabContent, times(1)).getNotificationsCount();
        verifyNoMoreInteractions(mockedJobNotificationTabContent);
    }

    @Test
    public void setWorkflowNoteForJob_inputIsValid_setWorkflowNoteCalled() {
        setupPresenterImplConcrete();
        presenterImpl.workflowNoteModel = new WorkflowNoteModelBuilder().build();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.setWorkflowNoteModel("Description");

        // Verify Test
        verify(mockedCommonGinjector.getJobStoreProxyAsync()).setWorkflowNote(
                eq(presenterImpl.workflowNoteModel),
                eq(Long.valueOf(presenterImpl.jobId).intValue()),
                any(AsyncCallback.class));
    }

    @Test
    public void setWorkflowNoteForJob_callbackWithError_errorMessageInView() {
        setupPresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getSetJobWorkflowNoteCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void setWorkflowNoteForItem_inputIsValid_setWorkflowNoteCalled() {
        setupPresenterImplConcrete();
        presenterImpl.workflowNoteModel = new WorkflowNoteModelBuilder().build();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.setWorkflowNoteModel(testModelDelivering, true);

        verify(mockedCommonGinjector.getJobStoreProxyAsync()).setWorkflowNote(
                any(WorkflowNoteModel.class),
                eq(Long.valueOf(testModelDelivering.getJobId()).intValue()),
                eq(Long.valueOf(testModelDelivering.getChunkId()).intValue()),
                eq(Long.valueOf(testModelDelivering.getItemId()).shortValue()),
                any(AsyncCallback.class));
    }

    @Test
    public void setWorkflowNoteForItem_callbackWithError_errorMessageInView() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getSetItemWorkflowNoteCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(anyString());

    }

    @Test
    public void setFileStoreUrl_nullAnchor_doNothing() {
        setupPresenterImplConcrete();
        final JobModel model = new JobModel().withDataFile(null);

        // Test Subject Under Test
        presenterImpl.setFileStoreUrl(null, model);

        // Verify Test
        verifyNoMoreInteractions(mockedFileStore);
    }

    @Test
    public void setFileStoreUrl_nullJobModel_setLinkInvisible() {
        setupPresenterImplConcrete();

        // Test Subject Under Test
        presenterImpl.setFileStoreUrl(mockedFileStore, null);

        // Verify Test
        verify(mockedFileStore).setVisible(false);
        verify(mockedFileStore).setVisible(true);
        verify(mockedFileStore).setText(anyString());
        verify(mockedFileStore).setHref(anyString());
        verifyNoMoreInteractions(mockedFileStore);
    }

    @Test
    public void setFileStoreUrl_nullDataFile_setLinkInvisible() {
        setupPresenterImplConcrete();
        final JobModel model = new JobModel().withDataFile(null);

        // Test Subject Under Test
        presenterImpl.setFileStoreUrl(mockedFileStore, model);

        // Verify Test
        verify(mockedFileStore).setVisible(false);
        verify(mockedFileStore).setVisible(true);
        verify(mockedFileStore).setText(anyString());
        verify(mockedFileStore).setHref(anyString());
        verifyNoMoreInteractions(mockedFileStore);
    }

    @Test
    public void setFileStoreUrl_emptyDataFile_setLinkInvisible() {
        setupPresenterImplConcrete();
        final JobModel model = new JobModel().withDataFile("");

        // Test Subject Under Test
        presenterImpl.setFileStoreUrl(mockedFileStore, model);

        // Verify Test
        verify(mockedFileStore).setVisible(false);
        verify(mockedFileStore).setVisible(true);
        verify(mockedFileStore).setText(anyString());
        verify(mockedFileStore).setHref(anyString());
        verifyNoMoreInteractions(mockedFileStore);
    }

    @Test
    public void setFileStoreUrl_invalidDataFile_setLinkInvisible() {
        setupPresenterImplConcrete();
        final JobModel model = new JobModel().withDataFile("a");

        // Test Subject Under Test
        presenterImpl.setFileStoreUrl(mockedFileStore, model);

        // Verify Test
        verify(mockedFileStore).setVisible(false);
        verify(mockedFileStore).setVisible(true);
        verify(mockedFileStore).setText(anyString());
        verify(mockedFileStore).setHref(anyString());
        verifyNoMoreInteractions(mockedFileStore);
    }

    @Test
    public void setFileStoreUrl_validDataFile_setLinkInvisible() {
        setupPresenterImplConcrete();
        presenterImpl.setUrlDataioFilestoreRs();
        final JobModel model = new JobModel().withDataFile("a:b:23");

        // Test Subject Under Test
        presenterImpl.setFileStoreUrl(mockedFileStore, model);

        // Verify Test
        verify(mockedFileStore).setHrefAndText("url/files/23");
        verify(mockedFileStore).setVisible(true);
        verifyNoMoreInteractions(mockedFileStore);
    }

    /* Private methods */

    private String buildHeaderText(String jobId, String submitterNumber, String sinkName) {
        return MOCKED_TEXT_JOBID + " " + jobId
                + ", " + MOCKED_TEXT_SUBMITTER + " " + submitterNumber
                + ", " + MOCKED_TEXT_SINK + " " + sinkName;
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImplConcrete(mockedPlace, mockedPlaceController, header);
        presenterImpl.viewInjector = mockedViewInjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.jobId = "1234";
    }

    private void setupPresenterImplConcrete() {
        presenterImpl = new PresenterImplConcrete(mockedPlace, mockedPlaceController, mockedItemsListView);
        presenterImpl.viewInjector = mockedViewInjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.jobId = "1234";
    }
}