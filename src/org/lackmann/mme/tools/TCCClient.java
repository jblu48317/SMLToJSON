package org.lackmann.mme.tools;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class TCCClient
{
	private static String TCC_url  = null;
	private static String deviceID = null;
	private static String serialID  = null;
	private static String user  = null;
	private static String password  = null;
	
	final static Logger LOGGER = LogManager.getLogger(TCCClient.class.getName());


	TCCClient(String p_TCC_url, String p_deviceID, String p_serialID, String l_user, String l_password)
	{
		TCC_url  = p_TCC_url;
		deviceID = p_deviceID;
		serialID = p_serialID;
		user     = l_user;
		password = l_password;
		
		if(deviceID == null)
		{
			try
			{
				initDeviceID();
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	private void initDeviceID() throws ClientProtocolException, IOException, URISyntaxException
	{
		LOGGER.debug("Register to TCC Server");
		//		HttpResponse response = Request.Post(TCC_url)
		//				.useExpectContinue()
		//				.version(HttpVersion.HTTP_1_1)
		//				.bodyString(jsonMeterReading, ContentType.DEFAULT_TEXT)
		//				.execute().returnResponse();

		//	HttpResponse response = Request.Get(uri)
		//	.useExpectContinue()
		//	.version(HttpVersion.HTTP_1_1)
		//	.execute().returnResponse();



		if(deviceID  == null)
		{
			//			final String JSON_DATA= "{\"request\":\"REGISTER\",\"deviceID\":\"cabe4847-a821-4289-8b21-f96f8915f9ec\"}\n"; 
			//			long len = JSON_DATA.length();

			URI uri = new URIBuilder(TCC_url   + "/register")
					.setParameter("serial", serialID)
//					.setParameter("user", user)
//					.setParameter("password", password)
					.build();

			CloseableHttpClient httpclient = HttpClients.createDefault();
			
			LOGGER.debug("URI to be used for register request:");
			LOGGER.debug(uri);
			
			HttpGet httpget = new HttpGet(uri);

			CloseableHttpResponse response = httpclient.execute(httpget);

			LOGGER.debug(response.getStatusLine());
			try {
				HttpEntity entity = response.getEntity();

				if (entity != null) {
					long len = entity.getContentLength();
					if (len != -1 && len < 2048) {
						//					System.out.println(EntityUtils.toString(entity));

						final JSONObject obj = new JSONObject(EntityUtils.toString(entity));
						deviceID = obj.getString("deviceID");

					} else {
						// Stream content out	
					}
				}
			} finally {
				response.close();
			}
		}
	}

	public void openChannel() throws ClientProtocolException, IOException, URISyntaxException
	{
		URI uri = new URIBuilder(TCC_url   + "/openchannel")
				.setParameter("serial", serialID)
				.setParameter("deviceID", deviceID)
				.setParameter("user", user)
				.setParameter("password", password)
				.build();

		System.err.println(uri);
		
		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(uri);

		HttpResponse response = httpclient.execute(httpget);
    EntityUtils.consume(response.getEntity());
    
		System.err.println("Status = '" + response.getStatusLine() + "'");
		try {
			HttpEntity entity = response.getEntity();
			System.err.println("Content = '" + EntityUtils.toString(entity) + "'");

		} 
		catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

	public String getDeviceID()
	{
		// TODO Auto-generated method stub
		return deviceID;
	}
}
