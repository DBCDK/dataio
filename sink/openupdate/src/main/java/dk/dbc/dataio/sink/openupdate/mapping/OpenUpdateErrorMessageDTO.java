package dk.dbc.dataio.sink.openupdate.mapping;

import dk.dbc.dataio.jsonb.JSONBContext;

public class OpenUpdateErrorMessageDTO {

    public enum ErrorType {ERROR, WARNING}
    private JSONBContext jsonbContext = new JSONBContext();

    // Optional values
    private ErrorType type;
    private Long ordinalPositionOfField;
    private Long ordinalPositionOfSubField;
    private String errorMessage;

    public ErrorType getType() {
        return type;
    }
    public void setType(ErrorType type) {
        this.type = type;
    }

    public Long getOrdinalPositionOfField() {
        return ordinalPositionOfField;
    }
    public void setOrdinalPositionOfField(Long ordinalPositionOfField) {
        this.ordinalPositionOfField = ordinalPositionOfField;
    }

    public Long getOrdinalPositionOfSubField() {
        return ordinalPositionOfSubField;
    }
    public void setOrdinalPositionOfSubField(Long ordinalPositionOfSubField) {
        this.ordinalPositionOfSubField = ordinalPositionOfSubField;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
