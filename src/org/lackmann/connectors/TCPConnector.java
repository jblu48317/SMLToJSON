package org.lackmann.connectors;

import java.net.InetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmuc.jsml.transport.SerialReceiver;
import org.openmuc.jsml.transport.TConnection;
import org.openmuc.jsml.transport.TSAP;

public class TCPConnector
{
	private TConnection TCP_Connection = null;

	private String tcp_Address;

	private int tcp_Port;

	final static Logger LOGGER = LogManager.getLogger(TCPConnector.class.getName());

	public TCPConnector(String tcp_Address, int tcp_Port)
	{
		this.tcp_Address = tcp_Address;	
		this.tcp_Port = tcp_Port;	
	}

	public boolean open()
	{
		boolean status = false;
		int openRetries = 3;
		int openRetryWaitTime = 1;

		for(int r=0; r < openRetries; r++)
		{

			try
			{
				TSAP sml_tSAP = new TSAP();
				TCP_Connection = sml_tSAP.connectTo(InetAddress.getByName(tcp_Address), tcp_Port, 0);
				status = true;
				break;
			}
			catch(java.io.IOException ex)
			{
				LOGGER.error("Unable to open address '" + tcp_Address + ":" + tcp_Port + "'");
			}
			TCP_Connection = null;

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

	public boolean hasConnection()
	{
		if(TCP_Connection == null)
			return false;
		return true;
	}

	public TConnection getConnection()
	{
		return TCP_Connection;
	}

	public void close()
	{
		if(TCP_Connection != null)
			TCP_Connection.close();
		TCP_Connection = null;
	}

}
