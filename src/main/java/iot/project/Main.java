package iot.project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uniluebeck.itm.util.logging.Logging;

public class Main {
	static int webServerPort = 8080;

	static {
		Logging.setLoggingDefaults();
	}

	public static void main(String[] args) {
		// Obtain an instance of a logger for this class
		Logger log = LoggerFactory.getLogger(Main.class);

		// Start a web server
		setupWebServer(webServerPort);
		log.info("Web server started on port " + webServerPort);
		log.info("Open http://localhost:" + webServerPort + " and/or http://localhost:" + webServerPort + "/hello");

		// Do your stuff here

	}

	public static void setupWebServer(int webServerPort) {
		// Set the web server's port
		spark.Spark.port(webServerPort);

		// Serve static files from src/main/resources/webroot
		spark.Spark.staticFiles.location("/webroot");

		// Return "Hello World" at URL /hello
		spark.Spark.get("/hello", (req, res) -> "Hello World");

		// Wait for server to be initialized
		spark.Spark.awaitInitialization();
	}

}
