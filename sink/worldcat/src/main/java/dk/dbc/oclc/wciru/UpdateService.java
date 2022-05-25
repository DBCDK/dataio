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
