package org.lackmann.mme.tools;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.lackmann.connectors.IPCConnector;
import org.lackmann.connectors.RESTConnector;
import org.lackmann.connectors.TCPConnector;
import org.lackmann.connectors.USBConnector;
import org.lackmann.connectors.db.InfluxConnector;
import org.lackmann.connectors.db.MeterDataRecord;
import org.openmuc.jsml.structures.OctetString;
import org.openmuc.jsml.structures.SmlFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;



public class SmlToJSONClient {

	public final static String APP_NAME = "SMLToJSON - Data Structure Converter for Receiving and Forwarding Meter Data";

	public final static String MAIN_VERSION = "0";
	public final static String SUB_VERSION  = "03";

	public final static String EMAIL = "info@lackmann.de";

	private static Options opts; 
	protected static OctetString client_id = null;
	protected static OctetString server_id = null;

	private static short Input_Mode = 0;  // 0 = none, 1 = TCP, 2 = USB

	// Input Connectors
	private static USBConnector usbCon = null;
	private static TCPConnector tcpCon = null;

	// Output Connectors
	private static IPCConnector  ipcCon = null;
	private static RESTConnector restCon = null;
	private static InfluxConnector influxCon = null;


	private static int artificialTimestamp = 1;
	private static int artificialEnergy;
	private static Random randomGenerator  = null;

	private static boolean shutdown = false;

	final static Logger LOGGER = LogManager.getLogger(SmlToJSONClient.class.getName());

	public static void main(String[] args) throws IOException, PortInUseException, UnsupportedCommOperationException 
	{

		opts = new Options();

		Runtime.getRuntime().addShutdownHook(new Thread() 
		{
			public void run() 
			{
				try 
				{
					Thread.sleep(200);
					LOGGER.debug("Shouting down ...");
					shutdown= true;
				} 
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		});

		if(!opts.processCommandLine(args))
		{
			System.exit(1);
		}

		if(opts.getUSB_Device() != null)
		{
			LOGGER.info("Reading USB-Data from '" + opts.getUSB_Device() + "'");
			usbCon = new USBConnector(opts.getUSB_Device(), opts.getRetries(), opts.getWaitTime());
		}
		if(opts.getTCP_Port() > 0)
		{
			LOGGER.info("Reading TCP-Data from '" + opts.getTCP_Address() + ":" + opts.getTCP_Port() + "'");
			tcpCon = new TCPConnector(opts.getTCP_Address(), opts.getTCP_Port(), opts.getRetries(), opts.getWaitTime());
		}

		if(opts.isArtificialData())
		{
			LOGGER.info("Creating artificial data");
			randomGenerator = new Random();
		}

		if(opts.getUSB_Device() == null && opts.getTCP_Port() <= 0 && !opts.isArtificialData())
		{
			LOGGER.warn("No input data source");
			System.exit(1);
		}

		if(opts.getIPC_Port() > 0)
		{
			LOGGER.info("Sending via IPC with '" + opts.getIPC_Peer() + ":" + opts.getIPC_Port() + "' ");
			ipcCon = new IPCConnector(opts.getIPC_Peer(), opts.getIPC_Port(), opts.getIPC_Timeout());
		}

		if(opts.getREST_url() != null)
		{
			LOGGER.info("Sending via REST with '" + opts.getREST_url() + "' ");
			restCon = new RESTConnector(opts.getREST_url(), opts.getREST_Timeout());
		}

		if(opts.getInflux_url() != null)
		{
			LOGGER.info("Sending to Influx DB with '" 
					+ opts.getInflux_url() 
					+ ":" + opts.getInflux_dbname()
					+ ":" + opts.getInflux_dsname()
					+ ":" + opts.getInflux_login()
					+ ":" + opts.getInflux_password()
					);

			influxCon = new InfluxConnector(
					opts.getInflux_url(), 
					opts.getInflux_dbname(), 
					opts.getInflux_dsname(), 
					opts.getInflux_login(), 
					opts.getInflux_password() 
					);
		}

		ArrayList <String> jsonMeterReading = new ArrayList <String>();
		ArrayList <String> IPCTelegram  = new ArrayList <String>();

		while(true)
		{
			jsonMeterReading.clear();
			IPCTelegram.clear();

			if(shutdown == true)
			{
				break;
			}

			if(opts.isArtificialData())
			{
				LOGGER.debug("Creating artificial meter data record");
				server_id = new OctetString("ARTIFICIAL");
				jsonMeterReading.add(createArtificialReading());
			}

			if(openInputDevice() == true)
			{
				SmlFileWorker smlWorker = new SmlFileWorker();

				for(int r=0; r < opts.getRetries(); r++)
				{
					if(shutdown == true)
					{
						closeInputDevice();
						break;
					}

					SmlFile tmpSmlFile = null;
					tmpSmlFile = readSMLFileFromUSB();
					if(tmpSmlFile != null)
					{
						smlWorker.setSmlFile(tmpSmlFile);
						jsonMeterReading.add(smlWorker.getSMLAsJson());
						IPCTelegram.add(smlWorker.getForIPC());							
					}
					else
					{
						tmpSmlFile = readSMLFileFromTCP();
						if(tmpSmlFile != null)
						{
							smlWorker.setSmlFile(tmpSmlFile);
							jsonMeterReading.add(smlWorker.getSMLAsJson());
							IPCTelegram.add(smlWorker.getForIPC());							
						}
						closeInputDevice();
						break;
					}

					continue;
				}
				closeInputDevice();
			}

			if(shutdown == true)
			{
				break;
			}

			if(jsonMeterReading.size() > 0 || IPCTelegram.size() > 0)
			{
				if(hasOutputDevice())
				{
					if(openOutputDevice())
					{
						processSMLReading(jsonMeterReading, IPCTelegram);

						closeOutputDevice();
					}
					else
					{
						LOGGER.warn("Unable to open any output device");
					}
				}
				else
				{
					LOGGER.warn("No output device defined. Result will be written to stdout");
					processSMLReading(jsonMeterReading, IPCTelegram);
				}
			}
			else
			{
				LOGGER.warn("No data read from any device");
			}

			try
			{
				java.util.concurrent.TimeUnit.SECONDS.sleep(opts.getWaitTime());
			} 
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}

		} // closing main reading loop


		closeInputDevice();
		closeOutputDevice();

		LOGGER.info("Shutdown completed");

	}

