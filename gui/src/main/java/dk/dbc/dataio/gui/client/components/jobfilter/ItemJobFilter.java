/*
 *
 *  * DataIO - Data IO
 *  * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 *  * Denmark. CVR: 15149043
 *  *
 *  * This file is part of DataIO.
 *  *
 *  * DataIO is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * DataIO is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

/**
 * This is the Item Filter
 */
public class ItemJobFilter extends BaseJobFilter {
    interface ItemJobFilterUiBinder extends UiBinder<HTMLPanel, ItemJobFilter> {
    }

    private static ItemJobFilterUiBinder ourUiBinder = GWT.create(ItemJobFilterUiBinder.class);

    @SuppressWarnings("unused")
    @UiConstructor
    public ItemJobFilter() {
        this("", false);
    }

    ItemJobFilter(String parameter, boolean invertFilter) {
        this(GWT.create(Texts.class), GWT.create(Resources.class), parameter, invertFilter);
    }

    ItemJobFilter(Texts texts, Resources resources, String parameter, boolean invertFilter) {
        super(texts, resources, invertFilter);
        initWidget(ourUiBinder.createAndBindUi(this));
        setParameter(invertFilter, parameter);
    }

    @UiField PromptedTextBox item;


    /**
     * Event handler for handling changes in the submitter value
     * @param event The ValueChangeEvent
     */
    @UiHandler("item")
    @SuppressWarnings("unused")
    void itemValueChanged(ValueChangeEvent<String> event) {
        filterChanged();
    }

    /**
     * Gets the name of the job filter
     * @return the name of the job filter
     */
    @Override
    public String getName() {
        return texts.itemFilter_name();
    }

    /**
     *  Gets the current value of the job filter
     * @return the current value of the filter
     */
    @Override
    public JobListCriteria getValue() {
        String value = item.getValue();
        if (value == null || value.isEmpty() ) return new JobListCriteria();

        return new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.RECORD_ID, ListFilter.Op.IN, value));
    }

    /**
     * Sets the selection according to the key value, setup in the parameter attribute<br>
     * The value is given in url as a plain integer, as the submitter number
     * @param filterParameter The filter parameters to be used by this job filter
     */
    @Override
    public void localSetParameter(String filterParameter) {
        if (!filterParameter.isEmpty()) {
            item.setValue(filterParameter, true);
        }
    }

    /**
     * Gets the parameter value for the filter
     * @return The stored filter parameter for the specific job filter
     */
    @Override
    public String getParameter() {
        return item.getValue();
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus at a time, and the widget that does will receive all keyboard events.
     * @param focused whether this widget should take focus or release it
     */
    @Override
    public void setFocus(boolean focused) {
        item.setFocus(focused);
    }


    /**
     * Adds a changehandler to the job filter
     * @param changeHandler the changehandler
     * @return a Handler Registration object
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return item.addChangeHandler( changeHandler );
    }


}