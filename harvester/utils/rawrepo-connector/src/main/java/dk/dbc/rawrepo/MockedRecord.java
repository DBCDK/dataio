package dk.dbc.rawrepo;

import java.util.Arrays;
import java.util.Date;

public class MockedRecord implements Record {
    private final RecordId recordId;
    private final boolean isOriginal;
    private boolean isDeleted;
    private boolean isEnriched;
    private byte[] content;
    private Date created;
    private Date modified;
    private String mimeType;
    private String enrichmentTrail;

    public MockedRecord(RecordId recordId, boolean isOriginal) {
        this.recordId = recordId;
        this.isOriginal = isOriginal;
        this.isDeleted = false;
        this.isEnriched = false;
        this.mimeType = "mimeType";
        enrichmentTrail = null;
        content = null;
        created = modified = new Date();
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

    @Override
    public void setDeleted(boolean b) {
        isDeleted = b;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(String s) {
        mimeType = s;
    }

    @Override
    public Date getCreated() {
        if (created != null) {
            return new Date(created.getTime());
        }
        return null;
    }

    @Override
    public RecordId getId() {
        return recordId;
    }

    @Override
    public Date getModified() {
        return new Date(modified.getTime());
    }

    @Override
    public boolean isOriginal() {
        return isOriginal;
    }

    @Override
    public boolean isEnriched() {
        return isEnriched;
    }

    @Override
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

    @Override
    public void setContent(byte[] bytes) {
        content = new byte[bytes.length];
        System.arraycopy(bytes, 0, content, 0, bytes.length);
    }

    @Override
    public void setCreated(Date date) {
        if (date != null) {
            created = new Date(date.getTime());
        } else {
            created = null;
        }
    }

    @Override
    public void setModified(Date date) {
        modified = new Date(date.getTime());
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