	private static SmlFile readSMLFileFromTCP()
	{
		if(tcpCon != null && tcpCon.hasConnection())
		{
			try
			{
				return tcpCon.getConnection().getSMLFile();
			}
			catch(java.io.IOException ex)
			{
				LOGGER.error("Error reading from TCP connection '" + 
						opts.getTCP_Address() + ":" + opts.getTCP_Port() + "'");
				if(ex.getLocalizedMessage().contains("Timeout"))
				{
					int timeout = tcpCon.getTimeout();
					if(timeout > -1)
					{
						LOGGER.error("Details: Timeout " + timeout + " ms  elapsed");
					}
					else
					{
						LOGGER.error("Details: Timeout elapsed");
					}
				}
				else
				{
					LOGGER.error("Details: \"" + ex.getLocalizedMessage() +"\"");
				}
			}

		}

		return null;
	}
	

	private static SmlFile readSMLFileFromUSB()
	{
		if(usbCon != null && usbCon.hasConnection())
		{
			try
			{
				return usbCon.getConnection().getSMLFile();
			}
			catch(java.io.IOException ex)
			{
				LOGGER.error("Error in reading from USB device '" + opts.getUSB_Device() + "'");
				if(ex.getLocalizedMessage().contains("Timeout"))
				{
					int timeout = usbCon.getTimeout();
					
					if(timeout > -1)
					{
						LOGGER.error("Details: Timeout " + timeout + " ms  elapsed");
					}
					else
					{
						LOGGER.error("Details: Timeout elapsed");
					}
				}
				else
				{
					LOGGER.error("Details: \"" + ex.getLocalizedMessage() +"\"");
				}
			}
		}

		return null;
	}
	

	private static boolean openInputDevice()
	{
		boolean status = false;

		if(usbCon != null)
		{
			status = usbCon.open();
		}

		if(tcpCon != null)
		{
			if(status)
				tcpCon.open();
			else
				status = tcpCon.open();
		}

		if(opts.isArtificialData())
		{
			status = true;
		}

		return status;
	}

	private static boolean hasOutputDevice()
	{
		return (ipcCon != null || restCon != null || influxCon != null);
	}

	private static boolean openOutputDevice()
	{
		boolean status = false;

		if(ipcCon != null)
		{
			status = ipcCon.open();
		}

		if(restCon != null)
		{
			status = restCon.open();
		}

		if(influxCon != null)
		{
			status = influxCon.open();
		}

		return status;
	}

	private static void closeInputDevice()
	{
		if(usbCon != null)
		{
			usbCon.close();
		}

		if(tcpCon != null)
		{
			tcpCon.close();
		}

		return;
	}

