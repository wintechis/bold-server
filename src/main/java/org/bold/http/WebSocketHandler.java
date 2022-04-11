package org.bold.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.jsonldjava.core.RDFDataset;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketHandler extends WebSocketServer implements SailConnectionListener {
	private final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);
	private List<WebSocket> sockets = new LinkedList<>();
	private SailRepositoryConnection connection;

	public WebSocketHandler(int port, SailRepository repo) {
		super(new InetSocketAddress(port));
		connection = repo.getConnection();
	}

	@Override
	public void statementAdded(Statement st) {
		StringWriter output = new StringWriter();
		Rio.write(st, output, RDFFormat.NTRIPLES);
		String payload = output.toString();
		for(WebSocket conn : sockets) {
			conn.send(payload);
		}
	}

	@Override
	public void statementRemoved(Statement st) {
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		log.info("New WebSocket connection " + conn.getResourceDescriptor());
		StringWriter output = new StringWriter();
		RDFWriter writer = Rio.createWriter(RDFFormat.NTRIPLES, output);
		connection.export(writer);
		conn.send(output.toString());
		sockets.add(conn);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		log.info("WebSocket connection " + conn.getResourceDescriptor() + " closed");
		sockets.remove(conn);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
	}

	@Override
	public void onStart() {
		log.info("WebSocket Handler started on port " + getPort());
	}
	
}
