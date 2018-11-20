/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.lackmann.mme.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.openmuc.jsml.structures.*;
import org.openmuc.jsml.structures.responses.*;
import org.openmuc.jsml.structures.requests.*;
import org.openmuc.jsml.transport.SerialReceiver;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lackmann.connectors.USBConnector;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;


import java.util.Random;


public class SmlToTCCClient {

	private static final String pfileName = "config.properties";
	private static final String user = "lackmann";
	private static final String password = "geheim";

	protected static OctetString client_id = null;
	protected static OctetString server_id = null;
	private static SmlTime actSensorTime = null;
	private static float power = -1;
	private static float energy_01 = -1;
	private static float energy_01_01 = -1;
	private static float energy_01_02 = -1;
	private static float energy_02 = -1;
	private static float energy_02_01 = -1;
	private static float energy_02_02 = -1;
	private static float energy_00 = -1;
	private static String appliedMethod  = "I-INFO";

	private static boolean hasExtendedRecord = false;

	private static String USBDevice = null;
	private static int retries;
	private static SerialReceiver receiver = null;
	private static SerialPort port = null;

	private static String TCC_url        = null;
	private static String serialID       = null;
	private static String deviceID       = null;

	private static int waitTime           = 60;

	private static boolean artificialData  = false;
	private static Random randomGenerator  = null;
	private static int artificialTimestamp = 1;

	private static Properties prop = new Properties();
	
	private static TCCClient tccClient = null;

	final static Logger LOGGER = LogManager.getLogger(SmlToTCCClient.class.getName());