	private static void closeOutputDevice()
	{
		if(ipcCon != null)
		{
			ipcCon.close();
		}

		if(restCon != null)
		{
			restCon.close();
		}

		if(influxCon != null)
		{
			try
			{
				influxCon.close();
			} catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return;
	}

	private static void processSMLReading(ArrayList<String> jsonMeterReading, ArrayList<String> IPCTelegram)
	{

		if(ipcCon != null && ipcCon.hasConnection() && !IPCTelegram.isEmpty())
		{
			LOGGER.info("IPC data forwarding");
			Iterator <String> it = IPCTelegram.iterator();
			while(it.hasNext())
			{
				ipcCon.send(it.next());
			}
		}

		if(!jsonMeterReading.isEmpty())
		{
			if(restCon != null && restCon.hasConnection())
			{
				LOGGER.info("REST data forwarding");
				Iterator <String> it = jsonMeterReading.iterator();
				while(it.hasNext())
				{
					restCon.send(it.next());
				}
			}

			if(influxCon != null && influxCon.hasConnection())
			{
				LOGGER.info("InfluxDB data forwarding");
				ObjectMapper mapper = new ObjectMapper();
				Iterator <String> it = jsonMeterReading.iterator();
				while(it.hasNext())
				{
					try
					{
						MeterDataRecord mdr = mapper.readValue(it.next(), MeterDataRecord.class);
						influxCon.send(mdr);
					} catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			if(influxCon == null && restCon == null)
			{
				LOGGER.info(jsonMeterReading);
			}

		}

		return;
	}

	protected static void shutdownClient()
	{
		System.err.println("Shutdown Client ...");
		System.err.println("Done");
	}

	private static String createArtificialReading()
	{
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Instant instant = timestamp.toInstant();

		int power = randomGenerator.nextInt(100) + 100;
		artificialEnergy += (power*opts.getWaitTime()/360);
		artificialTimestamp += opts.getWaitTime();

		StringBuilder jsonReturn = new StringBuilder("{");
		jsonReturn.append("\"appliedMethod\": \"ARTIFICIAL\", ");
		jsonReturn.append("\"timestamp\": \"" + instant + "\", ");

		jsonReturn.append("\"v_1_8_0\": \"" +  artificialEnergy + "\",");
		jsonReturn.append("\"v_1_8_1\": \"-1\",");
		jsonReturn.append("\"v_1_8_2\": \"-1\",");
		jsonReturn.append("\"v_2_8_0\": \"-1\",");
		jsonReturn.append("\"power\": \"" +  power + "\",");

		jsonReturn.append("\"serverID\": \"" + server_id + "\",");
		jsonReturn.append("\"secindex\": \"" + artificialTimestamp + "\"");
		jsonReturn.append("}");

		return jsonReturn.toString();
	}

}

class Options
{

	private String USB_Device = null;

	private String TCP_Address = null;
	private int TCP_Port = -1;

	private String IPC_Peer = null;
	private int IPC_Port         = -1;
	private int IPC_Timeout      = 1000; // 1 seconds

	private String REST_Url        = null;
	private int    REST_Timeout      = 1000; // 1 seconds

	private String influx_url = null;
	private String influx_dbname;
	private String influx_dsname;
	private String influx_login;
	private String influx_password;

	private int waitTime           = 60;
	private int retries = 3;

	private boolean artificialData  = false;


	boolean processCommandLine(String[] args)
	{
		OptionParser parser = new OptionParser();
		OptionSet options = null;
		parser.accepts( "retries" ).withRequiredArg().ofType( Integer.class );
		parser.accepts( "device" ).withRequiredArg().ofType( String.class );
		parser.accepts( "ipc" ).withRequiredArg().ofType( String.class );
		parser.accepts( "ipc_timeout" ).withRequiredArg().ofType( String.class );
		parser.accepts( "wait" ).withRequiredArg().ofType( Integer.class );
		parser.accepts( "rest" ).withRequiredArg().ofType( String.class );
		parser.accepts( "rest_timeout" ).withRequiredArg().ofType( String.class );
		parser.accepts( "tcp" ).withOptionalArg().ofType( String.class );
		parser.accepts( "influx_url" ).withRequiredArg().ofType( String.class );
		parser.accepts( "influx_login" ).withRequiredArg().ofType( String.class );
		parser.accepts( "influx_password" ).withRequiredArg().ofType( String.class );
		parser.accepts( "influx_dbname" ).withRequiredArg().ofType( String.class );
		parser.accepts( "influx_dsname" ).withRequiredArg().ofType( String.class );
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
			System.out.println(SmlToJSONClient.APP_NAME);
			System.out.println("Version " + SmlToJSONClient.MAIN_VERSION + "." + SmlToJSONClient.SUB_VERSION);
			System.out.println(SmlToJSONClient.EMAIL);
			System.out.println();
		}


		boolean result = true;

		//
		// Input options
		//

		if (options.has("wait") && options.hasArgument("wait"))
		{
			waitTime = (int) options.valueOf("wait");
		}
		else
			System.out.println("'wait' for processing intermission set to default value: '" + waitTime);

		if (options.has("retries") && options.hasArgument("retries"))
		{
			retries = (int) options.valueOf("retries");
		}

		if (options.has("device") && options.hasArgument("device"))
		{
			USB_Device = (String) options.valueOf("device");
			if(USB_Device.equals("random"))
			{
				artificialData = true;
				USB_Device = null;
			}
		}

		if (options.has("tcp"))
		{
			String opStr = (String) options.valueOf("tcp");

			if(opStr != null)
			{
				String[] parts = opStr.split(":");

				if(parts.length == 2)
				{
					TCP_Address = parts[0];
					TCP_Port = Integer.parseInt(parts[1]);
				}
				else if(parts.length == 1)
				{
					TCP_Address = (String) options.valueOf("tcp");
				}
				else
				{
					System.err.println("\nInvalid value for TCP-option: " + opStr + "\n");
					TCP_Port = -1;
					result = false;
				}
			}
			else
			{		// TODO Auto-generated method stub

				result = true;
			}

			if (TCP_Port <= 1000 || TCP_Port >= 100000)
			{
				System.err.println("\nInvalid value for TCP-port: " + TCP_Port + "\n");
				TCP_Port = -1;
				result = false;
			}
		}

		if (!artificialData && USB_Device == null && TCP_Port < 0 )
		{
			System.err.println("\nNeiher USB input device option '--device' nor TCP address option '--tcp' is set!\n");
			result = false;
		}

		//
		// Output options
		//

		if (options.has("ipc") && options.hasArgument("ipc"))
		{
			String opStr = (String) options.valueOf("ipc");

			if(opStr.indexOf(':') >= 0)
			{
				String[] parts = opStr.split(":");

				if(parts.length != 2)
				{
					System.err.println("\nInvalid value for IPC-option: " + opStr + "\n");
					IPC_Port = -1;
					result = false;
				}
				else
				{
					IPC_Peer = parts[0];
					IPC_Port = Integer.parseInt(parts[1]);
				}
			}
			else
			{
				IPC_Port = Integer.parseInt(opStr);
			}

			if (IPC_Port <= 1000 || IPC_Port >= 100000)
			{
				System.err.println("\nUInvalid value for IPC-port: " + IPC_Port + "\n");
				IPC_Port = -1;
				result = false;
			}

		}

		if (options.has("rest") && options.hasArgument("rest"))
		{
			REST_Url = (String) options.valueOf("rest");
		}

		if (options.has("influx_url") && options.hasArgument("influx_url"))
		{
			if(options.has("influx_dbname") && options.hasArgument("influx_dbname"))
			{
				influx_dbname = (String) options.valueOf("influx_dbname");
			}
			else
			{
				System.err.println("\nNo Influx database name set. Use '--influx_dbname'!\n");
				result = false;				
			}

			if(options.has("influx_dsname") && options.hasArgument("influx_dsname"))
			{
				influx_dsname = (String) options.valueOf("influx_dsname");
			}	

			else
			{
				System.err.println("\nNo Influx data series name set. Use '--influx_dsname'!\n");
				result = false;				
			}

			if(result)
			{
				influx_url = (String) options.valueOf("influx_url");

				influx_login = "";
				influx_password = "";

				if(options.has("influx_login") && options.hasArgument("influx_login"))
				{
					influx_login = (String) options.valueOf("influx_login");

					if(options.has("influx_password") && options.hasArgument("influx_password"))
					{
						influx_password = (String) options.valueOf("influx_password");
					}
					else
					{
						System.err.println("\nNo Influx database password set. Use '--influx_password'!\n");
						result = false;				
					}
				}
			}

		}

		if (retries <= 0 && !artificialData)
		{
			System.err.println("\nNumber of retries option '--retries' not set!\n");
			result = false;
		}

		return result;
	}

	public int getREST_Timeout()
	{
		return REST_Timeout;
	}

	public String getREST_url()
	{
		return REST_Url;
	}

	public String getUSB_Device()
	{
		return USB_Device;
	}

	public String getTCP_Address()
	{
		return TCP_Address;
	}

	public int getTCP_Port()
	{
		return TCP_Port;
	}

	public int getIPC_Port()
	{
		return IPC_Port;
	}

	public String getIPC_Peer()
	{
		return IPC_Peer;
	}

	public int getIPC_Timeout()
	{
		return IPC_Timeout;
	}

	public String getInflux_dbname()
	{
		return influx_dbname;
	}

	public String getInflux_dsname()
	{
		return influx_dsname;
	}

	public String getInflux_url()
	{
		return influx_url;
	}

	public String getInflux_login()
	{
		return influx_login;
	}

	public String getInflux_password()
	{
		return influx_password;
	}

	public int getWaitTime()
	{
		return waitTime;
	}

	public int getRetries()
	{
		return retries;
	}

	public boolean isArtificialData()
	{
		return artificialData;
	}

}




