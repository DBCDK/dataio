package dk.dbc.dataio.sink.openupdate;

import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResponse;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

/**
 * Created by ThomasBerg on 21/10/15.
 */
public class TestData {

    public static UpdateRecordResult getWebserviceResultOK() throws JAXBException {
        return unmarchelUpdateRecordResponse(WEBSERVICE_RESULT_OK).getUpdateRecordResult();
    }

    public static UpdateRecordResult webserviceResultWithValidationErrors() throws JAXBException {

        return unmarchelUpdateRecordResponse(WEBSERVICE_RESULT_WITH_VALIDATION_ERROR).getUpdateRecordResult();
    }

    private static UpdateRecordResponse unmarchelUpdateRecordResponse(String xmlResponseToUnmarhal) throws JAXBException {
            final Unmarshaller unmarshaller = JAXBContext.newInstance(UpdateRecordResponse.class).createUnmarshaller();
            StringReader reader = new StringReader(xmlResponseToUnmarhal);
            final UpdateRecordResponse unmarshalledUpdateRecordResult = (UpdateRecordResponse) unmarshaller.unmarshal(reader);
            return unmarshalledUpdateRecordResult;
    }

    private final static String WEBSERVICE_RESULT_OK = "<updateRecordResponse xmlns=\"http://oss.dbc.dk/ns/catalogingUpdate\">\n" +
            "         <updateRecordResult>\n" +
            "            <updateStatus>ok</updateStatus>\n" +
            "         </updateRecordResult>\n" +
            "      </updateRecordResponse>";

    private final static String WEBSERVICE_RESULT_WITH_VALIDATION_ERROR = "<updateRecordResponse xmlns=\"http://oss.dbc.dk/ns/catalogingUpdate\">\n" +
            "         <updateRecordResult>\n" +
            "            <updateStatus>validation_error</updateStatus>\n" +
            "            <validateInstance>\n" +
            "               <validateEntry>\n" +
            "                  <warningOrError>error</warningOrError>\n" +
            "                  <urlForDocumentation>http://www.kat-format.dk/danMARC2/Danmarc2.5.htm</urlForDocumentation>\n" +
            "                  <ordinalPositionOfField>1</ordinalPositionOfField>\n" +
            "                  <ordinalPositionOfSubField>1</ordinalPositionOfSubField>\n" +
            "                  <message>Værdien '191919' er ikke end del af de valide værdier: '870970'</message>\n" +
            "               </validateEntry>\n" +
            "               <validateEntry>\n" +
            "                  <warningOrError>error</warningOrError>\n" +
            "                  <urlForDocumentation>http://www.kat-format.dk/danMARC2/Danmarc2.5.htm</urlForDocumentation>\n" +
            "                  <ordinalPositionOfField>1</ordinalPositionOfField>\n" +
            "                  <message>Delfelt 'a' mangler i felt '001'</message>\n" +
            "               </validateEntry>\n" +
            "               <validateEntry>\n" +
            "                  <warningOrError>error</warningOrError>\n" +
            "                  <urlForDocumentation>http://www.kat-format.dk/danMARC2/Danmarc2.5.htm</urlForDocumentation>\n" +
            "                  <ordinalPositionOfField>1</ordinalPositionOfField>\n" +
            "                  <message>Delfelt 'b' er gentaget '2' gange i feltet</message>\n" +
            "               </validateEntry>\n" +
            "            </validateInstance>\n" +
            "         </updateRecordResult>\n" +
            "      </updateRecordResponse>";


