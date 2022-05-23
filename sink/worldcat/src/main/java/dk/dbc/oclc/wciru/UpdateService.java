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

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import java.net.URL;

@WebServiceClient(name = "UpdateService", targetNamespace = "http://Update.os.oclc.ORG", wsdlLocation = "/wsdl/oclc-wciru.wsdl")
public class UpdateService
    extends Service
{

    private final static URL UPDATESERVICE_WSDL_LOCATION;
    private final static WebServiceException UPDATESERVICE_EXCEPTION;
    private final static QName UPDATESERVICE_QNAME = new QName("http://Update.os.oclc.ORG", "UpdateService");

    static {
        UPDATESERVICE_WSDL_LOCATION = UpdateService.class.getResource("/wsdl/oclc-wciru.wsdl");
        WebServiceException e = null;
        if (UPDATESERVICE_WSDL_LOCATION == null) {
            e = new WebServiceException("Cannot find '/wsdl/oclc-wciru.wsdl' wsdl. Place the resource correctly in the classpath.");
        }
        UPDATESERVICE_EXCEPTION = e;
    }

    public UpdateService() {
        super(__getWsdlLocation(), UPDATESERVICE_QNAME);
    }

    public UpdateService(WebServiceFeature... features) {
        super(__getWsdlLocation(), UPDATESERVICE_QNAME, features);
    }

    public UpdateService(URL wsdlLocation) {
        super(wsdlLocation, UPDATESERVICE_QNAME);
    }

    public UpdateService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, UPDATESERVICE_QNAME, features);
    }

    public UpdateService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public UpdateService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    @WebEndpoint(name = "Update")
    public UpdateInterface getUpdate() {
        return super.getPort(new QName("http://Update.os.oclc.ORG", "Update"), UpdateInterface.class);
    }

    @WebEndpoint(name = "Update")
    public UpdateInterface getUpdate(WebServiceFeature... features) {
        return super.getPort(new QName("http://Update.os.oclc.ORG", "Update"), UpdateInterface.class, features);
    }

    private static URL __getWsdlLocation() {
        if (UPDATESERVICE_EXCEPTION!= null) {
            throw UPDATESERVICE_EXCEPTION;
        }
        return UPDATESERVICE_WSDL_LOCATION;
    }
}
