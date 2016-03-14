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

package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowBinderModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    @Mock Presenter mockedPresenter;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock static ClickEvent mockedClickEvent;
    @Mock ViewGinjector mockedViewInjector;
    @Mock Cell.Context mockedContext;
    @Mock NativeEvent mockedEvent;


    // Test Data
    private FlowComponentModel flowComponentModel1 = new FlowComponentModelBuilder().build();

    private FlowModel flowModel1 = new FlowModelBuilder().setComponents(Collections.singletonList(flowComponentModel1)).build();
    private SinkModel sinkModel1 = new SinkModelBuilder().setName("SInam1").build();

    private FlowBinderModel flowBinderModel1 = new FlowBinderModelBuilder()
            .setName("FBnam1")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Collections.singletonList(new SubmitterModelBuilder().build()))
            .setSinkModel(sinkModel1).build();

    private FlowBinderModel flowBinderModel2 = new FlowBinderModelBuilder()
            .setName("FBnam2")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Collections.singletonList(new SubmitterModelBuilder().build()))
            .setSinkModel(sinkModel1).build();

    private FlowBinderModel flowBinderModelEmpty = new FlowBinderModelBuilder()
            .setName("FB No Subs")
            .setFlowModel(flowModel1)
            .setSubmitterModels(new ArrayList<>())
            .setSinkModel(sinkModel1).build();

    private FlowBinderModel flowBinderModelOneSubmitter = new FlowBinderModelBuilder()
            .setName("FB One Sub")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Collections.singletonList(new SubmitterModelBuilder().setName("Sub 1").setNumber("1234").build()))
            .setSinkModel(sinkModel1).build();

    private FlowBinderModel flowBinderModelTwoSubmitters = new FlowBinderModelBuilder()
            .setName("FB Two Subs")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Arrays.asList(
                    new SubmitterModelBuilder().setName("Sub 1").setNumber("1234").build(),
                    new SubmitterModelBuilder().setName("Sub 2").setNumber("2345").build() ))
            .setSinkModel(sinkModel1).build();

    private FlowBinderModel flowBinderModelThreeSubmitters = new FlowBinderModelBuilder()
            .setName("FB Three Subs")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Arrays.asList(
                    new SubmitterModelBuilder().setName("Sub 2").setNumber("2345").build(),
                    new SubmitterModelBuilder().setName("Sub 1").setNumber("1234").build(),
                    new SubmitterModelBuilder().setName("Sub 3").setNumber("3456").build() ))
            .setSinkModel(sinkModel1).build();

    private List<FlowBinderModel> flowBinderModels = Arrays.asList(flowBinderModel1, flowBinderModel2);

    // Subject Under Test
    private ViewConcrete view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_LABEL_FLOWBINDERS = "Mocked Text: Flowbinders";
    final static String MOCKED_COLUMNHEADER_NAME = "Mocked Text: Navn";
    final static String MOCKED_COLUMNHEADER_DESCRIPTION = "Mocked Text: Beskrivelse";
    final static String MOCKED_COLUMNHEADER_PACKAGING = "Mocked Text: Rammeformat";
    final static String MOCKED_COLUMNHEADER_FORMAT = "Mocked Text: Indholdsformat";
    final static String MOCKED_COLUMNHEADER_CHARSET = "Mocked Text: Tegnsæt";
    final static String MOCKED_COLUMNHEADER_DESTINATION = "Mocked Text: Destination";
    final static String MOCKED_COLUMNHEADER_RECORDSPLITTER = "Mocked Text: Recordssplitter";
    final static String MOCKED_COLUMNHEADER_SUBMITTERS = "Mocked Text: Submittere";
    final static String MOCKED_COLUMNHEADER_FLOW = "Mocked Text: Flow";
    final static String MOCKED_COLUMNHEADER_SINK = "Mocked Text: Sink";
    final static String MOCKED_COLUMNHEADER_QUEUE_PROVIDER = "Mocked Text: Queue provider";
    final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: Handling";
    final static String MOCKED_BUTTON_EDIT = "Mocked Text: Rediger";
    final static String MOCKED_TEXT_SUBMITTERS = "Mocked Text: submittere";


    @Before
    public void setupMockedTextsBehaviour() {

        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedMenuTexts.menu_FlowBinders()).thenReturn("Header Text");

        when(mockedTexts.label_FlowBinders()).thenReturn(MOCKED_LABEL_FLOWBINDERS);
        when(mockedTexts.columnHeader_Name()).thenReturn(MOCKED_COLUMNHEADER_NAME);
        when(mockedTexts.columnHeader_Description()).thenReturn(MOCKED_COLUMNHEADER_DESCRIPTION);
        when(mockedTexts.columnHeader_Packaging()).thenReturn(MOCKED_COLUMNHEADER_PACKAGING);
        when(mockedTexts.columnHeader_Format()).thenReturn(MOCKED_COLUMNHEADER_FORMAT);
        when(mockedTexts.columnHeader_Charset()).thenReturn(MOCKED_COLUMNHEADER_CHARSET);
        when(mockedTexts.columnHeader_Destination()).thenReturn(MOCKED_COLUMNHEADER_DESTINATION);
        when(mockedTexts.columnHeader_RecordSplitter()).thenReturn(MOCKED_COLUMNHEADER_RECORDSPLITTER);
        when(mockedTexts.columnHeader_Submitters()).thenReturn(MOCKED_COLUMNHEADER_SUBMITTERS);
        when(mockedTexts.columnHeader_Flow()).thenReturn(MOCKED_COLUMNHEADER_FLOW);
        when(mockedTexts.columnHeader_Sink()).thenReturn(MOCKED_COLUMNHEADER_SINK);
        when(mockedTexts.columnHeader_QueueProvider()).thenReturn(MOCKED_COLUMNHEADER_QUEUE_PROVIDER);
        when(mockedTexts.columnHeader_Action()).thenReturn(MOCKED_COLUMNHEADER_ACTION);
        when(mockedTexts.button_Edit()).thenReturn(MOCKED_BUTTON_EDIT);
        when(mockedTexts.text_Submitters()).thenReturn(MOCKED_TEXT_SUBMITTERS);
    }


    public class ViewConcrete extends View {
        public ViewConcrete() {
            super();
        }

        @Override
        public Texts getTexts() {
            return mockedTexts;
        }
    }


     // Testing starts here...

    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        setupView();

        // Verify invocations
        verify(view.flowBindersTable).addRangeChangeHandler(any(RangeChangeEvent.Handler.class));
        verify(view.flowBindersTable).setRowCount(anyInt(), eq(true));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_NAME));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESCRIPTION));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_PACKAGING));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FORMAT));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_CHARSET));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_DESTINATION));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_RECORDSPLITTER));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_SUBMITTERS));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FLOW));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_SINK));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_QUEUE_PROVIDER));
        verify(view.flowBindersTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_ACTION));
        verify(view.flowBindersTable).setSelectionModel(view.selectionModel);
        verify(view.flowBindersTable).addDomHandler(any(DoubleClickHandler.class), eq(DoubleClickEvent.getType()));
        verifyNoMoreInteractions(view.flowBindersTable);
    }



    @Test
    public void setFlowBinders_validData_dataSetupCorrect() {
        // Setup
        setupView();

        List<FlowBinderModel> models = view.dataProvider.getList();

        assertThat(models.isEmpty(), is(true));

        // Subject Under Test
        view.setFlowBinders(flowBinderModels);

        assertThat(models.isEmpty(), is(false));
        assertThat(models.size(), is(2));
        assertThat(models.get(0).getName(), is(flowBinderModel1.getName()));
        assertThat(models.get(1).getName(), is(flowBinderModel2.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructNameColumn_call_correctlySetup() {
        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDescriptionColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructDescriptionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getDescription()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructPackagingColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructPackagingColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getPackaging()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFormatColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructFormatColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getFormat()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructCharsetColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructCharsetColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getCharset()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructDestinationColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructDestinationColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getDestination()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructRecordSplitterColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructRecordSplitterColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getRecordSplitter()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSubmittersColumn_call_correctlySetup() {

        // See also the detailed test of SubmitterColumn class below

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructSubmittersColumn();

        // Test that correct getValue handler has been setup - remember format: "name (number)"
        SafeHtml cellValue = (SafeHtml) column.getValue(flowBinderModel1);
        assertThat(cellValue.asString(),
                is(flowBinderModel1.getSubmitterModels().get(0).getNumber() + " ("
                        + flowBinderModel1.getSubmitterModels().get(0).getName() + ")"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructFlowColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getFlowModel().getFlowName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSinkColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructSinkColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getSinkModel().getSinkName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructQueueProviderColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructQueueProviderColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(flowBinderModel1.getQueueProvider()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructActionColumn_call_correctlySetup() {

        // Setup
        setupView();

        // Subject Under Test
        Column column = view.constructActionColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(flowBinderModel1), is(mockedTexts.button_Edit()));

        // Test that the right action is activated upon click
        view.setPresenter(mockedPresenter);
        FieldUpdater fieldUpdater = column.getFieldUpdater();
        fieldUpdater.update(334, flowBinderModel1, "Updated Button Text");  // Simulate a click on the column
        verify(mockedPresenter).editFlowBinder(flowBinderModel1);
    }

    @Test(expected = NullPointerException.class)
    public void submitterColumn_getValue_nullSubmitters_exception() {

        // Setup
        ViewConcrete.SubmitterColumn submitterColumn = new ViewConcrete().new SubmitterColumn();

        // Test subject under test
        submitterColumn.getValue(null);  // Throws exception
    }

    @Test
    public void submitterColumn_getValue_noSubmitters_ok() {

        // Setup
        ViewConcrete.SubmitterColumn submitterColumn = new ViewConcrete().new SubmitterColumn();

        // Test subject under test
        SafeHtml value = submitterColumn.getValue(flowBinderModelEmpty);

        // Verify test
        assertThat(value.asString(), is(""));
    }

    @Test
    public void submitterColumn_getValue_oneSubmitter_ok() {

        // Setup
        ViewConcrete.SubmitterColumn submitterColumn = new ViewConcrete().new SubmitterColumn();

        // Test subject under test
        SafeHtml value = submitterColumn.getValue(flowBinderModelOneSubmitter);

        // Verify test
        assertThat(value.asString(), is("1234 (Sub 1)"));
    }

    @Test
    public void submitterColumn_getValue_twoSubmitters_ok() {

        // Setup
        ViewConcrete.SubmitterColumn submitterColumn = new ViewConcrete().new SubmitterColumn();

        // Test subject under test
        SafeHtml value = submitterColumn.getValue(flowBinderModelTwoSubmitters);

        // Verify test
        assertThat(value.asString(), is("<a href='javascript:;'>2 Mocked Text: submittere</a>"));
    }

    @Test
    public void submitterColumn_getValue_threeSubmitters_ok() {

        // Setup
        ViewConcrete.SubmitterColumn submitterColumn = new ViewConcrete().new SubmitterColumn();

        // Test subject under test
        SafeHtml value = submitterColumn.getValue(flowBinderModelThreeSubmitters);

        // Verify test
        assertThat(value.asString(), is("<a href='javascript:;'>3 Mocked Text: submittere</a>"));
    }

    @Test
    public void submitterColumn_onBrowserEvent_notClick_displayNothing() {

        // Setup
        setupView();
        ViewConcrete.SubmitterColumn submitterColumn = view.new SubmitterColumn();
        when(mockedEvent.getType()).thenReturn("clickX");

        // Test subject under test
        ((Column) submitterColumn).onBrowserEvent(mockedContext, null, flowBinderModelThreeSubmitters, mockedEvent);

        // Verify test
        verifyNoMoreInteractions(view.popupList);
    }

    @Test(expected = NullPointerException.class)
    public void submitterColumn_onBrowserEvent_nullSubmitters_throw() {

        // Setup
        setupView();
        ViewConcrete.SubmitterColumn submitterColumn = view.new SubmitterColumn();
        when(mockedEvent.getType()).thenReturn("click");

        // Test subject under test
        ((Column) submitterColumn).onBrowserEvent(mockedContext, null, null, mockedEvent);
    }

    @Test
    public void submitterColumn_onBrowserEvent_noSubmitters_displayNothing() {

        // Setup
        setupView();
        ViewConcrete.SubmitterColumn submitterColumn = view.new SubmitterColumn();
        when(mockedEvent.getType()).thenReturn("click");

        // Test subject under test
        ((Column) submitterColumn).onBrowserEvent(mockedContext, null, flowBinderModelEmpty, mockedEvent);

        // Verify test
        verifyNoMoreInteractions(view.popupList);
    }

    @Test
    public void submitterColumn_onBrowserEvent_oneSubmitters_displayNothing() {

        // Setup
        setupView();
        ViewConcrete.SubmitterColumn submitterColumn = view.new SubmitterColumn();
        when(mockedEvent.getType()).thenReturn("click");

        // Test subject under test
        ((Column) submitterColumn).onBrowserEvent(mockedContext, null, flowBinderModelOneSubmitter, mockedEvent);

        // Verify test
        verifyNoMoreInteractions(view.popupList);
    }

    @Test
    public void submitterColumn_onBrowserEvent_twoSubmitters_displayDialogBox() {

        // Setup
        setupView();
        ViewConcrete.SubmitterColumn submitterColumn = view.new SubmitterColumn();
        when(mockedEvent.getType()).thenReturn("click");

        // Test subject under test
        ((Column) submitterColumn).onBrowserEvent(mockedContext, null, flowBinderModelTwoSubmitters, mockedEvent);

        // Verify test
        InOrder inOrder = inOrder(view.popupList);
        verify(view.popupList).clear();
        inOrder.verify(view.popupList).addItem("1234 (Sub 1)", "1234");
        inOrder.verify(view.popupList).addItem("2345 (Sub 2)", "2345");
        verify(view.popupList).show();
        verifyNoMoreInteractions(view.popupList);
    }

    @Test
    public void submitterColumn_onBrowserEvent_threeSubmitters_displayDialogBox() {

        // Setup
        setupView();
        ViewConcrete.SubmitterColumn submitterColumn = view.new SubmitterColumn();
        when(mockedEvent.getType()).thenReturn("click");

        // Test subject under test
        ((Column) submitterColumn).onBrowserEvent(mockedContext, null, flowBinderModelThreeSubmitters, mockedEvent);

        // Verify test
        InOrder inOrder = inOrder(view.popupList);
        verify(view.popupList).clear();
        inOrder.verify(view.popupList).addItem("1234 (Sub 1)", "1234");
        inOrder.verify(view.popupList).addItem("2345 (Sub 2)", "2345");
        inOrder.verify(view.popupList).addItem("3456 (Sub 3)", "3456");
        verify(view.popupList).show();
        verifyNoMoreInteractions(view.popupList);
    }


    /*
     * Private methods
     */
    private void setupView() {
        view = new ViewConcrete();
        view.viewInjector = mockedViewInjector;
    }
}
