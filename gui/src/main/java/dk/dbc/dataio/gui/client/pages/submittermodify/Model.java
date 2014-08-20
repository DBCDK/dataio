package dk.dbc.dataio.gui.client.pages.submittermodify;

import dk.dbc.dataio.gui.client.model.GenericModel;

public class Model extends GenericModel {
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
    public Model(Long id, Long version, String number, String name, String description) {
        super(id, version);
        this.number = number;
        this.name = name;
        this.description = description;
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

    /**
     * Validates the class attributes in this model
     *
     * @return true if valid, false if not valid
     */
    public void validate(SubmitterModifyConstants constants) {
        if (number.isEmpty() || name.isEmpty() || description.isEmpty()) {
            throw new IllegalArgumentException(constants.error_InputFieldValidationError());
        }
        try {
            Long.valueOf(number);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(constants.error_NumberInputFieldValidationError());
        }
    }


}
