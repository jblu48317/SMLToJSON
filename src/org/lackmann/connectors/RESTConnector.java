package org.lackmann.connectors;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RESTConnector
{

	private String rest_url;

	private int timeout;

	final static Logger LOGGER = LogManager.getLogger(RESTConnector.class.getName());

	public RESTConnector(String rest_url, int timeout)
	{
		this.rest_url = rest_url;
	}

	public boolean hasConnection()
	{
		if(rest_url == null)
			return false;
		return true;
	}

	public String getConnection()
	{
		return rest_url;
	}

	public void send(String jsonMeterReading)
	{
		try
		{
			LOGGER.debug("Send to via REST to '" + rest_url + "': " + jsonMeterReading);

			HttpResponse response = Request.Post(rest_url)
					.useExpectContinue()
					.version(HttpVersion.HTTP_1_1)
					.bodyString(jsonMeterReading, ContentType.DEFAULT_TEXT)
					.connectTimeout(timeout)
					.execute().returnResponse();

			LOGGER.debug(response.getStatusLine());
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}


	}

	public boolean open()
	{
		return true;
	}

	public void close()
	{
		
	}

}
