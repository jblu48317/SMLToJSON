package org.lackmann.connectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openmuc.jsml.transport.SerialReceiver;

import co.gongzh.procbridge.ProcBridge;
import co.gongzh.procbridge.ProcBridgeException;

public class IPCConnector
{
	private static ProcBridge   IPC_pb = null;
	private String ipc_Peer;
	private int ipc_Port;
	private int timeout;
	
	final static Logger LOGGER = LogManager.getLogger(IPCConnector.class.getName());


	public IPCConnector(String ipc_Peer, int ipc_Port, int timeout)
	{
		this.ipc_Peer = ipc_Peer;
		this.ipc_Port = ipc_Port;
		this.timeout  = timeout;
}

	public boolean hasConnection()
	{
		if(IPC_pb == null)
			return false;
		return true;
	}

	public ProcBridge getConnection()
	{
		return IPC_pb;	
	}

	public JSONObject send(String content)
	{
		JSONObject resp = null;

		try {

			LOGGER.debug("Send via IPC: " + content);
			resp = IPC_pb.request("metering", content);
			LOGGER.debug("Response from IPC: " + resp);


		} 
		catch (ProcBridgeException e) {
			LOGGER.debug(e.getMessage());
		}
		
		return resp;
		
	}

	public boolean open()
	{
		IPC_pb = new ProcBridge(ipc_Peer, ipc_Port, timeout);
		if(IPC_pb == null)
		{
			return false;
		}
		return true;
	}

	public void close()
	{
		return;		
	}

}
