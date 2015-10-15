package dk.dbc.dataio.sink.openupdate.mapping;

import java.util.List;

/**
 * Created by ThomasBerg on 14/10/15.
 */
public class OpenUpdateResponseDTO {

    public enum Status {OK, VALIDATION_ERROR, FAILED_INVALID_AGENCY, FAILED_INVALID_SCHEMA, FAILED_INVALID_OPTION, FAILED_VALIDATION_INTERNAL_ERROR, FAILED_UPDATE_INTERNAL_ERROR, }

    private Status status;

    List<OpenUpdateErrorMessageDTO> errorMessages = null;

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }

    public List<OpenUpdateErrorMessageDTO> getErrorMessages() {
        return errorMessages;
    }
    public void setErrorMessages(List<OpenUpdateErrorMessageDTO> errorMessages) {
        this.errorMessages = errorMessages;
    }
}
