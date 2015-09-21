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

package dk.dbc.dataio.gui.client.components.jobfilter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.inject.Inject;
import dk.dbc.dataio.gui.client.components.PromptedList;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.List;

/**
 * This is the Sink Job Filter
 */
public class SinkJobFilter extends BaseJobFilter {
    final String NO_SINK_ID_SELECTED = "0";  // Setting SinkId to zero means no filtering

    interface SinkJobFilterUiBinder extends UiBinder<HTMLPanel, SinkJobFilter> {
    }

    private static SinkJobFilterUiBinder ourUiBinder = GWT.create(SinkJobFilterUiBinder.class);

    FlowStoreProxyAsync flowStoreProxy;
    ChangeHandler sinkJobValueChangeHandler = null;


    @UiConstructor
    public SinkJobFilter() {
        this((Texts) GWT.create(Texts.class), (Resources) GWT.create(Resources.class), (FlowStoreProxyAsync) GWT.create(FlowStoreProxy.class));
    }

    @Inject
    public SinkJobFilter(Texts texts, Resources resources, FlowStoreProxyAsync flowStoreProxy) {
        super(texts, resources);
        this.flowStoreProxy = flowStoreProxy;
        initWidget(ourUiBinder.createAndBindUi(this));
        flowStoreProxy.findAllSinks(new FetchSinksCallback());
    }

    @UiField PromptedList sinkList;

    /**
     * Fetches the name of this filter
     * @return The name of the filter
     */
    @Override
    public String getName() {
        return texts.sinkFilter_name();
    }


    /*
     * Override HasChangeHandlers Interface Methods
     */
    @Override
    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        sinkJobValueChangeHandler = changeHandler;
        return sinkList.addChangeHandler(changeHandler);
    }

    /*
     * Private
     */

    /**
     * This class is the callback class for the findAllSinks method in the Flow Store
     */
    class FetchSinksCallback extends FilteredAsyncCallback<List<SinkModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
        }
        @Override
        public void onSuccess(List<SinkModel> models) {
            sinkList.addAvailableItem(texts.sinkFilter_ChooseASinkName(), NO_SINK_ID_SELECTED);
            for (SinkModel model: models) {
                sinkList.addAvailableItem(model.getSinkName(), String.valueOf(model.getId()));
            }
            sinkList.setEnabled(true);
        }
    }

    @Override
    public JobListCriteria getValue() {
        if( sinkList.getSelectedKey() == "0") return new JobListCriteria();

        return new JobListCriteria().where( new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, sinkList.getSelectedKey()));
    }

}