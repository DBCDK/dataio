package dk.dbc.dataio.sink.rawrepo.update.v3.connector;

import java.util.List;
import java.util.Objects;

public class UpdateResponse {
    private UpdateResponseStatus status;
    private List<ValidationMessage> errors;

    public UpdateResponseStatus getStatus() {
        return status;
    }

    public void setStatus(UpdateResponseStatus status) {
        this.status = status;
    }

    public List<ValidationMessage> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationMessage> errors) {
        this.errors = errors;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdateResponse that = (UpdateResponse) o;
        return status == that.status && Objects.equals(errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, errors);
    }

    @Override
    public String toString() {
        return "UpdateResponse{" +
                "status=" + status +
                ", errors=" + errors +
                '}';
    }
}
