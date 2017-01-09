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

package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;

/**
 * This class is a parameter abstraction for the PgJobStore.addAccTestJob() method.
 * <p>
 * Parameter initialization failures will result in fatal diagnostics being added
 * to the internal diagnostics list, and the corresponding parameter field being
 * given a null value.
 * </p>
 */
public class AddAccTestJobParam extends AddJobParam {

    public AddAccTestJobParam(AccTestJobInputStream accTestJobInputStream, FlowStoreServiceConnector flowStoreServiceConnector) throws NullPointerException {
        super(accTestJobInputStream, flowStoreServiceConnector);
    }

    @Override
    protected Flow lookupFlow() {
        return ((AccTestJobInputStream) jobInputStream).getFlow();
    }

    @Override
    protected RecordSplitterConstants.RecordSplitter lookupTypeOfDataPartitioner () {
        return ((AccTestJobInputStream) jobInputStream).getTypeOfDataPartitioner();
    }
}
