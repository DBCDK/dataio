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
        final byte[] copy = new byte[content.length];
        System.arraycopy(content, 0, copy, 0, content.length);
        return copy;
    }

    @Override
    public boolean hasContent() {
        return content != null;
    }

    @Override
    public Date getCreated() {
        return new Date(created.getTime());
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
    public void setContent(byte[] bytes) {
        content = new byte[bytes.length];
        System.arraycopy(bytes, 0, content, 0, bytes.length);
    }

    @Override
    public void setCreated(Date date) {
        created = new Date(date.getTime());
    }

    @Override
    public void setModified(Date date) {
        modified = new Date(date.getTime());
    }
}
