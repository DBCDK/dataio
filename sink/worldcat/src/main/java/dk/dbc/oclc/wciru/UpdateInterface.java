package dk.dbc.oclc.wciru;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.bind.annotation.XmlSeeAlso;


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
