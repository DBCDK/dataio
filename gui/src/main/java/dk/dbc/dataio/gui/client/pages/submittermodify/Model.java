package dk.dbc.dataio.gui.client.pages.submittermodify;

import com.google.gwt.user.client.rpc.IsSerializable;
import dk.dbc.dataio.gui.client.model.GenericBackendModel;

public class Model extends GenericBackendModel implements IsSerializable {
    private String number;
    private String name;
    private String description;

    /**
     * Constructor with all parameters
     * @param id
     * @param version
     * @param number
     * @param name
     * @param description
     */
    public Model(long id, long version, String number, String name, String description) {
        super(id, version);
        this.number = number;
        this.name = name;
        this.description = description;
    }

    public Model() {
        super(0L, 0L);
        this.number = "";
        this.name = "";
        this.description = "";
    }


    /**
     * @return number
     */
    public String getNumber() {
        return number;
    }

    /**
     * Set number
     * @param number
     */
    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set description
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /*
     * Validates if the String, set as number, can be cast to number format
     */
    public void validateNumber(SubmitterModifyConstants constants) {
        try {
            Long.valueOf(number);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(constants.error_NumberInputFieldValidationError());
        }
    }

    /**
     * Checks for empty String values
     */
    public boolean isInputFieldsEmpty() throws IllegalArgumentException {
        return number.isEmpty() || name.isEmpty() || description.isEmpty() ;
    }
}
