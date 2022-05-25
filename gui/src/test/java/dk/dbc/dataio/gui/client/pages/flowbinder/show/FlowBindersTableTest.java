package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.popup.PopupListBox;
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
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


/**
 * FlowBindersTable unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class FlowBindersTableTest {

    @Mock
    ListDataProvider<FlowBinderModel> mockedDataProvider;
    @Mock
    List<FlowBinderModel> mockedFlowBindersList;
    @Mock
    Texts mockedTexts;
    @Mock
    View mockedView;
    @Mock
    Presenter mockedPresenter;
    @Mock
    Column mockedColumn;
    @Mock
    SingleSelectionModel mockedSelectionModel;


    // Test Data
    private FlowComponentModel flowComponentModel1 = new FlowComponentModelBuilder().build();

    private FlowModel flowModel1 = new FlowModelBuilder().setComponents(Collections.singletonList(flowComponentModel1)).build();
    private SinkModel sinkModel1 = new SinkModelBuilder().setName("SInam1").build();

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

    private FlowBinderModel flowBinderModel = new FlowBinderModelBuilder()
            .setName("FB Three Subs")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Arrays.asList(
                    new SubmitterModelBuilder().setName("Sub 2").setNumber("2345").build(),
                    new SubmitterModelBuilder().setName("Sub 1").setNumber("1234").build(),
                    new SubmitterModelBuilder().setName("Sub 3").setNumber("3456").build()))
            .setSinkModel(sinkModel1).build();

    @Before
    public void setupTexts() {
        when(mockedTexts.button_Create()).thenReturn("ButtonCreate");
        when(mockedTexts.button_Edit()).thenReturn("ButtonEdit");
        when(mockedTexts.text_Submitters()).thenReturn("Submittere");
    }

    @Test
    public void constructor_noData_emptyOk() {
        // Subject under test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);

        // Verify Test
        assertThat(flowBindersTable.getRowCount(), is(0));
    }

    @Test
    public void constructor_data_dataOk() {
        // Prepare test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);
        flowBindersTable.dataProvider = mockedDataProvider;
        when(mockedDataProvider.getList()).thenReturn(mockedFlowBindersList);
        List<FlowBinderModel> flowBinderModels = new ArrayList<>();
        flowBinderModels.add(new FlowBinderModelBuilder().setId(11).build());
        flowBinderModels.add(new FlowBinderModelBuilder().setId(22).build());

        // Subject under test
        flowBindersTable.setFlowBinders(flowBinderModels);

        // Verify Test
        verify(mockedDataProvider, times(2)).getList();
        verify(mockedFlowBindersList).clear();
        verify(mockedFlowBindersList).addAll(flowBinderModels);
        verifyNoMoreInteractions(mockedDataProvider);
        verifyNoMoreInteractions(mockedFlowBindersList);
    }

    @Test
    public void constructor_data_checkGetValueCallbacks() {
        // Prepare test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);
        flowBindersTable.texts = mockedTexts;

        // Subject Under Test
        assertThat(flowBindersTable.getColumnCount(), is(12));
        assertThat(flowBindersTable.getColumn(0).getValue(flowBinderModel), is("FB Three Subs"));
        assertThat(flowBindersTable.getColumn(1).getValue(flowBinderModel), is("description"));
        assertThat(flowBindersTable.getColumn(2).getValue(flowBinderModel), is("flowbinder-packaging"));
        assertThat(flowBindersTable.getColumn(3).getValue(flowBinderModel), is("flowbinder-format"));
        assertThat(flowBindersTable.getColumn(4).getValue(flowBinderModel), is("flowbinder-charset"));
        assertThat(flowBindersTable.getColumn(5).getValue(flowBinderModel), is("flowbinder-destination"));
        assertThat(flowBindersTable.getColumn(6).getValue(flowBinderModel), is("XML"));
        assertThat(flowBindersTable.getColumn(7).getValue(flowBinderModel).toString(), is("safe: \"<a href='javascript:;'>3 Submittere</a>\""));
        assertThat(flowBindersTable.getColumn(8).getValue(flowBinderModel), is("name"));
        assertThat(flowBindersTable.getColumn(9).getValue(flowBinderModel), is("SInam1"));
        assertThat(flowBindersTable.getColumn(10).getValue(flowBinderModel), is("queue-provider"));
        assertThat(flowBindersTable.getColumn(11).getValue(flowBinderModel), is("ButtonEdit"));
    }

    @Test
    public void getDoubleClickHandler_callback_editFlowBinderInPresenter() {
        // Prepare test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);
        flowBindersTable.setPresenter(mockedPresenter);
        flowBindersTable.setSelectionModel(mockedSelectionModel);
        when(mockedSelectionModel.getSelectedObject()).thenReturn(flowBinderModel);
        flowBindersTable.texts = mockedTexts;

        // Subject Under Test
        DoubleClickHandler clickHandler = flowBindersTable.getDoubleClickHandler();
        clickHandler.onDoubleClick(null);

        // Verify Test
        verify(mockedPresenter).editFlowBinder(flowBinderModel);
    }

    @Test
    public void SafeHtmlCell_render_renderCorrect() {
        // Prepare test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);
        FlowBindersTable.SafeHtmlCell cell = flowBindersTable.new SafeHtmlCell();
        SafeHtmlBuilder shBuilder = new SafeHtmlBuilder().appendEscaped("Test Data: ");

        // Subject Under Test
        cell.render(null, new SafeHtmlBuilder().appendEscaped("What?").toSafeHtml(), shBuilder);

        // Verify Test
        assertThat(shBuilder.toSafeHtml().toString(), is("safe: \"Test Data: What?\""));
    }

    @Test
    public void SubmitterColumn_getValueNoSubmitters_getCorrectValue() {
        // Prepare test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);
        flowBindersTable.texts = mockedTexts;
        FlowBindersTable.SubmitterColumn submitterColumn = flowBindersTable.new SubmitterColumn();

        // Subject Under Test
        SafeHtml value = submitterColumn.getValue(flowBinderModelEmpty);

        // Verify Test
        assertThat(value.toString(), is("safe: \"\""));
    }

    @Test
    public void SubmitterColumn_getValueOneSubmitter_getCorrectValue() {
        // Prepare test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);
        flowBindersTable.texts = mockedTexts;
        FlowBindersTable.SubmitterColumn submitterColumn = flowBindersTable.new SubmitterColumn();

        // Subject Under Test
        SafeHtml value = submitterColumn.getValue(flowBinderModelOneSubmitter);

        // Verify Test
        assertThat(value.toString(), is("safe: \"1234 (Sub 1)\""));
    }

    @Test
    public void SubmitterColumn_getValueThreeSubmitters_getCorrectValue() {
        // Prepare test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);
        flowBindersTable.texts = mockedTexts;
        FlowBindersTable.SubmitterColumn submitterColumn = flowBindersTable.new SubmitterColumn();

        // Subject Under Test
        SafeHtml value = submitterColumn.getValue(flowBinderModel);

        // Verify Test
        assertThat(value.toString(), is("safe: \"<a href='javascript:;'>3 Submittere</a>\""));
    }

    @Test
    public void SubmitterColumn_onBrowserEventNonClick_ok() {
        // Prepare test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);
        PopupListBox mockedPopupList = mock(PopupListBox.class);
        mockedView.popupList = mockedPopupList;
        flowBindersTable.texts = mockedTexts;
        FlowBindersTable.SubmitterColumn submitterColumn = flowBindersTable.new SubmitterColumn();
        Cell.Context mockedContext = mock(Cell.Context.class);
        Element mockedElement = mock(Element.class);
        NativeEvent mockedNativeEvent = mock(NativeEvent.class);
        when(mockedNativeEvent.getType()).thenReturn("non-click");

        // Subject Under Test
        submitterColumn.onBrowserEvent(mockedContext, mockedElement, flowBinderModel, mockedNativeEvent);

        // Verify Test
        verify(mockedContext).getIndex();
        verifyNoMoreInteractions(mockedContext);
        verifyNoMoreInteractions(mockedElement);
        verify(mockedNativeEvent, times(2)).getType();
        verifyNoMoreInteractions(mockedNativeEvent);
        verifyNoMoreInteractions(mockedPopupList);
    }

    @Test
    public void SubmitterColumn_onBrowserEventClick_ok() {
        // Prepare test
        FlowBindersTable flowBindersTable = new FlowBindersTable(mockedView);
        PopupListBox mockedPopupList = mock(PopupListBox.class);
        mockedView.popupList = mockedPopupList;
        flowBindersTable.texts = mockedTexts;
        FlowBindersTable.SubmitterColumn submitterColumn = flowBindersTable.new SubmitterColumn();
        Cell.Context mockedContext = mock(Cell.Context.class);
        Element mockedElement = mock(Element.class);
        NativeEvent mockedNativeEvent = mock(NativeEvent.class);
        when(mockedNativeEvent.getType()).thenReturn("click");

        // Subject Under Test
        submitterColumn.onBrowserEvent(mockedContext, mockedElement, flowBinderModel, mockedNativeEvent);

        // Verify Test
        verify(mockedContext).getIndex();
        verifyNoMoreInteractions(mockedContext);
        verifyNoMoreInteractions(mockedElement);
        verify(mockedNativeEvent, times(2)).getType();
        verifyNoMoreInteractions(mockedNativeEvent);
        verify(mockedPopupList).clear();
        verify(mockedPopupList).addItem("1234 (Sub 1)", "1234");
        verify(mockedPopupList).addItem("2345 (Sub 2)", "2345");
        verify(mockedPopupList).addItem("3456 (Sub 3)", "3456");
        verify(mockedPopupList).show();
        verifyNoMoreInteractions(mockedPopupList);
    }

}