    public final static String MARCX_VALID_FROM_PROCESSING = "<?xml version='1.0'?>\n" +
            "    <dataio-harvester-datafile>\n" +
            "        <data-container>\n" +
            "            <data-supplementary>\n" +
            "                <creationDate>20140717</creationDate>\n" +
            "                <enrichmentTrail>191919,870970</enrichmentTrail>\n" +
            "            </data-supplementary>\n" +
            "            <data>\n" +
            "                <collection xmlns=\"info:lc/xmlns/marcxchange-v1\">\n" +
            "                    <marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" format=\"danMARC2\" type=\"Bibliographic\">\n" +
            "                        <marcx:leader>00000n 2200000 4500</marcx:leader>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">\n" +
            "                            <marcx:subfield code=\"a\">51203399</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"b\">870970</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"c\">20150411130000</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"d\">20140717</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"f\">a</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"004\">\n" +
            "                            <marcx:subfield code=\"r\">n</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"a\">e</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"006\">\n" +
            "                            <marcx:subfield code=\"d\">15</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"2\">b</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"008\">\n" +
            "                            <marcx:subfield code=\"t\">m</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"u\">f</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"a\">2014</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"b\">dk</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"l\">eng</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"v\">0</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"009\">\n" +
            "                            <marcx:subfield code=\"a\">m</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"g\">th</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"023\">\n" +
            "                            <marcx:subfield code=\"b\">7350062383188</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"032\">\n" +
            "                            <marcx:subfield code=\"x\">ACC201429</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"a\">DBI201431</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"041\">\n" +
            "                            <marcx:subfield code=\"a\">eng</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"u\">swe</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"u\">nor</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"u\">fin</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"u\">dan</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">\n" +
            "                            <marcx:subfield code=\"a\">Invasion day</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"260\">\n" +
            "                            <marcx:subfield code=\"a\">[S.l.]</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"b\">TakeOne</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"c\">2014</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"300\">\n" +
            "                            <marcx:subfield code=\"n\">1 dvd-video</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"l\">91 min.</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"508\">\n" +
            "                            <marcx:subfield code=\"a\">Engelsk tale</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"508\">\n" +
            "                            <marcx:subfield code=\"a\">Undertekster på svensk, norsk, finsk og dansk</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"512\">\n" +
            "                            <marcx:subfield code=\"i\">I undertekstning med titel</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"t\">Dragon day</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"512\">\n" +
            "                            <marcx:subfield code=\"a\">Produktion: Matter Media Studios (USA), Burning Myth Film (USA), 2013</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"517\">\n" +
            "                            <marcx:subfield code=\"&amp;\">1</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"a\">Mærkning: Tilladt for børn over 15 år</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"538\">\n" +
            "                            <marcx:subfield code=\"a\">12166</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"652\">\n" +
            "                            <marcx:subfield code=\"m\">77.7</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"700\">\n" +
            "                            <marcx:subfield code=\"0\"/>\n" +
            "                            <marcx:subfield code=\"a\">Jeffrey</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"h\">Travis</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"4\">aus</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"4\">drt</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"700\">\n" +
            "                            <marcx:subfield code=\"0\"/>\n" +
            "                            <marcx:subfield code=\"a\">Patterson</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"h\">Matt</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"4\">cng</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"4\">aus</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"720\">\n" +
            "                            <marcx:subfield code=\"o\">Ethan Flower</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"4\">act</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"720\">\n" +
            "                            <marcx:subfield code=\"o\">Åsa Wallander</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"4\">act</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"720\">\n" +
            "                            <marcx:subfield code=\"o\">Jenn Gotzon</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"4\">act</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"720\">\n" +
            "                            <marcx:subfield code=\"o\">Eloy Mendez</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"4\">act</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"720\">\n" +
            "                            <marcx:subfield code=\"o\">Scoot McNairy</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"4\">act</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"996\">\n" +
            "                            <marcx:subfield code=\"a\">DBC</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"d08\">\n" +
            "                            <marcx:subfield code=\"o\">wnn</marcx:subfield>\n" +
            "                            <marcx:subfield code=\"o\">osh</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"d08\">\n" +
            "                            <marcx:subfield code=\"o\">aut</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"s10\">\n" +
            "                            <marcx:subfield code=\"a\">DBC</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"s12\">\n" +
            "                            <marcx:subfield code=\"t\">TeamBMV201429</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"z98\">\n" +
            "                            <marcx:subfield code=\"a\">Minus korrekturprint</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"z99\">\n" +
            "                            <marcx:subfield code=\"a\">osh</marcx:subfield>\n" +
            "                        </marcx:datafield>\n" +
            "                    </marcx:record>\n" +
            "                </collection>\n" +
            "            </data>\n" +
            "        </data-container>\n" +
            "    </dataio-harvester-datafile>";
}
