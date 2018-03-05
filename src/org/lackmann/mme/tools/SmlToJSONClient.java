/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.lackmann.mme.tools;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

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

import co.gongzh.procbridge.ProcBridge;
import co.gongzh.procbridge.ProcBridgeException;
import org.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;
import org.apache.http.client.fluent.Request;

import java.util.Random;


public class SmlToJSONClient {

	protected static OctetString client_id = null;
	protected static OctetString server_id = null;
	private static SmlTime actSensorTime = null;
	private static int power = -1;
	private static int energy_01 = -1;
	private static int energy_02 = -1;
	private static int energy_00 = -1;
	private static String applied_method  = "I-INFO";

	private static boolean hasExtendedRecord = false;

	private static String USBDevice = null;
	private static int retries;
	private static SerialReceiver receiver = null;
	private static SerialPort port = null;

	private static int IPC_Port         = -1;
	private static String IPC_Peer = "127.0.0.1";
	private static int IPC_Timeout      = 10000; // 10 seconds
	private static ProcBridge IPC_pb    = null;

	private static String REST_url        = null;

	private static int waitTime           = 60;
	
	private static boolean artificialData  = false;
	private static Random randomGenerator  = null;
	private static int artificialTimestamp = 1;


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

		//System.err.println(IPC_Port);

		if(IPC_Port > 0)
		{
			IPC_pb = new ProcBridge(IPC_Peer, IPC_Port, IPC_Timeout);
			System.err.println("Running IPC with '" + IPC_Peer + ":" + IPC_Port + "' ");
		}

		if(REST_url != null)
		{
			System.err.println("Running REST with '" + REST_url + "' ");
		}

		boolean ParseSuccess = false;
		
		if(artificialData == true)
		{
			randomGenerator = new Random();
			energy_00 = 1000;
			server_id = new OctetString("ARTIFICIAL");
			applied_method = "ARTIFICIAL";
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
				// TODO Auto-generated catch block
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

		if(IPC_Port > 0)
		{
			try {
				JSONObject resp;

				//System.err.println(jsonMeterReading);
				resp = IPC_pb.request("metering", jsonMeterReading);
				System.err.println(resp);


			} 
			catch (ProcBridgeException e) {
				System.err.println(e.getMessage());
			}
		}

		if(REST_url != null)
		{
			try
			{
				//System.err.println("Send to REST (" + REST_url + "): " + jsonMeterReading);
				SendToRESTUrl(jsonMeterReading);
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}

		}

		if(IPC_Port <= 0 && REST_url == null)
		{
			System.out.println(jsonMeterReading);
		}
		
		return;
	}

	private static void SendToRESTUrl(String jsonMeterReading) throws ClientProtocolException, IOException
	{
		HttpResponse response = Request.Post(REST_url)
				.useExpectContinue()
				.version(HttpVersion.HTTP_1_1)
				.bodyString(jsonMeterReading, ContentType.DEFAULT_TEXT)
				.execute().returnResponse();

		System.err.println(response.getStatusLine());
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
		energy_02 = -1;

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
						int unit = entry.getUnit().getVal();
						SmlValue sml_value = entry.getValue();
						// System.err.println("Unit: " + entry.getUnit().getVal());
						// System.err.println("Unit: " + entry.getObjName().toString());
						ASNObject obj = sml_value.getChoice();
						// System.err.println("Type = " + obj.getClass().getName());
						int value = 0;
						if (obj.getClass().equals(Integer8.class)) {
							value = ((Integer8) obj).getVal();
						}
						else if (obj.getClass().equals(Integer16.class)) {
							value = ((Integer16) obj).getVal();
						}
						else if (obj.getClass().equals(Integer32.class)) {
							value = ((Integer32) obj).getVal();
						}
						else if (obj.getClass().equals(Integer64.class)) {
							//							value = ((Integer64) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned8.class)) {
							value = ((Unsigned8) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned16.class)) {
							value = ((Unsigned16) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned32.class)) {
							value = ((Unsigned32) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned64.class)) {
							//							value = ((Unsigned64) obj).getVal();
						}
						else if (obj.getClass().equals(OctetString.class)) {
							//							OctetString strVal = (OctetString) obj;
						}
						else {
							//							System.err.println(obj.getClass().getName());
						}
						if(entry.getUnit().getVal() == 30)
						{
							//System.err.println(entry.getObjName().toString());
							if(entry.getObjName().toString().equals("01 00 01 08 00 FF"))
							{
								//System.err.println("Unit: " + entry.getUnit().getVal());
								energy_01 = value;
							}
							else if(entry.getObjName().toString().equals("01 00 02 08 00 FF"))
							{
								//System.err.println("Unit: " + entry.getUnit().getVal());
								energy_02 = value;
							}
							else
							{
								//System.err.println("Unit: " + entry.getUnit().getVal());
								energy_00 = value;
							}
						}
						else
						{
							//System.err.println("OBIS: " + entry.getObjName().toString());
							power = value;
							hasExtendedRecord = true;
						}
					}
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
		parser.accepts( "ipc" ).withRequiredArg().ofType( String.class );
		parser.accepts( "wait" ).withRequiredArg().ofType( Integer.class );
		parser.accepts( "rest" ).withRequiredArg().ofType( String.class );
		
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
			REST_url = (String) options.valueOf("rest");
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

		return result;
	}

	private static String writeAsJsonToString()
	{
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Instant instant = timestamp.toInstant();

		float value_00 = -1;
		float value_01 = -1;
		float value_02 = -1;

		if(hasExtendedRecord)
		{
			if(energy_00 > -1)
			{
				value_00 =  (((float)energy_00)/10000);
			}
			if(energy_01 > -1)
			{
				value_01 =  (((float)energy_01)/10000);
			}
			if(energy_02 > -1)
			{
				value_02 =  (((float)energy_02)/10000);
			}

			if(value_00 == -1 && value_01 > 0)
			{
				value_00 = value_01;
				value_01 = -1;
			}
		}
		else
		{
			value_00 =  (((float)energy_01)/10000);
		}

		String meterReading = String.format(
				Locale.ROOT,
				"{applied_method: '%s', server_id: '%s', reading_date: '%s', timestamp: %d, value: %.4f, value_01: %.4f, value_02: %.4f, power: %d}",
				applied_method  ,
				server_id.toHexString(),
				instant.toString(),
				artificialData? artificialTimestamp : ((Unsigned32)actSensorTime.getChoice()).getVal(),
				value_00,
				value_01,
				value_02,
				power
				);

		return meterReading;

	}
}




