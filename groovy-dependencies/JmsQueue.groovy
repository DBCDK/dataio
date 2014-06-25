
// Start with:
// groovysh -cp target/groovy-dependencies-1.0-SNAPSHOT.jar -Dcontainer.hostname=localhost -Dcontainer.http.port=8080

import dk.dbc.dataio.integrationtest.JmsQueueConnector
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage

import org.glassfish.jersey.client.ClientConfig
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature
import dk.dbc.dataio.commons.utils.httpclient.HttpClient

import dk.dbc.dataio.commons.types.jms.JmsConstants

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import dk.dbc.dataio.commons.types.ChunkItem
import dk.dbc.dataio.commons.types.ChunkResult
import dk.dbc.dataio.commons.utils.json.JsonUtil

import dk.dbc.dataio.commons.utils.service.Base64Util

ENCODING = StandardCharsets.UTF_8.name()
client = HttpClient.newClient(new ClientConfig().register(new Jackson2xFeature()))

marcx = "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\"><marcx:record format=\"danMARC2\" type=\"Bibliographic\" xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\"><marcx:leader>00000n    2200000   4500</marcx:leader><marcx:datafield tag=\"001\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">4 539 593 6</marcx:subfield><marcx:subfield code=\"b\">870970</marcx:subfield><marcx:subfield code=\"c\">20131114205943</marcx:subfield><marcx:subfield code=\"d\">20131114</marcx:subfield><marcx:subfield code=\"f\">a</marcx:subfield></marcx:datafield><marcx:datafield tag=\"002\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"b\">710100</marcx:subfield><marcx:subfield code=\"c\">9 328 594 8</marcx:subfield><marcx:subfield code=\"x\">71010093285948</marcx:subfield></marcx:datafield><marcx:datafield tag=\"004\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"r\">c</marcx:subfield><marcx:subfield code=\"a\">h</marcx:subfield></marcx:datafield><marcx:datafield tag=\"008\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"b\">fr</marcx:subfield><marcx:subfield code=\"u\">u</marcx:subfield><marcx:subfield code=\"j\">f</marcx:subfield><marcx:subfield code=\"l\">fre</marcx:subfield><marcx:subfield code=\"v\">0</marcx:subfield></marcx:datafield><marcx:datafield tag=\"009\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">a</marcx:subfield><marcx:subfield code=\"g\">xx</marcx:subfield></marcx:datafield><marcx:datafield tag=\"100\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">Proust</marcx:subfield><marcx:subfield code=\"h\">Marcel</marcx:subfield><marcx:subfield code=\"4\">aut</marcx:subfield></marcx:datafield><marcx:datafield tag=\"245\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">Ã€ la recherche du temps perdu</marcx:subfield></marcx:datafield><marcx:datafield tag=\"260\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">Paris</marcx:subfield><marcx:subfield code=\"b\">Gallimard</marcx:subfield><marcx:subfield code=\"c\">1988-2012</marcx:subfield></marcx:datafield><marcx:datafield tag=\"300\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">7 bind</marcx:subfield></marcx:datafield><marcx:datafield tag=\"504\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">SÃ¦deskildring og psykologisk analyse af adelens og storborgerskabets parisiske verden i perioden 1880-1920. FortÃ¦lleren Marcel er en fremmed i den ydre verden, og forsÃ¸ger gennem sit indre liv at genfinde og fastholde den tabte fortid</marcx:subfield></marcx:datafield><marcx:datafield tag=\"520\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"a\">Originaludgave: 1913-1927</marcx:subfield></marcx:datafield><marcx:datafield tag=\"652\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"m\">82</marcx:subfield></marcx:datafield><marcx:datafield tag=\"666\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"s\">samfundsforhold</marcx:subfield></marcx:datafield><marcx:datafield tag=\"666\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"q\">Frankrig</marcx:subfield></marcx:datafield><marcx:datafield tag=\"666\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"q\">Paris</marcx:subfield></marcx:datafield><marcx:datafield tag=\"666\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"i\">1880-1889</marcx:subfield></marcx:datafield><marcx:datafield tag=\"666\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"s\">1890-1899</marcx:subfield></marcx:datafield><marcx:datafield tag=\"666\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"s\">1900-1909</marcx:subfield></marcx:datafield><marcx:datafield tag=\"666\" ind1=\"0\" ind2=\"0\"><marcx:subfield code=\"s\">1910-1919</marcx:subfield></marcx:datafield></marcx:record></marcx:collection>"

item = new ChunkItem(0, Base64Util.base64encode(marcx), ChunkItem.Status.SUCCESS)
items = new ArrayList<ChunkItem>()
items.add(item)
chunkResult = new ChunkResult(1, 1, StandardCharsets.UTF_8, items)

message = new MockedJmsTextMessage()
message.setText(JsonUtil.toJson(chunkResult))
message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE)
message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE)
message.setStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME, "url/dataio/fbs/ws")

// reponse = HttpClient.doPostWithJson(client, message, JmsQueueConnector.JMS_QUEUE_SERVICE_BASEURL, JmsQueueConnector.QUEUE_RESOURCE_ENDPOINT, URLEncoder.encode(JmsQueueConnector.SINKS_QUEUE_NAME, ENCODING))

