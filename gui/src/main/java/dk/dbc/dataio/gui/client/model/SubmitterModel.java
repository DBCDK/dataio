/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.model;

import dk.dbc.dataio.gui.client.util.Format;

import java.util.List;

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

    /**
     * Default constructor.
     * Note that: Other classes depend upon the default value for id being exactly 0
     */
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
     * @return true if no empty String values were found, otherwise false
     */
    public boolean isInputFieldsEmpty() {
        return number.isEmpty() || name.isEmpty() || description.isEmpty() ;
    }

    /**
     * Checks if the submitter name contains illegal characters.
     * A-Å, 0-9, - (minus), + (plus), _ (underscore) and space is valid
     * @return a list containing illegal characters found. Empty list if none found.
     */
    public List<String> getDataioPatternMatches() {
        return Format.getDataioPatternMatches(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmitterModel)) return false;

        SubmitterModel model = (SubmitterModel) o;

        if (number != null ? !number.equals(model.number) : model.number != null) return false;
        if (name != null ? !name.equals(model.name) : model.name != null) return false;
        return !(description != null ? !description.equals(model.description) : model.description != null);

    }

    @Override
    public int hashCode() {
        int result = number != null ? number.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
