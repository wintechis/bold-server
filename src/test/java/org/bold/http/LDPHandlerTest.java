package org.bold.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bold.io.FileUtils;
import org.bold.sim.SimulationEngine;
import org.bold.sim.SimulationHandler;
import org.bold.sim.Vocabulary;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.fau.rw.ti.LDPInferencerConnection;

public class LDPHandlerTest {

    private static final Integer TEST_PORT = 8080;

    private static final String EXPECTED_JSON = "{\"val\":10}";

    private SimulationHandler handler = null;
    private SimulationEngine ngin = null;

    @Before
    public void createEngine() throws Exception {
        handler = new SimulationHandler(TEST_PORT, "ldp");
        ngin = handler.getSimulationEngine();
    }

    @After
    public void closeEngine() {
        try {
            handler.terminate();
            clearResults();
        } catch (Exception e) {
            e.printStackTrace();
            // and ignore...
        }
    }

    @Test
    public void testAccept() throws Exception {
        ngin.registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "video/mp4");

        startSimulation("sim.ttl");
        int status = sendDummyRequest(h);

        assertEquals(406, status);
    }

    @Test
    public void testAcceptWildcard() throws Exception {
        ngin.registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "*/*");

        startSimulation("sim.ttl");
        int status = sendDummyRequest(h);

        assertEquals(404, status);
    }

    @Test
    public void testContentType() throws Exception {
        ngin.registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Content-Type", "video/mp4");

        startSimulation("sim.ttl");
        int status = sendDummyRequest(h);

        assertEquals(415, status);
    }

    @Test
    public void testNotFound() throws Exception {
        ngin.registrationDone();

        startSimulation("sim.ttl");
        int status = sendDummyRequest();

        assertEquals(404, status);
    }

    @Test
    public void testBasicContainer() throws Exception {
        ValueFactory factory = SimpleValueFactory.getInstance();
        ngin.registerDataset("ldp.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/turtle");

        startSimulation("sim.ttl");
        Model model = getModel("rootContainer", h);
        assertTrue(model.contains(factory.createIRI("http://127.0.1.1:" + TEST_PORT + "/", "rootContainer"), RDF.TYPE, LDP.BASIC_CONTAINER));
    }

    @Test
    public void testBasicContainerGetLinkRel() throws Exception {
        ngin.registerDataset("ldp.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/turtle");

        startSimulation("sim.ttl");
        HttpResponse resp = getResponse("rootContainer", h);
        assertTrue("GET on ldp:BasicContainer does not return the right Link header (5.2.1.4)", Stream.of(resp.getHeaders("Link")).map(
            header -> header.getValue()
        ).filter(
            hv -> hv.equals(LDP.BASIC_CONTAINER + "; rel=\"type\"")
        ).count() > 0);
    }

    @Test
    public void testResourceGetLinkRel() throws Exception {
        ngin.registerDataset("ldp.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/turtle");

        startSimulation("sim.ttl");
        HttpResponse resp = getResponse("rootContainer/resourceA", h);
        assertFalse("GET on non container returns a Link header as if it was a container (5.2.1.4)", Stream.of(resp.getHeaders("Link")).map(
            header -> header.getValue()
        ).filter(
            hv -> hv.equals(LDP.BASIC_CONTAINER + "; rel=\"type\"")
        ).count() > 0);
    }

    @Test
    public void testCreateResourcePostStatusCode() throws Exception {
        ngin.registerDataset("ldp.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/turtle");

        startSimulation("sim.ttl");

        CloseableHttpClient client = HttpClients.createMinimal();
        HttpPost req = new HttpPost("http://localhost:" + TEST_PORT + "/rootContainer");
        HttpResponse resp = client.execute(req);

        assertEquals("Creating a new resource in a container did not result in a 201 status code (5.2.3.1)", 201, resp.getStatusLine().getStatusCode());
    }

    @Test
    public void testCreateResourcePostLinkHeader() throws Exception {
        ngin.registerDataset("ldp.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/turtle");

        startSimulation("sim.ttl");

        CloseableHttpClient client = HttpClients.createMinimal();
        HttpPost req = new HttpPost("http://localhost:" + TEST_PORT + "/rootContainer");
        HttpResponse resp = client.execute(req);

        assertTrue("Creating a new resource in a container did not result in the correct Location header (5.2.3.1)", Stream.of(resp.getHeaders("Location")).map(
            header -> header.getValue()
        ).filter(
            hv -> hv.startsWith("http://127.0.1.1:" + TEST_PORT + "/rootContainer/", 0)
        ).count() > 0);
    }

    @Test
    public void testPutChangeContainmentTriples() throws Exception {
        ngin.registerDataset("ldp.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/turtle");

        startSimulation("sim.ttl");

        CloseableHttpClient client = HttpClients.createMinimal();
        HttpPut req = new HttpPut("http://localhost:" + TEST_PORT + "/rootContainer");
        req.setEntity(new InputStreamEntity(FileUtils.getFileOrResource("ldp-put-container.ttl"), ContentType.create("text/turtle")));
        HttpResponse resp = client.execute(req);

        assertEquals("Changing a containment triple did not lead to 409 (5.2.4.1)", 409, resp.getStatusLine().getStatusCode());
    }

    @Test
    public void testPutNotChangeContainmentTriples() throws Exception {
        ngin.registerDataset("ldp.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/turtle");

        startSimulation("sim.ttl");

        CloseableHttpClient client = HttpClients.createMinimal();
        HttpPut req = new HttpPut("http://localhost:" + TEST_PORT + "/rootContainer");
        req.setEntity(new InputStreamEntity(FileUtils.getFileOrResource("ldp-put-container2.ttl"), ContentType.create("text/turtle")));
        HttpResponse resp = client.execute(req);

        assertEquals("PUT while not changing a containment triple did not work (5.2.4.1)", 204, resp.getStatusLine().getStatusCode());
    }

    @Test
    public void testDeleteContainedResource() throws Exception {
        ngin.registerDataset("ldp.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/turtle");

        startSimulation("sim.ttl");

        CloseableHttpClient client = HttpClients.createMinimal();
        HttpDelete req = new HttpDelete("http://localhost:" + TEST_PORT + "/rootContainer/resourceA");
        client.execute(req);

        assertEquals("DELETE on a contained resource did not remove the containment triple (5.2.5.1)", 1, ngin.getConnection().getStatements(null, LDP.CONTAINS, null).stream().count());
    }

    @Test
    public void testDirectContainer() throws Exception {
        ngin.registerDataset("ldp-direct.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "text/turtle");

        startSimulation("sim.ttl");

        CloseableHttpClient client = HttpClients.createMinimal();
        HttpPost req = new HttpPost("http://localhost:" + TEST_PORT + "/directContainer");
        req.setEntity(new InputStreamEntity(FileUtils.getFileOrResource("ldp-put-resource.ttl"), ContentType.create("text/turtle")));
        HttpResponse resp = client.execute(req);

        ngin.getConnection().getStatements(null, null, null, true).forEach(System.out::println);

        System.out.println(ngin.getConnection().getRepository());

        assertEquals(
            "Creating a resource in a DirectContainer did not lead to the appropriate membership triple",
            1,
            ngin.getConnection().getStatements(
                Vocabulary.VALUE_FACTORY.createIRI("http://127.0.1.1:" + TEST_PORT + "/directContainer#a"),
                Vocabulary.VALUE_FACTORY.createIRI("http://127.0.1.1:" + TEST_PORT + "/value"),
                Vocabulary.VALUE_FACTORY.createIRI(resp.getHeaders("Location")[0].getValue())
            ).stream().count()
        );
    }

    @Test
    public void testJSONValue() throws Exception {
        ngin.registerDataset("json-value.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "application/json");

        startSimulation("sim.ttl");
        String rep = getRepresentation("val", h);

        assertEquals(EXPECTED_JSON, rep);
    }

    @Test
    public void testJSONNoValue() throws Exception {
        ngin.registerDataset("json-no-value.trig").registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Accept", "application/json");

        startSimulation("sim.ttl");
        String rep = getRepresentation("val", h);

        assertEquals(0, rep.length());
    }

    private static String getRepresentation(String path, Map<String, String> headers) throws Exception {
        CloseableHttpClient client = HttpClients.createMinimal();
        HttpGet req = new HttpGet("http://localhost:" + TEST_PORT + "/" + path);

        for (Map.Entry<String, String> kv : headers.entrySet()) {
            req.addHeader(kv.getKey(), kv.getValue());
        }

        CloseableHttpResponse resp = client.execute(req);
        return IOUtil.readString(resp.getEntity().getContent());
    }

    private static Model getModel(String path, Map<String, String> headers) throws Exception {
        CloseableHttpClient client = HttpClients.createMinimal();
        HttpGet req = new HttpGet("http://localhost:" + TEST_PORT + "/" + path);

        for (Map.Entry<String, String> kv : headers.entrySet()) {
            req.addHeader(kv.getKey(), kv.getValue());
        }

        CloseableHttpResponse resp = client.execute(req);
        return Rio.parse(resp.getEntity().getContent(), "http://localhost:" + TEST_PORT + "/" + path, RDFFormat.TURTLE);
    }
    
    private static HttpResponse getResponse(String path, Map<String, String> headers) throws Exception {
        CloseableHttpClient client = HttpClients.createMinimal();
        HttpGet req = new HttpGet("http://localhost:" + TEST_PORT + "/" + path);

        for (Map.Entry<String, String> kv : headers.entrySet()) {
            req.addHeader(kv.getKey(), kv.getValue());
        }

        return client.execute(req);
    }

    // TODO code below is duplicated from SimulationEngineTest, use helper class instead

    private static int sendDummyRequest() throws Exception {
        return sendDummyRequest(new HashMap<>());
    }

    private static int sendDummyRequest(Map<String, String> headers) throws Exception {
        return sendDummyRequest("not-found", headers);
    }

    private static int sendDummyRequest(String path, Map<String, String> headers) throws Exception {
        CloseableHttpClient client = HttpClients.createMinimal();
        HttpGet req = new HttpGet("http://localhost:" + TEST_PORT + "/" + path);

        for (Map.Entry<String, String> kv : headers.entrySet()) {
            req.addHeader(kv.getKey(), kv.getValue());
        }

        CloseableHttpResponse resp = client.execute(req);
        return resp.getStatusLine().getStatusCode();
    }

    private static int startSimulation(String filename) throws Exception {
        CloseableHttpClient client = HttpClients.createMinimal();
        HttpPut req = new HttpPut("http://localhost:" + TEST_PORT + "/sim");
        req.addHeader("Content-Type", "text/turtle");
        req.setEntity(new InputStreamEntity(FileUtils.getFileOrResource(filename)));
        CloseableHttpResponse resp = client.execute(req);
        return resp.getStatusLine().getStatusCode();
    }

    private static void clearResults() {
        File[] files = new File(".").listFiles((File f) -> f.getName().endsWith(".tsv"));

        for (File f : files) {
            f.delete();
        }
    }

}
