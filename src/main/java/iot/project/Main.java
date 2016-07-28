package iot.project;

import java.io.File;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.io.Files;

import de.farberg.spark.Socket.Server.ServerSocketSource;
import de.uniluebeck.itm.util.logging.Logging;
import io.netty.util.internal.ThreadLocalRandom;
import scala.Tuple2;

public class Main {
	
	// Dirty Workaround for data stream
   private static JavaPairDStream<String, Integer> HouseData;
   static int webServerPort = 8080;
   private static final String host = "localhost";
   private static final String MTP = null;
   static {
       Logging.setLoggingDefaults();
   }
   // Necessary since not using kafka due to time limitation
   public static String dataAsJs = "{}";
   
   // Generating random data which would be captured by the sensors in the house 
   // Data will look like Houseid, Roomid, io
   // io: 1 = Person in the Room --> Light should be turned on
   //	  0 = no Person in the Room --> Light should be turned off
   // example: 1,1,0   
   public static String datagenerate() {

       int hid = ThreadLocalRandom.current().nextInt(1, 1 + 1);
       int rid = ThreadLocalRandom.current().nextInt(1, 15 + 1);
       int io = ThreadLocalRandom.current().nextInt(0, 1 + 1);

       String hdata = hid + "," + rid + "," + io;
       return hdata;
   }

   
   public static void main(String[] args) {
       // Obtain an instance of a logger for this class
       Logger log = LoggerFactory.getLogger(Main.class);

       // Start a web server
       setupWebServer(webServerPort);
       log.info("Web server started on port " + webServerPort);


       // Create a server socket data source that sends string values
       ServerSocketSource<String> dataSource = new ServerSocketSource<>(() -> {
           return datagenerate();
       }, () -> 1000);

       // Create the context with a 1 second batch size
       SparkConf sparkConf = new SparkConf().setAppName("SmartX").setMaster("local[2]");
       JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));

       File myTempDir = Files.createTempDir();
       ssc.checkpoint(myTempDir.getAbsolutePath());

       // Create a JavaReceiverInputDStream on target ip:port 
       JavaReceiverInputDStream<String> lines = ssc.socketTextStream(host, dataSource.getLocalPort(), StorageLevels.MEMORY_AND_DISK_SER);
       
       // Filling the javaPairDStream with Tuples containing Houseid-Roomid as Key and io as value 
       HouseData = lines
               .mapToPair(x -> new Tuple2<String, Integer>(x.split(",")[0] + "-" + x.split(",")[1], Integer.parseInt(x.split(",")[2])))
               .reduceByKey((i1, i2) -> i2)
               
       // Keep historic Data of the Room if no update for the Room is streamed
               .updateStateByKey((values, state) -> {
                   int sum;
                   if (values.size() != 0) {
                       sum = values.get(values.size() - 1);
                   } else {
                       sum = Integer.parseInt(state.get().toString());
                   }

                   return Optional.of(sum);
               });

       // Converting RDD to Jason in order to be accessible through webserver
       HouseData.foreachRDD((i) -> {
    	   dataAsJs = "{";
           i.foreach((i2) -> {
        	   dataAsJs += ("\"" + i2._1 + "\":" + i2._2() +  "," );
           });
           dataAsJs = dataAsJs.substring(0,dataAsJs.length()-1);
           dataAsJs = dataAsJs +  "}";
       });
       
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

       // Return the House Data through HTTP
       spark.Spark.get("/lightData", (req, res) -> {
           return dataAsJs;
       });

       // Wait for server to be initialized
       spark.Spark.awaitInitialization();
   }

}