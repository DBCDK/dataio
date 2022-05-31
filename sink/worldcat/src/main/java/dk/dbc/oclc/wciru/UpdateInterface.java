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
