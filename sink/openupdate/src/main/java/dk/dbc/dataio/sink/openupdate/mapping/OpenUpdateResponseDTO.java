package dk.dbc.dataio.sink.openupdate.mapping;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

import java.util.List;

public class OpenUpdateResponseDTO {

    public enum Status {OK, VALIDATION_ERROR, FAILED_INVALID_AGENCY, FAILED_INVALID_SCHEMA, FAILED_INVALID_OPTION, FAILED_VALIDATION_INTERNAL_ERROR, FAILED_UPDATE_INTERNAL_ERROR, }
    private JSONBContext jsonbContext = new JSONBContext();
    private Status status;

    List<OpenUpdateErrorMessageDTO> errorMessages = null;

    public String asJson() throws JSONBException {
        return jsonbContext.marshall(this);
    }

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
