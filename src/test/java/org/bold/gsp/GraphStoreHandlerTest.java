package org.bold.gsp;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bold.io.FileUtils;
import org.bold.sim.SimulationEngine;
import org.bold.sim.SimulationHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GraphStoreHandlerTest {

    private static final Integer TEST_PORT = 8080;

    private SimulationHandler handler = null;
    private SimulationEngine ngin = null;

    @Before
    public void createEngine() throws Exception {
        handler = new SimulationHandler(TEST_PORT);
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

        assert status == 406;
    }

    @Test
    public void testContentType() throws Exception {
        ngin.registrationDone();

        Map<String, String> h = new HashMap<>();
        h.put("Content-Type", "video/mp4");

        startSimulation("sim.ttl");
        int status = sendDummyRequest(h);

        assert status == 415;
    }

    @Test
    public void testNotFound() throws Exception {
        ngin.registrationDone();

        startSimulation("sim.ttl");
        int status = sendDummyRequest();

        assert status == 404;
    }

    // TODO code below is duplicated from SimulationEngineTest, use helper class instead

    private static int sendDummyRequest() throws Exception {
        return sendDummyRequest(new HashMap<>());
    }

    private static int sendDummyRequest(Map<String, String> headers) throws Exception {
        CloseableHttpClient client = HttpClients.createMinimal();
        HttpGet req = new HttpGet("http://localhost:" + TEST_PORT + "/not-found");

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
