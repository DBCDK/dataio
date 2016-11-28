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

package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.gui.client.pages.sink.status.SinkStatusTable;
import dk.dbc.dataio.jobstore.types.SinkStatusSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SinkStatusModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private SinkStatusModelMapper (){}

    /**
     * Maps a list of SinkStatusSnapshot to a list of SinkStatusModel
     * @param sinkStatusSnapshots, the list of sinkStatusSnapshot
     * @return list of SinkStatusModels
     */
    public static List<SinkStatusTable.SinkStatusModel> toModel(List<SinkStatusSnapshot> sinkStatusSnapshots) {
        List<SinkStatusTable.SinkStatusModel> sinkStatusModels = new ArrayList<>(sinkStatusSnapshots.size());
        for (SinkStatusSnapshot sinkStatusSnapshot : sinkStatusSnapshots) {
            sinkStatusModels.add(toModel(sinkStatusSnapshot));
        }
        return sinkStatusModels;
    }

    /**
     * Maps a SinkStatusSnapshot to a SinkStatusModel
     * @param sinkStatusSnapshot, the sinkStatusSnapshot
     * @return SinkStatusModel
     */
    public static SinkStatusTable.SinkStatusModel toModel(SinkStatusSnapshot sinkStatusSnapshot) {
        return new SinkStatusTable.SinkStatusModel()
                .withSinkId(sinkStatusSnapshot.getSinkId())
                .withSinkType(sinkStatusSnapshot.getType().name())
                .withName(sinkStatusSnapshot.getName())
                .withOutstandingJobs(sinkStatusSnapshot.getNumberOfJobs())
                .withOutstandingChunks(sinkStatusSnapshot.getNumberOfChunks());
    }
}
