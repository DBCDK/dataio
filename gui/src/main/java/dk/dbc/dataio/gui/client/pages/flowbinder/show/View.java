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


import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.List;

/**
 * This class is the View class for the FlowBinders Show View
 */
public class View extends ViewWidget {
    final static String CLICKABLE_SUBMITTER_COLUMN_STYLE = "clickable-submitter-column-style";
    ListDataProvider<FlowBinderModel> dataProvider;
    SingleSelectionModel<FlowBinderModel> selectionModel = new SingleSelectionModel<>();



    public View() {
        super("");
        setupColumns();
    }


    /**
     * This method is used to put data into the view
     *
     * @param flowBinderModels The list of flowbinders to put into the view
     */
    public void setFlowBinders(List<FlowBinderModel> flowBinderModels) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(flowBinderModels);
    }


    /**
     * Private methods
     */


    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setFlowBinders method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(flowBindersTable);

        flowBindersTable.addColumn(constructNameColumn(), getTexts().columnHeader_Name());
        flowBindersTable.addColumn(constructDescriptionColumn(), getTexts().columnHeader_Description());
        flowBindersTable.addColumn(constructPackagingColumn(), getTexts().columnHeader_Packaging());
        flowBindersTable.addColumn(constructFormatColumn(), getTexts().columnHeader_Format());
        flowBindersTable.addColumn(constructCharsetColumn(), getTexts().columnHeader_Charset());
        flowBindersTable.addColumn(constructDestinationColumn(), getTexts().columnHeader_Destination());
        flowBindersTable.addColumn(constructRecordSplitterColumn(), getTexts().columnHeader_RecordSplitter());
        flowBindersTable.addColumn(constructSubmittersColumn(), getTexts().columnHeader_Submitters());
        flowBindersTable.addColumn(constructFlowColumn(), getTexts().columnHeader_Flow());
        flowBindersTable.addColumn(constructSinkColumn(), getTexts().columnHeader_Sink());
        flowBindersTable.addColumn(constructActionColumn(), getTexts().columnHeader_Action());
        flowBindersTable.setSelectionModel(selectionModel);
        flowBindersTable.addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    Column constructNameColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
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
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getDescription();
            }
        };
    }

    /**
     * This method constructs the Packaging column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Packaging column
     */
    Column constructPackagingColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getPackaging();
            }
        };
    }

    /**
     * This method constructs the Format column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Format column
     */
    Column constructFormatColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getFormat();
            }
        };
    }

    /**
     * This method constructs the Charset column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Charset column
     */
    Column constructCharsetColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getCharset();
            }
        };
    }

    /**
     * This method constructs the Destination column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Destination column
     */
    Column constructDestinationColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getDestination();
            }
        };
    }

    /**
     * This method constructs the RecordSplitter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed RecordSplitter column
     */
    Column constructRecordSplitterColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getRecordSplitter();
            }
        };
    }

    /**
     * This method constructs the Submitters column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Submitters column
     */
    Column constructSubmittersColumn() {
        return new SubmitterColumn();
    }

    /**
     * This method constructs the Flow column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Flow column
     */
    Column constructFlowColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getFlowModel().getFlowName();
            }
        };
    }

    /**
     * This method constructs the Sink column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Sink column
     */
    Column constructSinkColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getSinkModel().getSinkName();
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
        Column column = new Column<FlowBinderModel, String>(new ButtonCell()) {
            @Override
            public String getValue(FlowBinderModel model) {
                // The value to display in the button.
                return getTexts().button_Edit();
            }
        };
        column.setFieldUpdater(new FieldUpdater<FlowBinderModel, String>() {
            @Override
            public void update(int index, FlowBinderModel model, String buttonText) {
                presenter.editFlowBinder(model);
            }
        });
    return column;
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     * @return the double click handler
     */
    private DoubleClickHandler getDoubleClickHandler(){
        return new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent doubleClickEvent) {
                FlowBinderModel selected = selectionModel.getSelectedObject();
                if(selected != null) {
                    presenter.editFlowBinder(selected);
                }
            }
        };
    }


    /**
     * Private classes
     */
    class SubmitterColumn extends Column<FlowBinderModel, String> {
        public SubmitterColumn(Cell<String> cell) {
            super(cell);
        }

        public SubmitterColumn() {
            this(new ClickableTextCell());
        }

        @Override
        public String getValue(FlowBinderModel model) {
            return formatSubmitters(model.getSubmitterModels());
        }

        @Override
        public String getCellStyleNames(Cell.Context context, FlowBinderModel model) {
            List<SubmitterModel> models = model.getSubmitterModels();
            if (models != null && models.size() > 1) {
                return CLICKABLE_SUBMITTER_COLUMN_STYLE;
            } else {
                return "";
            }
        }

        @Override
        public void onBrowserEvent(Cell.Context context, Element elem, FlowBinderModel model, NativeEvent event) {
            super.onBrowserEvent(context, elem, model, event);
            if (isClickableColumn(model.getSubmitterModels()) && "click".equals(event.getType())) {
                showSubmittersInPopupList(model.getSubmitterModels());
            }
        }


        /*
         * Private methods
         */

        private boolean isEmptySubmitterColumn(List<SubmitterModel> models) {
            return models == null || models.size() == 0;
        }

        private boolean isClickableColumn(List<SubmitterModel> models) {
            return models != null && models.size() > 1;
        }

        private String formatSubmitters(List<SubmitterModel> models) {
            if (isEmptySubmitterColumn(models)) {
                return "";
            } else if (isClickableColumn(models)) {
                return models.size() + " " + getTexts().text_Submitters();
            } else {
                SubmitterModel model = models.get(0);
                return Format.inBracketsPairString(model.getNumber(), model.getName());
            }
        }

        private void showSubmittersInPopupList(List<SubmitterModel> submitters) {
            popupList.clear();
            for (SubmitterModel model: submitters) {
                popupList.add(Format.inBracketsPairString(model.getNumber(), model.getName()));
            }
            popupList.show();
        }
    }
}
