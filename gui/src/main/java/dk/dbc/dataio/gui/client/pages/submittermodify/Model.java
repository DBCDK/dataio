package dk.dbc.dataio.gui.client.pages.submittermodify;

public class Model {
    private Long id;
    private Long version;
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
        this.id = id;
        this.version = version;
        this.number = number;
        this.name = name;
        this.description = description;
    }

    /**
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Set id
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Set version
     * @param version
     */
    public void setVersion(Long version) {
        this.version = version;
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
    public Boolean validate() {
        Boolean isValid = true;
        if (number.isEmpty() || name.isEmpty() || description.isEmpty()) {
            isValid = false;
        }
        try {
            Long.valueOf(number);
        } catch (NumberFormatException e) {
            isValid = false;
        }
        return isValid;
    }


}
