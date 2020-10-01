package dk.dbc.dataio.harvester.types;

import java.util.Objects;

public class SFtpPickup extends Pickup {
    private String sFtpPort = "22";
    private String sFtpHost;
    private String sFtpUser;
    private String sFtpPassword;
    private String sFtpSubdirectory;

    public SFtpPickup() {
        super();
    }

    public SFtpPickup withSFtpPort(String sFtpPort) {
        this.sFtpPort = sFtpPort;
        return this;
    }

    public SFtpPickup withSFtpHost(String sFtpHost) {
        this.sFtpHost = sFtpHost;
        return this;
    }

    public SFtpPickup withSFtpuser(String sFtpUser) {
        this.sFtpUser = sFtpUser;
        return this;
    }

    public SFtpPickup withSFtpPassword(String sFtpPassword) {
        this.sFtpPassword = sFtpPassword;
        return this;
    }

    public SFtpPickup withSFtpSubdirectory(String sFtpSubdirectory) {
        this.sFtpSubdirectory = sFtpSubdirectory;
        return this;
    }

    public String getsFtpPort() {
        return sFtpPort;
    }

    public String getsFtpHost() {
        return sFtpHost;
    }

    public String getsFtpUser() {
        return sFtpUser;
    }

    public String getsFtpPassword() {
        return sFtpPassword;
    }

    public String getsFtpSubdirectory() {
        return sFtpSubdirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SFtpPickup that = (SFtpPickup) o;

        return Objects.equals(sFtpPort, that.sFtpPort) &&
                Objects.equals(sFtpHost, that.sFtpHost) &&
                Objects.equals(sFtpUser, that.sFtpUser) &&
                Objects.equals(sFtpPassword, that.sFtpPassword) &&
                Objects.equals(sFtpSubdirectory, that.sFtpSubdirectory);
    }


    @Override
    public int hashCode() {
        int result = sFtpPort != null ? sFtpPort.hashCode() : 0;
        result = 31 * result + sFtpHost.hashCode();
        result = 31 * result + sFtpUser.hashCode();
        result = 31 * result + sFtpPassword.hashCode();
        result = 31 * result + (sFtpSubdirectory != null ? sFtpSubdirectory.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SFtpPickup{" +
                "sFtpPort='" + sFtpPort + '\'' +
                ", sFtpHost='" + sFtpHost + '\'' +
                ", sFtpUser='" + sFtpUser + '\'' +
                ", sFtpPassword='" + sFtpPassword + '\'' +
                ", sFtpSubdirectory='" + sFtpSubdirectory + '\'' +
                '}';
    }
}
