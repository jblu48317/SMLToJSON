package org.lackmann.mme.tools;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

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

public class SmlToJSON {

	protected static OctetString client_id;
	protected static OctetString server_id;
	private static SmlTime actSensorTime;
	private static int power = -1;
	private static int energy = -1;
	private static int energy_02 = -1;
	private static int energy_00 = -1;

	private static boolean hasExtendedRecord = false;

	private static String device;
	private static int retries;
	private static SerialReceiver receiver;
	private static SerialPort port;


	public static void main(String[] args) throws IOException, PortInUseException, UnsupportedCommOperationException 
	{

		if(!processCommandLine(args))
		{
			System.exit(1);
		}
		

		try
		{
			//		receiver.setupComPort("/dev/ttyUSB0");
			// receiver = new SerialReceiver();
			// receiver.setupComPort(device);
			port = (SerialPort) CommPortIdentifier.getPortIdentifier(device).open("SML_USB",2000);

    	receiver = new SerialReceiver(port);
		}
		catch(java.io.IOException ex)
		{
			System.err.println("Device '" + device + "' konnte nicht geöffnet werden");
			System.err.println("Fehler: \"" + ex.getLocalizedMessage() +"\"");
			System.exit(1);
		} 
		catch (NoSuchPortException ex)
		{
			System.err.println("Device '" + device + "' nicht vorhanden");
			System.err.println("Fehler: \"" + ex.getLocalizedMessage() +"\"");
			System.exit(1);
		}

		SmlFileWorker smlWorker = new SmlFileWorker();

		for(int r=0; r < retries; r++)
		{
			try
			{
				smlWorker.setSmlFile(receiver.getSMLFile());
				System.out.println(smlWorker.getSMLAsJson());
				
				break;
			}
			catch(java.io.IOException ex)
			{
				System.err.println("Fehler beim Lesen von Device '" + device + "'");
				System.err.println("Details: \"" + ex.getLocalizedMessage() +"\"");
				continue;
			}
		}

		port.close();
		receiver.closeStream();



	}

	private static boolean processCommandLine(String[] args)
	{
		OptionParser parser = new OptionParser();
		parser.accepts( "retries" ).withRequiredArg().ofType( Integer.class );
		parser.accepts( "device" ).withRequiredArg().ofType( String.class );
		OptionSet options = parser.parse(args);

		boolean result = true;

		if (options.has("retries") && options.hasArgument("retries"))
		{
			retries = (int) options.valueOf("retries");
		}

		if (options.has("device") && options.hasArgument("device"))
		{
			device = (String) options.valueOf("device");
		}

		if (device == null)
		{
			System.out.println("\nKein USB-Device - Option '-device' nicht gesetzt!\n");
			result = false;
		}
		if (retries <= 0)
		{
			System.out.println("\nKeine Anzahl für Wiederholungen - Option '-retries' nicht gesetzt!\n");
			result = false;
		}

		return result;
	}

}
