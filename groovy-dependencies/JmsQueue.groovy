
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

ENCODING = StandardCharsets.UTF_8.name()
client = HttpClient.newClient(new ClientConfig().register(new Jackson2xFeature()))

item = new ChunkItem(0, "", ChunkItem.Status.IGNORE)
items = new ArrayList<ChunkItem>()
items.add(item)
chunkResult = new ChunkResult(1, 1, StandardCharsets.UTF_8, items)

message = new MockedJmsTextMessage()
message.setText(JsonUtil.toJson(chunkResult))
message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.SINK_RESULT_PAYLOAD_TYPE)
message.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.PROCESSOR_RESULT_PAYLOAD_TYPE)
message.setStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME, "url/dataio/fbs/ws")

