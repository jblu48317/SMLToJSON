package org.lackmann.connectors;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmuc.jsml.transport.SerialReceiver;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;


public class USBConnector
{

	private String usb_Device           = null;
	private SerialPort USB_Port         = null;
	private SerialReceiver USB_Receiver = null;
	private int retries;
	private int wait;

	final static Logger LOGGER = LogManager.getLogger(USBConnector.class.getName());

	public USBConnector(String usb_Device, int retries, int wait)
	{
		this.usb_Device = usb_Device;
		this.retries = retries;
		this.wait = wait;
		
	}

	public boolean open()
	{
		boolean status = false;
		
		File usb = new File(usb_Device);
		if(!usb.exists())
		{
			LOGGER.error("Device '" + usb_Device + "' not existing");
			return false;
		}

		for(int r=0; r < retries; r++)
		{

			try
			{
				USB_Port = (SerialPort) CommPortIdentifier.getPortIdentifier(usb_Device).open("SML_USB",500);
				try
				{
					USB_Port.enableReceiveTimeout(1000);
				} catch (UnsupportedCommOperationException ex)
				{
					LOGGER.error("Unable to set timeout for device '" + usb_Device + "'");
					LOGGER.error("Details: '" + ex.getLocalizedMessage() +"'");
				}
				USB_Receiver = new SerialReceiver(USB_Port);
				status = true;
				break;
			}
			catch(java.io.IOException ex)
			{
				LOGGER.error("Unable to open device '" + usb_Device + "'");
				LOGGER.error("Details: '" + ex.getLocalizedMessage() +"'");
			} 
			catch (NoSuchPortException ex)
			{
				LOGGER.error("Device '" + usb_Device + "' not existing");
				LOGGER.error("Details: '" + ex.getLocalizedMessage() +"'");
			}
			catch(PortInUseException ex)
			{
				LOGGER.error("Device '" + usb_Device + "' already in use");
				LOGGER.error("Details: '" + ex.getLocalizedMessage() +"'");
			}
			USB_Port = null;
			USB_Receiver = null;

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
		if(USB_Receiver == null)
			return false;
		return true;
	}

	public SerialReceiver getConnection()
	{
		return USB_Receiver;
	}

	public void close()
	{
		if(USB_Port != null)
		{
			USB_Port.close();
			USB_Port = null;
		}

		if(USB_Receiver != null)
		{
			try
			{
				USB_Receiver.closeStream();
				USB_Receiver = null;
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public int getTimeout()
	{
		if(USB_Port != null)
			return USB_Port.getReceiveTimeout();
		return -1;
	}
}
