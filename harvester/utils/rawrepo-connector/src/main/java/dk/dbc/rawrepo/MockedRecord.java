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

package dk.dbc.rawrepo;

import java.time.Instant;
import java.util.Arrays;

public class MockedRecord extends RecordData {
    private final RecordData.RecordId recordId;
    private final boolean isOriginal;
    private boolean isDeleted;
    private boolean isEnriched;
    private byte[] content;
    private String created;
    private String modified;
    private String mimeType;
    private String enrichmentTrail;
    private String trackingId;

    public MockedRecord(RecordData.RecordId recordId) {
        this(recordId, true);
    }

    public MockedRecord(RecordId recordId, boolean isOriginal) {
        this.recordId = recordId;
        this.isOriginal = isOriginal;
        this.isDeleted = false;
        this.isEnriched = false;
        this.mimeType = "mimeType";
        enrichmentTrail = null;
        trackingId = null;
        content = null;
        created = modified = Instant.now().toString();
    }

    @Override
    public byte[] getContent() {
        final byte[] copy = new byte[content.length];
        System.arraycopy(content, 0, copy, 0, content.length);
        return copy;
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean b) {
        isDeleted = b;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String s) {
        mimeType = s;
    }

    @Override
    public String getCreated() {
        if (created != null) {
            return created;
        }
        return null;
    }

    @Override
    public RecordId getId() {
        return recordId;
    }

    @Override
    public String getModified() {
        return modified;
    }

    public boolean isOriginal() {
        return isOriginal;
    }

    public boolean isEnriched() {
        return isEnriched;
    }

    public void setEnriched(boolean b) {
        isEnriched = b;
    }

    @Override
    public String getEnrichmentTrail() {
        return enrichmentTrail;
    }

    public void setEnrichmentTrail(String enrichmentTrail) {
        this.enrichmentTrail = enrichmentTrail;
    }

    public void setContent(byte[] bytes) {
        content = new byte[bytes.length];
        System.arraycopy(bytes, 0, content, 0, bytes.length);
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public void setModified(Instant modified) {
        this.modified = modified;
    }

    @Override
    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MockedRecord that = (MockedRecord) o;

        if (isOriginal != that.isOriginal) {
            return false;
        }
        if (isDeleted != that.isDeleted) {
            return false;
        }
        if (isEnriched != that.isEnriched) {
            return false;
        }
        if (!recordId.equals(that.recordId)) {
            return false;
        }
        if (!Arrays.equals(content, that.content)) {
            return false;
        }
        if (created != null ? !created.equals(that.created) : that.created != null) {
            return false;
        }
        if (modified != null ? !modified.equals(that.modified) : that.modified != null) {
            return false;
        }
        if (mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null) {
            return false;
        }
        return !(enrichmentTrail != null ? !enrichmentTrail.equals(that.enrichmentTrail) : that.enrichmentTrail != null);

    }

    @Override
    public int hashCode() {
        int result = recordId.hashCode();
        result = 31 * result + (isOriginal ? 1 : 0);
        result = 31 * result + (isDeleted ? 1 : 0);
        result = 31 * result + (isEnriched ? 1 : 0);
        result = 31 * result + (content != null ? Arrays.hashCode(content) : 0);
        result = 31 * result + (created != null ? created.hashCode() : 0);
        result = 31 * result + (modified != null ? modified.hashCode() : 0);
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        result = 31 * result + (enrichmentTrail != null ? enrichmentTrail.hashCode() : 0);
        return result;
    }
}
