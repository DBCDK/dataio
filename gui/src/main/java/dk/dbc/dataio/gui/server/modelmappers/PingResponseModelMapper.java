package dk.dbc.dataio.gui.server.modelmappers;

import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.gui.client.model.PingResponseModel;

public final class PingResponseModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private PingResponseModelMapper(){}

    /**
     * Maps a PingResponse to a Model
     * @param pingResponse, the ping response
     * @return model
     */
    public static PingResponseModel toModel(PingResponse pingResponse) {
        return new PingResponseModel(
                mapPingResponseStatus(pingResponse.getStatus())
        );
    }

    public static PingResponseModel.Status mapPingResponseStatus(PingResponse.Status status) {
        switch (status) {
            case FAILED:
                return PingResponseModel.Status.FAILED;
            default:
                return PingResponseModel.Status.OK;
        }
    }

}
