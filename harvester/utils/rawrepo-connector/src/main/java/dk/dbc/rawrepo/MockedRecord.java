package dk.dbc.rawrepo;

import java.util.Date;

public class MockedRecord implements Record {
    private final RecordId recordId;
    private final boolean isOriginal;
    private byte[] content;
    private Date created;
    private Date modified;

    public MockedRecord(RecordId recordId, boolean isOriginal) {
        this.recordId = recordId;
        this.isOriginal = isOriginal;
        content = null;
        created = modified = new Date();
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public boolean hasContent() {
        return content != null;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public RecordId getId() {
        return recordId;
    }

    @Override
    public Date getModified() {
        return modified;
    }

    @Override
    public boolean isOriginal() {
        return isOriginal;
    }

    @Override
    public void setContent(byte[] bytes) {
        content = bytes;
    }

    @Override
    public void setCreated(Date date) {
        created = date;
    }

    @Override
    public void setModified(Date date) {
        modified = date;
    }
}
