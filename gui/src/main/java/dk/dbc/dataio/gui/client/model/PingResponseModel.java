package dk.dbc.dataio.gui.client.model;

public class PingResponseModel extends GenericBackendModel {

    public enum Status { OK, FAILED }

    private Status status;

    public PingResponseModel(Status status){
        this.status = status;
    }

    public PingResponseModel() {
        this(Status.OK);
    }

    public Status getStatus() {
        return status;
    }
}
