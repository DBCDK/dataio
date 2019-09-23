/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.sink.marcconv;

import dk.dbc.dataio.commons.conversion.ConversionParam;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "param")
@NamedQueries({
    @NamedQuery(
            name = StoredConversionParam.DELETE_CONVERSION_PARAM_QUERY_NAME,
            query = StoredConversionParam.DELETE_CONVERSION_PARAM_QUERY)
})
public class StoredConversionParam {
    public static final String DELETE_CONVERSION_PARAM_QUERY =
            "DELETE FROM StoredConversionParam param" +
            " WHERE param.jobId = :jobId";
    public static final String DELETE_CONVERSION_PARAM_QUERY_NAME =
            "StoredConversionParam.delete";

    @Id
    private Integer jobId;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = ConversionParamConverter.class)
	private ConversionParam param;

    public StoredConversionParam() {}

    StoredConversionParam(Integer jobId) {
        this.jobId = jobId;
    }

    public Integer getJobId() {
        return jobId;
    }

    public ConversionParam getParam() {
        return param;
    }

    public void setParam(ConversionParam param) {
        this.param = param;
    }

    @Override
    public String toString() {
        return "StoredConversionParam{" +
                "jobId=" + jobId +
                ", param=" + param +
                '}';
    }
}
