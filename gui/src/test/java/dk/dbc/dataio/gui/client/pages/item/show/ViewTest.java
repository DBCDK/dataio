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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMock;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.modelBuilders.DiagnosticModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.ItemModelBuilder;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


/**
* PresenterImpl unit tests
* <p/>
* The test methods of this class uses the following naming convention:
* <p/>
* unitOfWork_stateUnderTest_expectedBehavior
*/
@RunWith(GwtMockitoTestRunner.class)
public class ViewTest {

    @Mock ViewGinjector mockedViewInjector;
    @Mock Presenter mockedPresenter;
    @Mock Resources mockedResources;
    @Mock static ClickEvent mockedClickEvent;
    @Mock Widget mockedWidget;
    @Mock SelectionChangeEvent mockedSelectionChangeEvent;
    @Mock SingleSelectionModel mockedSelectionModel;
    @Mock ItemModel mockedItemModel;
    @Mock HandlerRegistration mockedHandlerRegistration;

    // Test Data
    private DiagnosticModel diagnosticModel = new DiagnosticModelBuilder().build();
    private ItemModel itemModel = new ItemModelBuilder().setLifeCycle(ItemModel.LifeCycle.DELIVERING).build();

    /*
     * The tested view contains nested UiBinder views: ViewWidget (a UiBinder view) instantiates three ItemsListView
     * (also UiBinder views).
     * Normally, GwtMockito assures, that all @UiField's will be mocked, but for some reason, the three ItemsListView
     * objects are not mocked ???
     * Therefore, we will manually mock the ItemsListView objects so we know, that they for sure are instantiated, whenever
     * ViewWidget is instantiated (ViewWidget calls methods in ItemsListView in its constructor)
     * But when instantiating ItemsListView manually, we also have to manually mock it's three UiFields: itemsTable,
     * itemsPager and detailedTabs.
     * This is done in the following:
     */

    @GwtMock ItemsListView mockedItemsList;
    @Mock CellTable mockedItemsTable;
    @GwtMock JobDiagnosticTabContent mockedJobDiagnosticTabContent;
    @Mock CellTable mockedJobDiagnosticTable;
    @Mock SimplePager mockedItemsPager;
    @Mock DecoratedTabPanel mockedDetailedTabs;
    @Before
    public void setupMockedUiFields() {
        mockedItemsList.itemsTable = mockedItemsTable;
        mockedItemsList.itemsPager = mockedItemsPager;
        mockedItemsList.detailedTabs = mockedDetailedTabs;
        mockedJobDiagnosticTabContent.jobDiagnosticTable = mockedJobDiagnosticTable;
    }


    // Subject Under Test
    private ConcreteView view;

