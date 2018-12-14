/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.MarcRecordInfo;

import javax.persistence.EntityManager;

/**
 * The responsibility of this class is to ensure that volume records are
 * returned after their parents and possibly grandparents and in the reverse
 * order for deletions.
 *
 * This class assumes it is called in a transactional context with regards to
 * the given entity manager.
 *
 * This class is not thread safe.
 */
public class VolumeAfterParents extends JobItemReorderer {
    public VolumeAfterParents(int jobId, EntityManager entityManager) {
        super(jobId, entityManager);
    }

    @Override
    SortOrder getReorderedItemSortOrder(MarcRecordInfo recordInfo) {
        switch (recordInfo.getType()) {
            case VOLUME:
                if (recordInfo.isDelete()) {
                    return SortOrder.VOLUME_DELETE;
                }
                return SortOrder.VOLUME;
            case SECTION:
                if (recordInfo.isDelete()) {
                    return SortOrder.SECTION_DELETE;
                }
                return SortOrder.SECTION;
            default:
                if (recordInfo.isDelete()) {
                    return SortOrder.HEAD_DELETE;
                }
                return SortOrder.HEAD;
        }
    }
}
