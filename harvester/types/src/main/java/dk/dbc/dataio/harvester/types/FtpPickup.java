package dk.dbc.dataio.harvester.types;

import java.util.Objects;

public class FtpPickup extends Pickup {
    private String ftpPort = "21";
    private String ftpHost;
    private String ftpUser;
    private String ftpPassword;
    private String ftpSubdirectory;

    public FtpPickup() {
        super();
    }

    public FtpPickup withFtpPort(String ftpPort) {
        this.ftpPort = ftpPort;
        return this;
    }

    public FtpPickup withFtpHost(String ftpHost) {
        this.ftpHost = ftpHost;
        return this;
    }

    public FtpPickup withFtpUser(String ftpUser) {
        this.ftpUser = ftpUser;
        return this;
    }

    public FtpPickup withFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
        return this;
    }

    public FtpPickup withFtpSubdirectory(String ftpSubdirectory) {
        this.ftpSubdirectory = ftpSubdirectory;
        return this;
    }

    public String getFtpPort() {
        return ftpPort;
    }

    public String getFtpHost() {
        return ftpHost;
    }

    public String getFtpUser() {
        return ftpUser;
    }

    public String getFtpPassword() {
        return ftpPassword;
    }

    public String getFtpSubdirectory() {
        return ftpSubdirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FtpPickup that = (FtpPickup) o;

        return Objects.equals(ftpPort, that.ftpPort) &&
                Objects.equals(ftpHost, that.ftpHost) &&
                Objects.equals(ftpUser, that.ftpUser) &&
                Objects.equals(ftpPassword, that.ftpPassword) &&
                Objects.equals(ftpSubdirectory, that.ftpSubdirectory);
    }

    @Override
    public int hashCode() {
        int result = ftpPort != null ? ftpPort.hashCode() : 0;
        result = 31 * result + (ftpHost != null ? ftpHost.hashCode() : 0);
        result = 31 * result + (ftpUser != null ? ftpUser.hashCode() : 0);
        result = 31 * result + (ftpPassword != null ? ftpPassword.hashCode() : 0);
        result = 31 * result + (ftpSubdirectory != null ? ftpSubdirectory.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FtpPickup{" +
                "ftpPort='" + ftpPort + '\'' +
                ", ftpHost='" + ftpHost + '\'' +
                ", ftpUser='" + ftpUser + '\'' +
                ", ftpPassword='" + (ftpPassword == null ? "<none>" : ftpPassword.replaceAll(".*", "*****")) + '\'' +
                ", ftpSubdirectory='" + ftpSubdirectory + '\'' +
                '}';
    }

}
