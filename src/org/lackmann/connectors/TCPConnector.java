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
	private int retries;
	private int wait;

	final static Logger LOGGER = LogManager.getLogger(TCPConnector.class.getName());

	public TCPConnector(String tcp_Address, int tcp_Port, int retries, int wait)
	{
		this.tcp_Address = tcp_Address;	
		this.tcp_Port = tcp_Port;	
		this.retries = retries;
		this.wait = wait;
	}

	public boolean open()
	{
		boolean status = false;

		for(int r=0; r < retries; r++)
		{

			try
			{
				TSAP sml_tSAP = new TSAP();
				sml_tSAP.setMessageTimeout(1000);
				sml_tSAP.setMessageFragmentTimeout(1000);
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
				java.util.concurrent.TimeUnit.SECONDS.sleep(wait);
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
	
	public int getTimeout()
	{
		return -1;
	}

}
