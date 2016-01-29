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

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "reordereditem")
public class ReorderedItemEntity {
     /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

    @EmbeddedId
    private Key key;

    @Convert(converter = ChunkItemConverter.class)
    private ChunkItem chunkItem;

    @Convert(converter = RecordInfoConverter.class)
    private MarcRecordInfo recordInfo;

    public Key getKey() {
        return key;
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public ChunkItem getChunkItem() {
        return chunkItem;
    }

    public void setChunkItem(ChunkItem chunkItem) {
        this.chunkItem = chunkItem;
    }

    public MarcRecordInfo getRecordInfo() {
        return recordInfo;
    }

    public void setRecordInfo(MarcRecordInfo recordInfo) {
        this.recordInfo = recordInfo;
    }

    @Embeddable
    public static class Key {
        @Column(name = "jobid")
        private int jobId;

        @Column(name = "seqno")
        private int seqno;

        /* Private constructor in order to keep class static */
        private Key() {}

        public Key(int jobId, int seqno) {
            this.jobId = jobId;
            this.seqno = seqno;
        }

        public int getJobId() {
            return jobId;
        }

        public int getSeqno() {
            return seqno;
        }

        @Override
        public String toString() {
            return "Key{" +
                    "jobId=" + jobId +
                    ", seqno=" + seqno +
                    '}';
        }
    }
}
