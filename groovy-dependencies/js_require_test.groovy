

import dk.dbc.dataio.commons.types.Flow
import dk.dbc.dataio.commons.utils.service.Base64Util
import dk.dbc.dataio.gui.server.JavaScriptProjectFetcherImpl
import dk.dbc.dataio.jobprocessor.ejb.ChunkProcessorBean
import dk.dbc.dataio.jobstore.fsjobstore.JsonFileUtil
import dk.dbc.dataio.jobstore.types.Chunk

import java.nio.charset.Charset
import java.nio.file.Paths
// To run this file:
//    groovysh -cp target/groovy-dependencies-1.0-SNAPSHOT.jar js_require_test.groovy
// I'm (jda) using groovy 2.4.0 and java 1.8.0_25


// This file is created in order to play around with the Require-module in the Javascripts inside the JobProcessor
// We will load a Chunk and a Flow and give it to the job-processor, after modifing the flow with new javascripts.
// 
// The directory 265 contains a job with a single item which originally uses the DataIOBackendConverterE4X.
// Later in the code the flow wil be modified to use DataIOBackendConverter which uses the Require-module.

charset = Charset.forName("UTF-8")

// Read the chunk from file:
chunkPath = Paths.get("265/1.json")
rawChunk = JsonFileUtil.getJsonFileUtil(charset).readObjectFromFile(chunkPath, Chunk.class)
partitionedChunk = Chunk.convertToExternalChunk(rawChunk)

supplementaryProcessData = rawChunk.supplementaryProcessData

// Read the flow from file
flowPath = Paths.get("265/flow.json")
flow = JsonFileUtil.getJsonFileUtil(charset).readObjectFromFile(flowPath, Flow.class)

fetcher = new JavaScriptProjectFetcherImpl("https://svn.dbc.dk/repos")

// This revision has been modified to use Require over E4X - use this for require-testing purposes.
svnRevisionRequire = 81833 // If you need to use a version which uses E4X, use rev: 81834

// In order to bring fetchRequiredJavaScript to work with Require, you need to modify the job-processor.
// In JavascriptUtil.getAllDependentJavascripts, you need to add two lines:
// Just inside the for-loop:
//    for(Path path : javascriptDirs) {
// you must add these two lines:
//    Path currentPath = Paths.get(System.getProperty("user.dir")); 
//    path = currentPath.relativize(path);
//
// The purpose of the above is to let the files given to the module-handler have relative path.
// This is necessary as described in the code for Require.use.js
// Beware - this is not tested in a glassfish!
// Note: Depending on your connection this may take up to 30 seconds:
javascripts = fetcher.fetchRequiredJavaScript("datawell-convert", svnRevisionRequire, "js/marc_fbs.js", "convertRawRecord")

// Just some debug info:
// javascripts.each() { println "${it.getModuleName()}" };

// Modifing the loaded flow with our new javascripts:
flow.content.components[0].content.javascripts = javascripts

// Creating and initializing the chunk-processor:
chunkProcessor = new ChunkProcessorBean()

// Running the processor on our item:
processedChunk = chunkProcessor.process(partitionedChunk, flow, supplementaryProcessData)

// Just some debug to ensure that we get a sensibel result (or an exception):
processedChunk.iterator().hasNext()
item = processedChunk.iterator().next()
Base64Util.base64decode(item.data)