package org.lackmann.mme.tools;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.openmuc.jsml.structures.ASNObject;
import org.openmuc.jsml.structures.EMessageBody;
import org.openmuc.jsml.structures.Integer16;
import org.openmuc.jsml.structures.Integer32;
import org.openmuc.jsml.structures.Integer64;
import org.openmuc.jsml.structures.Integer8;
import org.openmuc.jsml.structures.OctetString;
import org.openmuc.jsml.structures.SmlFile;
import org.openmuc.jsml.structures.SmlList;
import org.openmuc.jsml.structures.SmlListEntry;
import org.openmuc.jsml.structures.SmlMessage;
import org.openmuc.jsml.structures.SmlTime;
import org.openmuc.jsml.structures.SmlValue;
import org.openmuc.jsml.structures.Unsigned16;
import org.openmuc.jsml.structures.Unsigned32;
import org.openmuc.jsml.structures.Unsigned64;
import org.openmuc.jsml.structures.Unsigned8;
import org.openmuc.jsml.structures.requests.SmlPublicOpenReq;
import org.openmuc.jsml.structures.responses.SmlGetListRes;
import org.openmuc.jsml.transport.TConnection;
import org.openmuc.jsml.transport.TSAP;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class SmlToJSONTCP {

	private static String address;
	private static int port;
	private static int retries;
	private static TConnection sml_tConnection;

	public static void main(String[] args) throws IOException, PortInUseException, UnsupportedCommOperationException 
	{

		if(!processCommandLine(args))
		{
			System.exit(1);
		}
		

		try
		{
      TSAP sml_tSAP = new TSAP();
      sml_tConnection = sml_tSAP.connectTo(InetAddress.getByName(address), port, 0);

		}
		catch(java.io.IOException ex)
		{
			System.err.println("Adresse '" + address + ":" + port + "' konnte nicht geöffnet werden");
			System.err.println("Fehler: \"" + ex.getLocalizedMessage() +"\"");
			System.exit(1);
		} 

		SmlFileWorker smlWorker = new SmlFileWorker();

		for(int r=0; r < retries; r++)
		{
			try
			{
				smlWorker.setSmlFile(sml_tConnection.getSMLFile());
				System.out.println(smlWorker.getSMLAsJson());

				break;
			}
			catch(java.io.IOException ex)
			{
				System.err.println("Fehler beim Lesen von Adresse '" + address + ":" + port + "'");
				System.err.println("Details: \"" + ex.getLocalizedMessage() +"\"");
				continue;
			}
		}
		
		sml_tConnection.close();

	}

	private static boolean processCommandLine(String[] args)
	{
		OptionParser parser = new OptionParser();
		parser.accepts( "retries" ).withRequiredArg().ofType( Integer.class );
		parser.accepts( "address" ).withRequiredArg().ofType( String.class );
		parser.accepts( "port" ).withRequiredArg().ofType( Integer.class );
		OptionSet options = parser.parse(args);

		boolean result = true;

		if (options.has("retries") && options.hasArgument("retries"))
		{
			retries = (int) options.valueOf("retries");
		}

		if (options.has("address") && options.hasArgument("address"))
		{
			address = (String) options.valueOf("address");
		}

		if (options.has("port") && options.hasArgument("port"))
		{
			port = (int) options.valueOf("port");
		}

		if (address == null)
		{
			System.out.println("\nKeine TCP/IP-Adresse  - Option '-address' nicht gesetzt!\n");
			result = false;
		}
		if (port <= 0)
		{
			System.out.println("\nKein TCP/IP-Port  - Option '-port' nicht gesetzt!\n");
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
