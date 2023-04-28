package dk.dbc.dataio.gui.client.model;

import java.util.Objects;

public class FtpFileModel extends GenericBackendModel{
    private String name;
    private String fileDate;
    private String fileSize;

    public String getName() {
        return name;
    }

    public String getFileDate() {
        return fileDate;
    }

    public String getFileSize() {
        return fileSize;
    }

    public FtpFileModel withName(String name) {
        this.name = name;
        return this;
    }

    public FtpFileModel withFileDate(String date) {
        this.fileDate = date;
        return this;
    }

    public FtpFileModel withFtpSize(String size) {
        this.fileSize = size;
        return this;
    }

    public FtpFileModel() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FtpFileModel that = (FtpFileModel) o;
        return Objects.equals(name, that.name) && Objects.equals(fileDate, that.fileDate) && Objects.equals(fileSize, that.fileSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fileDate, fileSize);
    }

    @Override
    public String toString() {
        return "FtpFileModel{" +
                "name='" + name + '\'' +
                ", fileDate='" + fileDate + '\'' +
                ", fileSize='" + fileSize + '\'' +
                '}';
    }
}
