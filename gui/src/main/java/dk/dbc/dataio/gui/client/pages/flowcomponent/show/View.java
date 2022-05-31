package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.LinkedList;
import java.util.List;

public class View extends ViewWidget {
    final static String FLOWCOMPONENT_MACRO_NAME = "FLOWCOMPONENT";
    final static String SVN_REVISION_MACRO = "REVISION";

    ListDataProvider<FlowComponentModel> dataProvider;
    SingleSelectionModel<FlowComponentModel> selectionModel = new SingleSelectionModel<>();

    public View() {
        super("");
        setupColumns();
    }

    /**
     * This method is used to put data into the view
     *
     * @param flowComponentModels The list of flowcomponents to put into the view
     */
    public void setFlowComponents(List<FlowComponentModel> flowComponentModels) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(flowComponentModels);
    }

    /**
     * Private methods
     */

    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setFlowComponents method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(flowComponentsTable);

        Texts texts = getTexts();
        flowComponentsTable.addColumn(constructNameColumn(), texts.columnHeader_Name());
        flowComponentsTable.addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        flowComponentsTable.addColumn(constructJavaScriptNameColumn(), texts.columnHeader_ScriptName());
        flowComponentsTable.addColumn(constructInvocationMethodColumn(), texts.columnHeader_InvocationMethod());
        flowComponentsTable.addColumn(constructSvnProjectColumn(), texts.columnHeader_Project());
        flowComponentsTable.addColumn(constructSvnRevisionColumn(), texts.columnHeader_Revision());
        flowComponentsTable.addColumn(constructSvnNextColumn(), texts.columnHeader_Next());
        flowComponentsTable.addColumn(constructActionColumn(), texts.columnHeader_Action());
        flowComponentsTable.setSelectionModel(selectionModel);
        flowComponentsTable.addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    Column constructNameColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getName();
            }
        };
    }

    /**
     * This method constructs the Description column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Description column
     */
    Column constructDescriptionColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getDescription();
            }
        };
    }

    /**
     * This method constructs the Script Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Script Name column
     */
    Column constructJavaScriptNameColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getInvocationJavascript();
            }
        };
    }

    /**
     * This method constructs the Invocation Method column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Invocation Method column
     */
    Column constructInvocationMethodColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getInvocationMethod();
            }
        };
    }

    /**
     * This method constructs the SVN Project column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed SVN Project column
     */
    Column constructSvnProjectColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getSvnProject();
            }
        };
    }

    /**
     * This method constructs the SVN Revision column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed SVN Revision column
     */
    Column constructSvnRevisionColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getSvnRevision();
            }
        };
    }

    /**
     * This method constructs the Next SVN Revision column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Next SVN Revision column
     */
    Column constructSvnNextColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getSvnNext();
            }
        };
    }

    /**
     * This method constructs the Action column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Action column
     */
    @SuppressWarnings("unchecked")
    Column constructActionColumn() {
        List<HasCell<FlowComponentModel, ?>> cells = new LinkedList<>();
        cells.add(new ActionHasCell(getTexts().button_ShowJSModules(), this::showJsModules));
        cells.add(new ActionHasCell(getTexts().button_Edit(), model -> presenter.editFlowComponent(model)));
        return new Column<FlowComponentModel, FlowComponentModel>(new CompositeCell<>(cells)) {
            @Override
            public FlowComponentModel getValue(FlowComponentModel model) {
                return model;
            }

            @Override
            public String getCellStyleNames(Cell.Context context, FlowComponentModel object) {
                return "button-cell";  // To allow css to place button horisontally
            }
        };
    }

    void showJsModules(FlowComponentModel model) {
        jsModulesPopup.setValue(jsModulesPopup.new DoubleListData(
                Format.macro(getTexts().header_SVNRevision(), SVN_REVISION_MACRO, model.getSvnRevision()),
                model.getJavascriptModules(),
                Format.macro(getTexts().header_SVNNextRevision(), SVN_REVISION_MACRO, model.getSvnNext()),
                model.getNextJavascriptModules()
        ));
        jsModulesPopup.setDialogTitle(Format.macro(getTexts().header_JSModulesListPopup(), FLOWCOMPONENT_MACRO_NAME, model.getName()));
        jsModulesPopup.show();
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     *
     * @return the double click handler
     */
    private DoubleClickHandler getDoubleClickHandler() {
        return doubleClickEvent -> {
            FlowComponentModel selected = selectionModel.getSelectedObject();
            if (selected != null) {
                presenter.editFlowComponent(selected);
            }
        };
    }


    /*
     * Local classes
     */

    private class ActionHasCell implements HasCell<FlowComponentModel, FlowComponentModel> {
        private ActionCell<FlowComponentModel> cell;

        public ActionHasCell(String text, ActionCell.Delegate<FlowComponentModel> delegate) {
            cell = new ActionCell<>(text, delegate);
        }

        @Override
        public Cell<FlowComponentModel> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<FlowComponentModel, FlowComponentModel> getFieldUpdater() {
            return null;
        }

        @Override
        public FlowComponentModel getValue(FlowComponentModel object) {
            return object;
        }
    }

}
