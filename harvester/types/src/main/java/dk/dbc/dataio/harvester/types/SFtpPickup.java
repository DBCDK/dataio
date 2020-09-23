package dk.dbc.dataio.harvester.types;

public class SFtpPickup extends Pickup {
    private String sFtpPort = "22";
    private String sFtpHost;
    private String SFtpUser;
    private String sFtpPassword;
    private String sFtpSubdirectory;

    public SFtpPickup withSFtpPort(String sFtpPort) {
        this.sFtpPort = sFtpPort;
        return this;
    }

    public SFtpPickup withSFtpHost(String sFtpHost) {
        this.sFtpHost = sFtpHost;
        return this;
    }

    public SFtpPickup withSFtpuser(String sFtpuser) {
        this.SFtpUser = sFtpuser;
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

    public String getSFtpUser() {
        return SFtpUser;
    }

    public String getsFtpPassword() {
        return sFtpPassword;
    }

    public String getsFtpSubdirectory() {
        return sFtpSubdirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SFtpPickup that = (SFtpPickup) o;

        if (sFtpPort != null ? !sFtpPort.equals(that.sFtpPort) : that.sFtpPort != null) return false;
        if (!sFtpHost.equals(that.sFtpHost)) return false;
        if (!SFtpUser.equals(that.SFtpUser)) return false;
        if (!sFtpPassword.equals(that.sFtpPassword)) return false;
        return sFtpSubdirectory != null ? sFtpSubdirectory.equals(that.sFtpSubdirectory) : that.sFtpSubdirectory == null;
    }

    @Override
    public int hashCode() {
        int result = sFtpPort != null ? sFtpPort.hashCode() : 0;
        result = 31 * result + sFtpHost.hashCode();
        result = 31 * result + SFtpUser.hashCode();
        result = 31 * result + sFtpPassword.hashCode();
        result = 31 * result + (sFtpSubdirectory != null ? sFtpSubdirectory.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SFtpPickup{" +
                "sFtpPort='" + sFtpPort + '\'' +
                ", sFtpHost='" + sFtpHost + '\'' +
                ", SFtpUser='" + SFtpUser + '\'' +
                ", sFtpPassword='" + sFtpPassword + '\'' +
                ", sFtpSubdirectory='" + sFtpSubdirectory + '\'' +
                '}';
    }
}
