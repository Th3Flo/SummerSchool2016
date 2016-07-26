package iot.project;

import io.netty.util.internal.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import scala.Product2;
import scala.Tuple2;
import com.google.common.collect.Lists;
import de.farberg.spark.examples.streaming.ServerSocketSource;
import de.uniluebeck.itm.util.logging.Logging;

public class Main {
	private static JavaPairDStream<String,String> HouseData;
	static int webServerPort = 8080;
	private static final String host = "localhost";

	static {
		Logging.setLoggingDefaults();
	}
	
public static String datagenerate (){
		
		int hid = ThreadLocalRandom.current().nextInt(1, 2 + 1);
		int rid = ThreadLocalRandom.current().nextInt(1, 6 + 1);
		int io = ThreadLocalRandom.current().nextInt(0, 1 + 1);
		
		String hdata= hid + "," + rid + "," + io;
		return hdata;
	}

	public static void main(String[] args) {
		// Obtain an instance of a logger for this class
		Logger log = LoggerFactory.getLogger(Main.class);

		// Start a web server
		setupWebServer(webServerPort);
		log.info("Web server started on port " + webServerPort);
		log.info("Open http://localhost:" + webServerPort + " and/or http://localhost:" + webServerPort + "/hello");

		// Do your stuff here
		// Create a server socket data source that sends string values
				ServerSocketSource<String> dataSource = new ServerSocketSource<>(() -> {return datagenerate();}, () -> 1000);

				// Create the context with a 1 second batch size
				SparkConf sparkConf = new SparkConf().setAppName("JavaNetworkWordCount").setMaster("local[2]");
				JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));

				// Create a JavaReceiverInputDStream on target ip:port and count the words in input stream of \n delimited text
				JavaReceiverInputDStream<String> lines = ssc.socketTextStream(host, dataSource.getLocalPort(), StorageLevels.MEMORY_AND_DISK_SER);
												
				HouseData = lines.mapToPair(x -> new Tuple2<String, String>(x.split(",")[0] + "-" + x.split(",")[1] , x.split(",")[2]))
						.reduceByKey((i1, i2) -> i2);
				
				
				HouseData.print();
				
				

				ssc.start();

				ssc.awaitTermination();
				ssc.close();
				dataSource.stop();

	}

	public static void setupWebServer(int webServerPort) {
		// Set the web server's port
		spark.Spark.port(webServerPort);

		// Serve static files from src/main/resources/webroot
		spark.Spark.staticFiles.location("/webroot");

		// Return "Hello World" at URL /hello
		spark.Spark.get("/hello", (req, res) -> "Hello World");
		
//		spark.Spark.get("/test", (req, res) -> HouseData.print());


		// Wait for server to be initialized
		spark.Spark.awaitInitialization();
	}

}