	public static void main(String[] args) throws IOException, PortInUseException, UnsupportedCommOperationException 
	{

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				shutdownClient();
			}
		});


		if(!processCommandLine(args))
		{
			System.exit(1);
		}

		try {
			loadProperties();
		}
		catch (FileNotFoundException ex) {
			System.out.println("Not existing file: 'config.properties'\n");
		}		

		if(TCC_url != null)
		{
			System.err.println("Running TCC Client with '" + TCC_url + "' ");
		}

		boolean ParseSuccess = false;
		
		
		tccClient = new TCCClient(TCC_url, deviceID, serialID, user, password);

		if(deviceID == null && tccClient.getDeviceID() == null)
		{
			System.err.println("No deviceID read");
			System.exit(1);
		}
		else if(deviceID == null)
		{
			deviceID = tccClient.getDeviceID();
			prop.setProperty("deviceID", deviceID);
			writeProperty();
		}
		
		try
		{
			tccClient.openChannel();
		}
		catch (Exception e) {
			LOGGER.error("Unable to open connection");
			LOGGER.error(e.getMessage());
			System.exit(1);
		}


		if(artificialData == true)
		{
			randomGenerator = new Random();
			energy_00 = 1000;
			server_id = new OctetString("ARTIFICIAL");
			appliedMethod = "ARTIFICIAL";
			actSensorTime = null;
			hasExtendedRecord = true;
		}

			

		while(true)
		{
			ParseSuccess = false;

			if(artificialData)
			{
				createArtificialReading();
				ParseSuccess = true;
			}

			else if(openDevice() == true)
			{
				for(int r=0; r < retries; r++)
				{
					try
					{
						parseSML();
						ParseSuccess = true;
						break;
					}
					catch(java.io.IOException ex)
					{
						System.err.println("Error while reading device '" + USBDevice + "'");
						System.err.println("Details: '" + ex.getLocalizedMessage() +"'");
						continue;
					}
				}

				closeDevice();
			}

			else
			{
				System.err.println("Device '" + USBDevice + "' not ready");
			}

			if(ParseSuccess)
			{
				processSMLReading();
			}

			try
			{
				java.util.concurrent.TimeUnit.SECONDS.sleep(waitTime);
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
				break;
			}

		} // closing main reading loop

	}

	private static boolean openDevice()
	{
		boolean status = false;
		int openRetries = 3;
		int openRetryWaitTime = 1;

		for(int r=0; r < openRetries; r++)
		{

			try
			{
				port = (SerialPort) CommPortIdentifier.getPortIdentifier(USBDevice).open("SML_USB",2000);
				receiver = new SerialReceiver(port);
				status = true;
				break;
			}
			catch(java.io.IOException ex)
			{
				System.err.println("Unable to open device '" + USBDevice + "'");
				System.err.println("Details: '" + ex.getLocalizedMessage() +"'");
				// System.exit(1);
			} 
			catch (NoSuchPortException ex)
			{
				System.err.println("Device '" + USBDevice + "' not existing");
				System.err.println("Details: '" + ex.getLocalizedMessage() +"'");
				// System.exit(1);
			}
			catch(PortInUseException ex)
			{
				System.err.println("Device '" + USBDevice + "' already in use");
				System.err.println("Details: '" + ex.getLocalizedMessage() +"'");
				// System.exit(1);
			}

			try
			{
				java.util.concurrent.TimeUnit.SECONDS.sleep(openRetryWaitTime);
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
				break;
			}

		}

		return status;
	}


	private static void closeDevice()
	{
		if(port != null)
			port.close();

		if(receiver != null)
		{
			try
			{
				receiver.closeStream();
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return;
	}

	private static void processSMLReading()
	{
		String jsonMeterReading = writeAsJsonToString();

		if(TCC_url != null)
		{
			try
			{
				//System.err.println("Send to REST (" + TCC_url + "): " + jsonMeterReading);
				// SendToRESTUrl(jsonMeterReading);
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}

		}

		return;
	}

	private static void writeProperty()
	{
		OutputStream output = null;

		try {

			output = new FileOutputStream("config.properties");

			// save properties to project root folder
			prop.store(output, null);

		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}

	protected static void shutdownClient()
	{
		System.err.println("Shutdown Client ...");
		System.err.println("Done");
	}

	private static void parseSML() throws java.io.IOException
	{
		power = -1;
		energy_00 = -1;
		energy_01 = -1;
		energy_01_01 = -1;
		energy_01_02 = -1;
		energy_02 = -1;
		energy_02_01 = -1;
		energy_02_02 = -1;

		SmlFile smlFile = receiver.getSMLFile();
		List<SmlMessage> smlMessages = smlFile.getMessages();

		for (int i = 0; i < smlMessages.size(); i++) {
			SmlMessage sml_message = smlMessages.get(i);

			EMessageBody tag = EMessageBody.toEnum(sml_message.getMessageBody().getTag().getVal());
			switch (tag) {
			case OPEN_REQUEST:
				SmlPublicOpenReq sml_PublicOpenReq = (SmlPublicOpenReq) sml_message.getMessageBody().getChoice();
				client_id = sml_PublicOpenReq.getClientId();
				break;
			case OPEN_RESPONSE:
				//				System.err.println("Got OpenResponse");
				break;
			case CLOSE_REQUEST:
				System.err.println("Got CloseRequest");
				break;
			case CLOSE_RESPONSE:
				//				System.err.println("Got CloseResponse");
				break;
			case GET_PROFILE_PACK_REQUEST:
				System.err.println("Got GetProfilePackRequest");
				break;
			case GET_PROFILE_PACK_RESPONSE:
				System.err.println("Got GetProfilePackResponse");
				break;
			case GET_PROFILE_LIST_REQUEST:
				System.err.println("Got GetProfileListRequest");
				break;
			case GET_PROFILE_LIST_RESPONSE:
				System.err.println("Got GetProfileListResponse");
				break;
			case GET_PROC_PARAMETER_REQUEST:
				System.err.println("Got GetProcParameterRequest");
				break;
			case GET_PROC_PARAMETER_RESPONSE:
				System.err.println("Got GetProcParameterResponse");
				break;
			case SET_PROC_PARAMETER_REQUEST:
				System.err.println("Got SetProcParameterRequest");
				break;
			case SET_PROC_PARAMETER_RESPONSE:
				System.err.println("Got SetProcParameterResponse");
				break;
			case GET_LIST_REQUEST:
				System.err.println("Got GetListRequest");
				break;
			case GET_LIST_RESPONSE:

				SmlGetListRes resp = (SmlGetListRes) sml_message.getMessageBody().getChoice();
				server_id = resp.getServerId();
				actSensorTime = resp.getActSensorTime();

				SmlList smlList = resp.getValList();
				SmlListEntry[] list = smlList.getValListEntry();

				for (SmlListEntry entry : list) {
					if(entry.getUnit().getVal() == 30 || entry.getUnit().getVal() == 27)
					{
						SmlValue sml_value = entry.getValue();
						// System.err.println("Object Name: " + entry.getObjName().toString());
						// System.err.println("Object Unit: " + entry.getUnit().getVal());
						// System.err.println("Object Scaler: " + entry.getScaler());
						int scaler = entry.getScaler().getIntVal();
						ASNObject obj = sml_value.getChoice();
						// System.err.println("SmlValue Type = " + obj.getClass().getName());
						// System.err.println("SmlValue Value as String = " + obj.toString());
						long value = 0;
						if (obj.getClass().equals(Integer8.class)) {
							// System.err.println("SmlValue Value= " + ((Integer8) obj).getVal());
							value = ((Integer8) obj).getVal();
						}
						else if (obj.getClass().equals(Integer16.class)) {
							// System.err.println("SmlValue Value= " + ((Integer16) obj).getVal());
							value = ((Integer16) obj).getVal();
						}
						else if (obj.getClass().equals(Integer32.class)) {
							// System.err.println("SmlValue Value= " + ((Integer32) obj).getVal());
							value = ((Integer32) obj).getVal();
						}
						else if (obj.getClass().equals(Integer64.class)) {
							// System.err.println("SmlValue Value= " + ((Integer64) obj).getVal());
							value = ((Integer64) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned8.class)) {
							// System.err.println("SmlValue Value= " + ((Unsigned8) obj).getVal());
							value = ((Unsigned8) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned16.class)) {
							// System.err.println("SmlValue Value= " + ((Unsigned16) obj).getVal());
							value = ((Unsigned16) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned32.class)) {
							// System.err.println("SmlValue Value= " + ((Unsigned32) obj).getVal());
							value = ((Unsigned32) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned64.class)) {
							// System.err.println("SmlValue Value= " + ((Unsigned64) obj).getVal());
							value = ((Unsigned64) obj).getVal();
						}
						else if (obj.getClass().equals(OctetString.class)) {
							//							OctetString strVal = (OctetString) obj;
						}
						else {
							//							System.err.println(obj.getClass().getName());
						}

						// System.err.println("Value = " + value);

						if(entry.getUnit().getVal() == 30)
						{
							// System.err.println(entry.getObjName().toString());
							if(entry.getObjName().toString().equals("01 00 01 08 00 FF"))
							{
								// System.err.println("Unit: " + entry.getUnit().getVal());
								energy_01 = (float) (value * Math.pow(10,scaler));
							}
							// System.err.println(entry.getObjName().toString());
							else if(entry.getObjName().toString().equals("01 00 01 08 01 FF"))
							{
								// System.err.println("Unit: " + entry.getUnit().getVal());
								energy_01_01 = (float) (value * Math.pow(10,scaler));
							}
							// System.err.println(entry.getObjName().toString());
							else if(entry.getObjName().toString().equals("01 00 01 08 02 FF"))
							{
								// System.err.println("Unit: " + entry.getUnit().getVal());
								energy_01_02 = (float) (value * Math.pow(10,scaler));
							}
							else if(entry.getObjName().toString().equals("01 00 02 08 00 FF"))
							{
								// System.err.println("Unit: " + entry.getUnit().getVal());
								energy_02 = (float) (value * Math.pow(10,scaler));
							}
							else if(entry.getObjName().toString().equals("01 00 02 08 01 FF"))
							{
								// System.err.println("Unit: " + entry.getUnit().getVal());
								energy_02_01 = (float) (value * Math.pow(10,scaler));
							}
							else if(entry.getObjName().toString().equals("01 00 02 08 02 FF"))
							{
								// System.err.println("Unit: " + entry.getUnit().getVal());
								energy_02_02 = (float) (value * Math.pow(10,scaler));
							}
							else
							{
								// System.err.println("Unit: " + entry.getUnit().getVal());
								energy_00 = (float) (value * Math.pow(10,scaler));
							}
						}
						else
						{
							// System.err.println("OBIS: " + entry.getObjName().toString());
							power = (float) (value * Math.pow(10,scaler));
							hasExtendedRecord = true;
						}
					}
				}

				if(energy_00 == -1 && energy_01 > 0)
				{
					energy_00 = energy_01;
				}

				break;
			case ATTENTION_RESPONSE:
				System.err.println("Got AttentionResponse");
				break;
			default:
				System.err.println("type not found");
			}
		}
	}


	private static void createArtificialReading()
	{
		power = randomGenerator.nextInt(100) + 100;
		energy_00 += (power*waitTime/360);
		energy_01 = -1;
		energy_02 = -1;
		artificialTimestamp += waitTime;

		return;
	}

	private static boolean processCommandLine(String[] args)
	{
		OptionParser parser = new OptionParser();
		OptionSet options = null;
		parser.accepts( "retries" ).withRequiredArg().ofType( Integer.class );
		parser.accepts( "device" ).withRequiredArg().ofType( String.class );
		parser.accepts( "wait" ).withRequiredArg().ofType( Integer.class );
		parser.accepts( "tcc" ).withRequiredArg().ofType( String.class );
		parser.accepts( "serialID" ).withRequiredArg().ofType( String.class );

		try
		{
			options = parser.parse(args);
		}
		catch(OptionException ex)
		{
			System.err.println("Error in reading command line: '" + ex.getLocalizedMessage() +"'");
			return false;

		}

		boolean result = true;

		if (options.has("wait") && options.hasArgument("wait"))
		{
			waitTime = (int) options.valueOf("wait");
		}

		if (options.has("retries") && options.hasArgument("retries"))
		{
			retries = (int) options.valueOf("retries");
		}

		if (options.has("device") && options.hasArgument("device"))
		{
			USBDevice = (String) options.valueOf("device");
			if(USBDevice.equals("random"))
			{
				artificialData = true;
			}
		}


		if (options.has("tcc") && options.hasArgument("tcc"))
		{
			TCC_url = (String) options.valueOf("tcc");
		}

		if (options.has("tcc") && options.hasArgument("tcc"))
		{
			serialID = (String) options.valueOf("serialID");
		}

		if (USBDevice == null)
		{
			System.err.println("\nUSB input device option '-device' not set!\n");
			result = false;
		}

		if (retries <= 0 && !artificialData)
		{
			System.err.println("\nNumber of retries option '-retries' not set!\n");
			result = false;
		}

		if (TCC_url == null)
		{
			System.err.println("\nTCC URL option '-tcc' not set!\n");
			result = false;
		}

		if (serialID == null)
		{
			System.err.println("\nserialID option '-serialID' not set!\n");
			result = false;
		}

		return result;
	}

	private static String writeAsJsonToString()
	{
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Instant instant = timestamp.toInstant();

		String meterReading = String.format(
				Locale.ROOT,
				"{appliedMethod: '%s', serverID: '%s', timestamp: '%s', secindex: %d, value: %.4f, value_01: %.4f",
				appliedMethod  ,
				server_id.toHexString(),
				instant.toString(),
				artificialData? artificialTimestamp : ((Unsigned32)actSensorTime.getChoice()).getVal(),
						energy_00,
						energy_01);

		if(energy_01_01 > 0)
		{
			meterReading = meterReading +  String.format(
					Locale.ROOT,
					", energy_01_01: %.4f",
					energy_01_01);
		}
		if(energy_01_02 > 0)
		{
			meterReading = meterReading +  String.format(
					Locale.ROOT,
					", energy_01_02: %.4f",
					energy_01_02);
		}

		meterReading = meterReading +  String.format(
				Locale.ROOT,
				", energy_02: %.4f",
				energy_02);

		if(energy_02_01 > 0)
		{
			meterReading = meterReading +  String.format(
					Locale.ROOT,
					", energy_02_01: %.4f",
					energy_02_01);
		}
		if(energy_02_02 > 0)
		{
			meterReading = meterReading +  String.format(
					Locale.ROOT,
					", energy_02_02: %.4f",
					energy_02_02);
		}

		meterReading = meterReading +  String.format(
				Locale.ROOT,
				", power: %.4f}",
				power);

		return meterReading;

	}

	private static void loadProperties() throws IOException

	{
		InputStream input = null;

		input = new FileInputStream(pfileName);

		// load a properties file
		prop.load(input);
		deviceID = prop.getProperty("deviceID");

		// get the property value and print it out
		System.err.println("Running with diviceID = " + deviceID);

		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}




