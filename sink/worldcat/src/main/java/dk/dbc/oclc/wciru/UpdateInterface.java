/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.oclc.wciru;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;


@WebService(name = "updateInterface", targetNamespace = "http://Update.os.oclc.ORG")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({
    ObjectFactory.class
})
public interface UpdateInterface {
    @WebMethod
    @WebResult(name = "updateResponse", targetNamespace = "http://www.loc.gov/zing/srw/update/", partName = "updateResponse")
    UpdateResponseType update(
        @WebParam(name = "updateRequest", targetNamespace = "http://www.loc.gov/zing/srw/update/", partName = "updateRequest")
        UpdateRequestType updateRequest);

    @WebMethod
    @WebResult(name = "explainResponse", targetNamespace = "http://www.loc.gov/zing/srw/update/", partName = "explainResponse")
    ExplainResponseType explain(
        @WebParam(name = "explainRequest", targetNamespace = "http://www.loc.gov/zing/srw/update/", partName = "explainRequest")
        ExplainRequestType explainRequest);
}
