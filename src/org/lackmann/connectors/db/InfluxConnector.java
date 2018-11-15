package org.lackmann.connectors.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class InfluxConnector {

	public final static String APP_NAME = "db.influx.connector - Connector to Infux DB Containing Meter Data";

	public final static String MAIN_VERSION = "0";
	public final static String SUB_VERSION  = "01";

	public final static String EMAIL = "info@lackmann.de";


	private String db_url = null;
	private String db_name = null;
	private String ds_name = null;
	private String db_login = null;
	private String db_passwd = null;

	private static Connection conn = null;
	private static Statement statement = null;

	private int maxDummyRecords = 10;

	public ResultSet rs = null;

	private InfluxDB influxDB = null;

	private String rpName;

	final static Logger LOGGER = LogManager.getLogger(InfluxConnector.class.getName());


	public static void main( String args[] ) 
	{

		Options opt = new Options();

		if(!opt.processCommandLine(args))
		{
			System.exit(1);
		}

		InfluxConnector db = new InfluxConnector(opt.get_db_url(), opt.get_db_name(), opt.get_ds_name(), opt.get_db_login(), opt.get_db_passwd());

		if(db.open() == false)
		{
			LOGGER.error("Unable to connect to InfluxDB: " + 
					opt.get_db_url() + ", " + opt.get_db_name() + ", " + opt.get_ds_name());
			System.exit(1);
		}

		db.createDummyRecords();

		List<MeterDataRecord> result = null;
		try
		{
			result = db.selectAll();
		} catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(result!=null)
		{
			Iterator<MeterDataRecord> it = result.iterator();
			while(it.hasNext())
			{
				System.out.println(it.next());
			}
		}
		else
		{
			LOGGER.info("No results selected");
		}

		try
		{
			db.deleteAll();
		} 
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		db.cleanup();
	}

	public InfluxConnector(String db_url, String db_name, String ds_name, String db_login, String db_passwd) 
	{
		this.db_url = db_url;
		this.db_name = db_name;
		this.ds_name = ds_name;
		this.db_login = db_login;
		this.db_passwd = db_passwd;

	}

	@SuppressWarnings({ "deprecation"})
	public boolean open() 
	{		
		influxDB = InfluxDBFactory.connect(db_url, db_login, db_passwd);

		if(influxDB == null)
		{
			return false;
		}

		influxDB.createDatabase(db_name);
		influxDB.setDatabase(db_name);

		rpName = "aRetentionPolicy";
		influxDB.createRetentionPolicy(rpName, db_name, "30d", "30m", 2, true);
		influxDB.setRetentionPolicy(rpName);

		influxDB.enableBatch(BatchOptions.DEFAULTS);

		return true;
	}

	public boolean hasConnection()
	{
		if(influxDB == null)
			return false;
		return true;
	}

	public InfluxDB getConnection()
	{
		return influxDB;
	}


	public void createDummyRecords() {

		LOGGER.info("Create Dummy Records");
		for(int i=0; i<maxDummyRecords; i++)
		{
			float V_1_8_0 = (float) (Math.random()*50 + 1);
			int Power = (int )(Math.random() * 50 + 1);
			int seconds = (int) (System.currentTimeMillis() / 1000l);

			insertRecord(seconds,
					Instant.now().toString(),
					V_1_8_0,
					0,
					0,
					0,
					Power,
					"Dummy"
					);
			try
			{
				Thread.sleep(2000);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public List<MeterDataRecord> selectAll() throws SQLException
	{
		LOGGER.info("Select all from data set '" + ds_name + "'");

		return getMeterData("Select * from " + ds_name);
	}

	public List<MeterDataRecord> selectAll_Ascending() throws SQLException
	{
		return getMeterData("Select * from " + ds_name);
	}

	public  List<MeterDataRecord>  selectLastInsertedRecord() throws SQLException
	{
		return getMeterData("Select * from " + ds_name);
	}

	private List<MeterDataRecord> getMeterData(String query) {

		// Run the query
		Query queryObject = new Query(query, db_name);
		QueryResult queryResult = influxDB.query(queryObject);

		LOGGER.debug("Result size: " + queryResult.getResults().size());

		// Map it
		InfluxDBResultMapper resultMapper = new InfluxDBResultMapper();
		return resultMapper.toPOJO(queryResult, MeterDataRecord.class);
	}

	public void deleteAll() throws SQLException
	{
		LOGGER.debug("Delete database");
		influxDB.deleteDatabase(db_name);
	}

	public void cleanup() 
	{
		influxDB.dropRetentionPolicy(rpName, db_name);
		influxDB.deleteDatabase(db_name);
	}

	public boolean send(MeterDataRecord mdr)
	{
		return insertRecord(
				mdr.getSecindex(),
				mdr.getTimestamp(),
				mdr.getV_1_8_0(),
				mdr.getV_1_8_1(),
				mdr.getV_1_8_2(),
				mdr.getV_2_8_0(),
				mdr.getPower(),
				mdr.getServerID()
				);
	}

	public boolean insertRecord(int secindex, String timestamp, double V_1_8_0, double V_1_8_1, double V_1_8_2, double V_2_8_0, 
			int Power, String ServerID)
	{
		LOGGER.info("Insert Record into dataset '" + ds_name + "'");


		influxDB.write(Point.measurement(ds_name)
				.time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.addField("secindex", secindex)
				.addField("ServerID", ServerID)
				.addField("V_1_8_0", V_1_8_0)
				.addField("V_1_8_1", V_1_8_1)
				.addField("V_1_8_2", V_1_8_2)
				.addField("V_2_8_0", V_2_8_0)
				.addField("Power", Power)
				.build());

		return true;
	}

	public void close() throws SQLException
	{
		LOGGER.info("Closing connection to database '" 
				+ db_url + ":"
				+ db_name + ":"
				+ ds_name 
				+ "'");
		influxDB.close();
	}

	public String get_db_name()
	{
		return db_name;
	}

	public String get_ds_name()
	{
		return ds_name;
	}

	public String getDb_url()
	{
		return db_url;
	}
}

class Options
{
	private String db_url = "http://127.0.0.1:8086";
	private String db_name = "openhab_db";
	private String ds_name = "MeterData";
	private String db_login = "openhab";
	private String db_passwd = "AnotherSuperbPassword456-";


	public boolean processCommandLine(String[] args)
	{
		OptionParser parser = new OptionParser();
		OptionSet options = null;
		parser.accepts( "url" ).withRequiredArg().ofType( String.class );
		parser.accepts( "login" ).withRequiredArg().ofType( String.class );
		parser.accepts( "password" ).withRequiredArg().ofType( String.class );
		parser.accepts( "dbname" ).withRequiredArg().ofType( String.class );
		parser.accepts( "dsname" ).withRequiredArg().ofType( String.class );
		parser.accepts( "version" );

		try
		{
			options = parser.parse(args);
		}
		catch(OptionException ex)
		{
			System.err.println("Error in reading command line: '" + ex.getLocalizedMessage() +"'");
			return false;

		}

		if (options.has("version"))
		{
			System.out.println();
			System.out.println(InfluxConnector.APP_NAME);
			System.out.println("Version " + InfluxConnector.MAIN_VERSION + "." + InfluxConnector.SUB_VERSION);
			System.out.println(InfluxConnector.EMAIL);
			System.out.println();
		}


		if (options.has("url") && options.hasArgument("url"))
		{
			db_url = (String) options.valueOf("url");
		}

		if (options.has("dbname") && options.hasArgument("dbname"))
		{
			db_name = (String) options.valueOf("dbname");
		}

		if (options.has("dsname") && options.hasArgument("dsname"))
		{
			ds_name = (String) options.valueOf("dsname");
		}

		if (options.has("login") && options.hasArgument("login"))
		{
			db_login = (String) options.valueOf("login");
		}

		if (options.has("password") && options.hasArgument("password"))
		{
			db_passwd = (String) options.valueOf("password");
		}

		return true;
	}

	public String get_db_url()
	{
		return db_url;
	}

	public String get_db_name()
	{
		return db_name;
	}

	public String get_ds_name()
	{
		return ds_name;
	}

	public String get_db_login()
	{
		return db_login;
	}

	public String get_db_passwd()
	{
		return db_passwd;
	}
}