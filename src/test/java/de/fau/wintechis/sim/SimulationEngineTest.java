package de.fau.wintechis.sim;

import de.fau.wintechis.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SimulationEngineTest {

    private static final Integer TEST_PORT = 8080;

    /**
     * Time in ms. Simulation duration: ~500ms
     */
    private static final long SHORTER_THAN_RUN = 150;

    /**
     * See comment above.
     */
    private static final long LONGER_THAN_RUN = 750;

    private SimulationEngine ngin = null;

    @Before
    public void createEngine() {
        ngin = new SimulationEngine(TEST_PORT);
    }

    @After
    public void closeEngine() {
        try {
            ngin.terminate();
        } catch (Exception e) {
            e.printStackTrace();
            // and ignore...
        }
    }

    @Test
    public void testConstructor() throws Exception {
        int status = sendDummyRequest();

        assert status == 404;
    }

    @Test
    public void testConfigure() {
        // TODO query/update correct behavior
    }

    //@Test FIXME init now called directly after agent starts, not after configuration
    public void testInit() throws Exception {
        ngin.registrationDone();

        assert ask("sim-initialized.rq", ngin.getConnection());
    }

    @Test
    public void testRun() throws Exception {
        ngin.registrationDone();

        int status = startSimulation("sim.ttl");

        assert status == 201;
    }

    @Test
    public void testUpdate() throws Exception {
        ngin.registrationDone();

        startSimulation("sim.ttl");

        TimeUnit.MILLISECONDS.sleep(SHORTER_THAN_RUN);

        assert ask("sim-updated.rq", ngin.getConnection());
    }

    @Test
    public void testUpdateTime() throws Exception {
        ngin.registerQuery("sim-dates.rq").registrationDone();

        startSimulation("sim-full.ttl");

        TimeUnit.MILLISECONDS.sleep(LONGER_THAN_RUN);

        // FIXME is in the server's root folder... Should be in a test folder
        File tsv = new File("sim-dates.tsv");

        assert tsv.exists();

        String buf = FileUtils.asString(new FileInputStream(tsv));

        assert buf.equals("2020\n2020\n2020\n2020\n2020\n2020\n"); // FIXME why does minutes() not return 0, 1, 2...?
    }

    @Test
    public void testUpdateDefaultTime() throws Exception {
        ngin.registerQuery("sim-dates.rq").registrationDone();

        startSimulation("sim-default.ttl");

        TimeUnit.MILLISECONDS.sleep(LONGER_THAN_RUN);

        // FIXME is in the server's root folder... Should be in a test folder
        File tsv = new File("sim-dates.tsv");

        assert tsv.exists();

        String buf = FileUtils.asString(new FileInputStream(tsv));

        assert buf.equals("1970\n1970\n1970\n1970\n1970\n1970\n"); // FIXME why does minutes() not return 0, 1, 2...?
    }

    @Test
    public void testReplay() throws Exception {
        ngin.registerQuery("sim-count.rq").registrationDone();

        startSimulation("sim.ttl");

        TimeUnit.MILLISECONDS.sleep(LONGER_THAN_RUN);

        // FIXME is in the server's root folder... Should be in a test folder
        File tsv = new File("sim-count.tsv");

        assert tsv.exists();

        String buf = FileUtils.asString(new FileInputStream(tsv));

        assert buf.equals("0\n1\n2\n3\n4\n5\n");
    }

    @Test
    public void testClean() throws Exception {
        ngin.registrationDone();

        long initSize = ngin.getConnection().size();

        startSimulation("sim.ttl");

        TimeUnit.MILLISECONDS.sleep(LONGER_THAN_RUN);

        long endSize = ngin.getConnection().size();

        assert endSize == initSize;
    }

    @Test
    public void testReplayTwice() throws Exception {
        ngin.registerQuery("sim-count.rq").registrationDone();

        startSimulation("sim.ttl");

        TimeUnit.MILLISECONDS.sleep(LONGER_THAN_RUN);

        // FIXME same as testReplay
        File tsv = new File("sim-count.tsv");
        tsv.delete();

        startSimulation("sim.ttl");

        TimeUnit.MILLISECONDS.sleep(LONGER_THAN_RUN);

        assert tsv.exists();

        String buf = FileUtils.asString(new FileInputStream(tsv));

        assert buf.equals("0\n1\n2\n3\n4\n5\n");
    }

    private static boolean ask(String filename, RepositoryConnection con) throws IOException {
        String buf = FileUtils.asString((FileUtils.getFileOrResource(filename)));
        return con.prepareBooleanQuery(buf).evaluate();
    }

    private static int sendDummyRequest() throws Exception {
        CloseableHttpClient client = HttpClients.createMinimal();
        HttpGet req = new HttpGet("http://localhost:" + TEST_PORT + "/");
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

}
