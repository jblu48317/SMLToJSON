package org.lackmann.mme.tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MulticastPublisher
{

	private DatagramSocket socket = null;
	private InetAddress group = null;
	private String address;
	private int port;
	

	public MulticastPublisher(String address, int port) throws Exception
	{
		super();
		this.address = address;
		this.port = port;
		this.socket = new DatagramSocket(port);
		this.group = InetAddress.getByName(address);
	}

	public void publish(String jsonMeterReading)
	{
		byte buf [] = jsonMeterReading.getBytes();
		DatagramPacket packet;
		packet = new DatagramPacket(buf, buf.length, this.group, this.port);
    try
		{
			this.socket.send(packet);
		} 
    catch (IOException e)
		{
			System.err.println(e.getMessage());
		}		
	}

	protected void shutdown() throws Throwable
	{
		if(this.socket != null)
			this.socket.close();
	}
	
	
	
}