    // Mocked Texts
    @Mock Texts mockedTexts;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuItems;
    final static String MOCKED_MENU_ITEMS = "Mocked Poster";
    final static String MOCKED_COLUMN_ITEM = "Mocked Post";
    final static String MOCKED_COLUMN_LEVEL = "Mocked Diagnostic level";
    final static String MOCKED_COLUMN_MESSAGE = "Mocked Diagnostic message";
    final static String MOCKED_COLUMN_STATUS = "Mocked Status";
    final static String MOCKED_ERROR_COULDNOTFETCHITEMS = "Mocked Det var ikke muligt at hente poster fra Job Store";
    final static String MOCKED_LABEL_BACK = "Mocked Tilbage til Joboversigten";
    final static String MOCKED_TEXT_ITEM = "Mocked Post";
    final static String MOCKED_TEXT_JOBID = "Mocked Job Id:";
    final static String MOCKED_TEXT_SUBMITTER = "Mocked Submitter:";
    final static String MOCKED_TEXT_SINK = "Mocked Sink:";
    final static String MOCKED_LIFECYCLE_PARTITIONING = "Mocked Partitioning";
    final static String MOCKED_LIFECYCLE_PROCESSING = "Mocked Processing";
    final static String MOCKED_LIFECYCLE_DELIVERING = "Mocked Delivering";
    final static String MOCKED_LIFECYCLE_DONE = "Mocked Done";
    final static String MOCKED_LIFECYCLE_UNKNOWN = "Mocked Ukendt Lifecycle";
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedMenuItems.menu_Items()).thenReturn(MOCKED_MENU_ITEMS);
        when(mockedTexts.column_Item()).thenReturn(MOCKED_COLUMN_ITEM);
        when(mockedTexts.column_Message()).thenReturn(MOCKED_COLUMN_MESSAGE);
        when(mockedTexts.column_Level()).thenReturn(MOCKED_COLUMN_LEVEL);
        when(mockedTexts.column_Status()).thenReturn(MOCKED_COLUMN_STATUS);
        when(mockedTexts.error_CouldNotFetchItems()).thenReturn(MOCKED_ERROR_COULDNOTFETCHITEMS);
        when(mockedTexts.label_Back()).thenReturn(MOCKED_LABEL_BACK);
        when(mockedTexts.text_Item()).thenReturn(MOCKED_TEXT_ITEM);
        when(mockedTexts.text_JobId()).thenReturn(MOCKED_TEXT_JOBID);
        when(mockedTexts.text_Submitter()).thenReturn(MOCKED_TEXT_SUBMITTER);
        when(mockedTexts.text_Sink()).thenReturn(MOCKED_TEXT_SINK);
        when(mockedTexts.lifecycle_Partitioning()).thenReturn(MOCKED_LIFECYCLE_PARTITIONING);
        when(mockedTexts.lifecycle_Processing()).thenReturn(MOCKED_LIFECYCLE_PROCESSING);
        when(mockedTexts.lifecycle_Delivering()).thenReturn(MOCKED_LIFECYCLE_DELIVERING);
        when(mockedTexts.lifecycle_Done()).thenReturn(MOCKED_LIFECYCLE_DONE);
        when(mockedTexts.lifecycle_Unknown()).thenReturn(MOCKED_LIFECYCLE_UNKNOWN);
    }


    // Testing starts here...
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {

        // Subject Under Test
        setupView();
        view.setupColumns(mockedItemsList);
        view.setupColumns(mockedJobDiagnosticTabContent);

        // Verify invocations
        verify(mockedItemsPager, times(3)).firstPage();
        verify(mockedItemsTable, times(1)).addColumn(isA(Column.class), eq(MOCKED_COLUMN_ITEM));
        verify(mockedItemsTable, times(1)).addColumn(isA(Column.class), eq(MOCKED_COLUMN_STATUS));
        verify(mockedItemsPager, times(1)).setDisplay(mockedItemsTable);
        verify(mockedJobDiagnosticTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_LEVEL));
        verify(mockedJobDiagnosticTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_MESSAGE));
    }

    @Test
    public void setSelectionEnabled_setSelectionEnabled_selectionsEnabled() {
        // Test setup
        setupView();

        // Subject Under Test
        view.setSelectionEnabled(true);

        // Verification
        verify(mockedItemsTable).setSelectionModel(view.allContext.selectionModel);
        verify(mockedItemsTable).setSelectionModel(view.failedContext.selectionModel);
        verify(mockedItemsTable).setSelectionModel(view.ignoredContext.selectionModel);
    }

    class ConcreteView extends View {
        Context context = new Context(mockedItemsList);
        SelectionChangeHandlerClass selectionChangeHandler = new SelectionChangeHandlerClass(context);

        public ConcreteView() {
            super(false);
            this.viewInjector = mockedViewInjector;
            context.handlerRegistration = mockedHandlerRegistration;
            context.selectionModel = mockedSelectionModel;
        }

        @Override
        Texts getTexts() {
            return mockedTexts;
        }
    }

    @Test
    public void selectionChangeHandlerClass_callEventHandler_verify() {
        // Test setup
        ConcreteView concreteView = setupViewConcrete();
        when(mockedSelectionModel.getSelectedObject()).thenReturn(mockedItemModel);
        concreteView.setPresenter(mockedPresenter);

        // Subject Under Test
        concreteView.selectionChangeHandler.onSelectionChange(mockedSelectionChangeEvent);

        // Verification
        verify(mockedPresenter).itemSelected(mockedItemsList, mockedItemModel);
    }


    @Test
    public void selectionChangeHandlerClass_callEventHandlerWithEmptySelection_verifyNoSelectionSetToPresenter() {
        // Test setup
        ConcreteView concreteView = setupViewConcrete();
        when(mockedSelectionModel.getSelectedObject()).thenReturn(null);
        concreteView.setPresenter(mockedPresenter);

        // Subject Under Test
        concreteView.selectionChangeHandler.onSelectionChange(mockedSelectionChangeEvent);

        // Verification
        verifyZeroInteractions(mockedPresenter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructItemColumn_call_correctlySetup() {
        // Test setup
        setupView();

        // Subject Under Test
        Column column = view.constructItemColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(itemModel), is(MOCKED_TEXT_ITEM + " " + itemModel.getItemNumber()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructStatusColumn_call_correctlySetup() {
        setupView();

        // Subject Under Test
        Column column = view.constructStatusColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(itemModel), is(MOCKED_LIFECYCLE_DELIVERING));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDiagnosticLevelColumn_call_correctlySetup() {
        // Test setup
        setupView();

        // Subject Under Test
        Column column = view.constructDiagnosticLevelColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(diagnosticModel), is(diagnosticModel.getLevel()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDiagnosticMessageColumn_call_correctlySetup() {
        // Test setup
        setupView();

        // Subject Under Test
        Column column = view.constructDiagnosticMessageColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(diagnosticModel), is(diagnosticModel.getMessage()));
    }

    private ConcreteView setupViewConcrete() {
        return new ConcreteView();
    }
    private void setupView() {
        view = new ConcreteView();
        view.viewInjector = mockedViewInjector;
    }
}
