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

package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.List;

public class HarvestRecordsRequest extends HarvestRequest<HarvestRecordsRequest> {
    private static final long serialVersionUID = 5138529001705123811L;

    private final List<String> recordIds;
    private Integer basedOnJob;

    @JsonCreator
    public HarvestRecordsRequest(
            @JsonProperty("submitterNumber") long submitterNumber,
            @JsonProperty("recordIds") List<String> recordIds) throws NullPointerException {
        super(submitterNumber);
        this.recordIds = InvariantUtil.checkNotNullOrThrow(recordIds, "recordIds");
    }

    public List<String> getRecordIds() {
        return recordIds;
    }

    public HarvestRecordsRequest getBasedOnJob(Integer jobId) {
        basedOnJob = jobId;
        return this;
    }

    public Integer getBasedOnJob() {
        return basedOnJob;
    }
}
