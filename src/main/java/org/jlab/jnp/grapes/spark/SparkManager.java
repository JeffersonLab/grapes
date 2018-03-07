package org.jlab.jnp.grapes.spark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

//import database.objects.StatusChangeDB;

public enum SparkManager {
	INSTANCE;

	// A name for the spark instance. Can be any string
	private static String appName = "StandAloneConnection";
	// Pointer / URL to the Spark instance - embedded
	private static String sparkMaster = "local[*]";
	private static String tempDir = "spark-warehouse";

	private static SparkSession sparkSession = null;

  // set up a connection
	private static void getConnection() {

		if (sparkSession == null) {
                    /* --- commented by Gagik
			this.sparkSession = SparkSession.builder().appName(appName).master(sparkMaster)
					.config("spark.sql.warehouse.dir", tempDir).getOrCreate();
                    */
		}

	}
  //public socket fo creating a session
	public static SparkSession getSession() {
		if (sparkSession == null) {
			getConnection();
		}
		return sparkSession;
	}

  //for connection to a mySQL database
	private static Map<String, String> jdbcOptions() {
		return findDomain(getHostName());
	}

	public static String jdbcAppendOptions() {

		return SparkManager.jdbcOptions().get("url") + "&user=" + SparkManager.jdbcOptions().get("user") + "&password="
				+ SparkManager.jdbcOptions().get("password");

	}

	public static Dataset<Row> mySqlDataset() {
		SparkSession spSession = getSession();
		spSession.sql("set spark.sql.caseSensitive=false");
		// Dataset<Row> demoDf =
		// spSession.read().format("jdbc").options(jdbcOptions()).load();

		Dataset<Row> demoDf = spSession.read().format("jdbc").options(jdbcOptions()).option("inferSchema", true)
				.option("header", true).option("comment", "#").load();

		return demoDf;
	}

  //sometimes I want to hold the instance, this is a bit of an obsolete way, but I keep it in case
	public static void hold() {
		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

  //Here is an encoder I use for the DC database. This encoder can be any class, but this is how it has to be defined by Spark

        /* ---- commented by Gagik
        public static Encoder<StatusChangeDB> statusChangeDBEncoder() {
		return Encoders.bean(StatusChangeDB.class);
	}
        */
  //I test this software on several machine. Because I use SQL, I need to know which machine I am testing on, i.e. my laptop, my work iMac, or Jlabs cluster
	private static String getHostName() {
		String retString = null;
		try {

			// run the Unix "hostname" command
			// using the Runtime exec method:
			Process p = Runtime.getRuntime().exec("hostname");

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			retString = stdInput.readLine();

		} catch (IOException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}
		return retString;
	}

  //Addresses for the mySQL connection
	private static Map<String, String> findDomain(String str) {
		Map<String, String> jdbcOptions = new HashMap<String, String>();

		if (str.contains("ikp")) {
			jdbcOptions.put("url", "jdbc:mysql://localhost:3306/test?jdbcCompliantTruncation=false");
			jdbcOptions.put("driver", "com.mysql.jdbc.Driver");
			jdbcOptions.put("dbtable", "status_change");
			jdbcOptions.put("user", "root");
			jdbcOptions.put("password", "");
		} else if (str.contains("jlab.org")) {
			jdbcOptions.put("url", "jdbc:mysql://clasdb:3306/dc_chan_status?jdbcCompliantTruncation=false");
			jdbcOptions.put("driver", "com.mysql.jdbc.Driver");
			jdbcOptions.put("dbtable", "status_change");
			jdbcOptions.put("user", "clasuser");
			jdbcOptions.put("password", "");
		} else if (str.contains("Mike-Kunkels")) {
			jdbcOptions.put("url", "jdbc:mysql://localhost:3306/test?jdbcCompliantTruncation=false");
			jdbcOptions.put("driver", "com.mysql.jdbc.Driver");
			jdbcOptions.put("dbtable", "status_change");
			jdbcOptions.put("user", "clasuser");
			jdbcOptions.put("password", "");
		} else {
			System.err.println("On an unknown server. Please use on ifarm1402 or ifarm1401");
			System.exit(-1);
		}
		return jdbcOptions;
	}

	public void shutdown() {
		SparkManager.sparkSession.stop();
	}

	public static void restart() {
		SparkSession.clearActiveSession();
	}
}
