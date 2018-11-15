package org.lackmann.connectors.db;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

@Measurement(name = "MeterData")
public class MeterDataRecord 
{
	@Column(name = "secindex")
	private int secindex;
	
	@Column(name = "appliedMethod")
	private String appliedMethod;
	
	@Column(name = "timestamp")
	private String timestamp;
	
	@Column(name = "v_1_8_0")
	private double v_1_8_0;
	
	@Column(name = "v_1_8_1")
	private double v_1_8_1;
	
	@Column(name = "v_1_8_2")
	private double v_1_8_2;
	
	@Column(name = "v_2_8_0")
	private double v_2_8_0;
	
	@Column(name = "power")
	private int power;
	
	@Column(name = "serverID")
	private String serverID;
	
	@Override
	public String toString()
	{
		return "MeterDataRecord [secindex=" + secindex 
				+ ", appliedMethod=" + appliedMethod 
				+ ", timestamp=" + timestamp 
				+ ", v_1_8_0="   + v_1_8_0 
				+ ", v_1_8_1="   + v_1_8_1 
				+ ", v_1_8_2="   + v_1_8_2 
				+ ", v_2_8_0="   + v_2_8_0 
				+ ", power="     + power 
				+ ", serverID="  + serverID
				+ "]";
	}

	public int getSecindex()
	{
		return secindex;
	}

	public void setSecindex(int secindex)
	{
		this.secindex = secindex;
	}

	public String getAppliedMethod()
	{
		return appliedMethod;
	}

	public void setAppliedMethod(String appliedMethod)
	{
		this.appliedMethod = appliedMethod;
	}

	public String getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(String timestamp)
	{
		this.timestamp = timestamp;
	}

	public double getV_1_8_0()
	{
		return v_1_8_0;
	}

	public void setV_1_8_0(double v_1_8_0)
	{
		this.v_1_8_0 = v_1_8_0;
	}

	public double getV_1_8_1()
	{
		return v_1_8_1;
	}

	public void setV_1_8_1(double v_1_8_1)
	{
		this.v_1_8_1 = v_1_8_1;
	}

	public double getV_1_8_2()
	{
		return v_1_8_2;
	}

	public void setV_1_8_2(double v_1_8_2)
	{
		this.v_1_8_2 = v_1_8_2;
	}

	public double getV_2_8_0()
	{
		return v_2_8_0;
	}

	public void setV_2_8_0(double v_2_8_0)
	{
		this.v_2_8_0 = v_2_8_0;
	}

	public int getPower()
	{
		return power;
	}

	public void setPower(int power)
	{
		this.power = power;
	}

	public String getServerID()
	{
		return serverID;
	}

	public void setServerID(String serverID)
	{
		this.serverID = serverID;
	}



}