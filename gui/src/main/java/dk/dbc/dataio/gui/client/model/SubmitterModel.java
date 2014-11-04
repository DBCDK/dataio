package dk.dbc.dataio.gui.client.model;

public class SubmitterModel extends GenericBackendModel {
    private String number;
    private String name;
    private String description;

    /**
     * Constructor with all parameters
     * @param id Id for the Submitter Model
     * @param version Version number for the Submitter Model
     * @param number Submitter number
     * @param name Submitter name
     * @param description Submitter description
     */
    public SubmitterModel(long id, long version, String number, String name, String description) {
        super(id, version);
        this.version = version;
        this.number = number;
        this.name = name;
        this.description = description;
    }

    public SubmitterModel() {
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
     * @param number Submitter number
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
     * @param name Submitter name
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
     * @param description Submitter description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /*
     * Validates if the String, set as number, can be cast to number format
     */
    public boolean isNumberValid() {
        try {
            Long.valueOf(number);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Checks for empty String values
     */
    public boolean isInputFieldsEmpty() {
        return number.isEmpty() || name.isEmpty() || description.isEmpty() ;
    }
}
