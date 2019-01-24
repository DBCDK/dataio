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

package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.place.shared.PlaceController;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

/**
 *
 */
public class PresenterJobsImpl extends PresenterImpl {
    /**
     * Default constructor
     *
     * @param placeController   PlaceController for navigation
     * @param globalJobsView    Global Jobs View, necessary for keeping filter state etc.
     * @param header            Breadcrumb header text
     */
    public PresenterJobsImpl(PlaceController placeController, View globalJobsView, String header) {
        super(placeController, globalJobsView, header);
    }

    /**
     * Abstract Methods
     *
     *
     */
    @Override
    protected void updateBaseQuery() {
        JobListCriteria criteria=new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"INFOMEDIA\"}"));
        view.dataProvider.setBaseCriteria( criteria );
    }
}
