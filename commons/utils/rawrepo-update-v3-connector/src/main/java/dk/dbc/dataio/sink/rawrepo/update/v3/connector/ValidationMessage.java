package dk.dbc.dataio.sink.rawrepo.update.v3.connector;

import java.util.Objects;

public class ValidationMessage {
    private ValidationStatus type;
    private String message;
    private Integer ordinalPositionOfField;
    private Integer ordinalPositionOfSubfield;
    private String urlForDocumentation;

    public ValidationStatus getType() {
        return type;
    }

    public void setType(ValidationStatus type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getOrdinalPositionOfField() {
        return ordinalPositionOfField;
    }

    public void setOrdinalPositionOfField(Integer ordinalPositionOfField) {
        this.ordinalPositionOfField = ordinalPositionOfField;
    }

    public Integer getOrdinalPositionOfSubfield() {
        return ordinalPositionOfSubfield;
    }

    public void setOrdinalPositionOfSubfield(Integer ordinalPositionOfSubfield) {
        this.ordinalPositionOfSubfield = ordinalPositionOfSubfield;
    }

    public String getUrlForDocumentation() {
        return urlForDocumentation;
    }

    public void setUrlForDocumentation(String urlForDocumentation) {
        this.urlForDocumentation = urlForDocumentation;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValidationMessage that = (ValidationMessage) o;
        return type == that.type && Objects.equals(message, that.message) && Objects.equals(ordinalPositionOfField, that.ordinalPositionOfField) && Objects.equals(ordinalPositionOfSubfield, that.ordinalPositionOfSubfield) && Objects.equals(urlForDocumentation, that.urlForDocumentation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, message, ordinalPositionOfField, ordinalPositionOfSubfield, urlForDocumentation);
    }

    @Override
    public String
    toString() {
        return "ValidationMessage{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", ordinalPositionOfField=" + ordinalPositionOfField +
                ", ordinalPositionOfSubfield=" + ordinalPositionOfSubfield +
                ", urlForDocumentation='" + urlForDocumentation + '\'' +
                '}';
    }
}
