package org.lackmann.mme.tools;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.lackmann.connectors.db.MeterDataRecord;
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

public class SmlFileWorker
{
	SmlFile smlFile;

	OctetString client_id;
	OctetString server_id;

	private SmlTime actSensorTime = null;
	private long power = -1;
	private long energy_180 = -1;
	private long energy_181 = -1;
	private long energy_182 = -1;
	private long energy_280 = -1;

	boolean hasExtendedRecord = false;

	public SmlFileWorker(SmlFile tSmlFile) throws java.io.IOException
	{
		parseSML(tSmlFile);
	}

	public SmlFileWorker()
	{
		// TODO Auto-generated constructor stub
	}

	public void setSmlFile(SmlFile tSmlFile) throws java.io.IOException
	{
		parseSML(tSmlFile);		
	}

	public String getSMLAsJson()
	{
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Instant instant = timestamp.toInstant();

		StringBuilder jsonReturn = new StringBuilder("{");
		jsonReturn.append("\"appliedMethod\": \"I-INFO\", ");
		jsonReturn.append("\"timestamp\": \"" + instant + "\", ");

		if(hasExtendedRecord)
		{
			if(energy_180 > -1)
			{
				jsonReturn.append("\"v_1_8_0\": \"" +  (((float)energy_180)/10000) + "\",");
			}
			if(energy_181 > -1)
			{
				jsonReturn.append("\"v_1_8_1\": \"" +  (((float)energy_181)/10000) + "\",");
			}
			if(energy_182 > -1)
			{
				jsonReturn.append("\"v_1_8_2\": \"" +  (((float)energy_182)/10000) + "\",");
			}
			if(energy_280 > -1)
			{
				jsonReturn.append("\"v_2_8_0\": \"" +  (((float)energy_280)/10000) + "\",");
			}
			if(power > -1)
			{
				jsonReturn.append("\"power\": \"" +  power + "\",");
			}
		}
		else
		{
			if(energy_180 > -1)
			{
				jsonReturn.append("\"v_1_8_0\": \"" + new BigDecimal(((float)energy_180)).toPlainString() + "\",");
			}
			if(energy_181 > -1)
			{
				jsonReturn.append("\"v_1_8_1\": \"" + new BigDecimal(((float)energy_181)).toPlainString() + "\",");
			}
			if(energy_182 > -1)
			{
				jsonReturn.append("\"v_1_8_2\": \"" + new BigDecimal(((float)energy_182)).toPlainString() + "\",");
			}
			if(energy_280 > -1)
			{
				jsonReturn.append("\"v_2_8_0\": \"" + new BigDecimal(((float)energy_280)).toPlainString() + "\",");
			}
		}


		jsonReturn.append("\"serverID\": \"" + server_id.toHexString() + "\",");
		jsonReturn.append("\"secindex\": \"" + ((Unsigned32)actSensorTime.getChoice()).getVal() + "\"");
		jsonReturn.append("}");

		return jsonReturn.toString();
	}

	public String getForIPC()
	{
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		Instant instant = timestamp.toInstant();

		return String.format(
				Locale.ROOT,
				"{'appliedMethod': '%s', 'serverID': '%s', 'timestamp': '%s', 'secindex': %d, 'value': %.4f, 'value_01': %.4f, 'value_02': %.4f, 'power': %d}",
				"I-INFO"  ,
				server_id.toHexString(),
				instant.toString(),
				((Unsigned32)actSensorTime.getChoice()).getVal(),
				((float)energy_180)/10000,
				((float)energy_181)/10000,
				((float)energy_182)/10000,
				power
				);	
	}


	private void parseSML(SmlFile tSmlFile) throws java.io.IOException
	{  
		smlFile = tSmlFile;

		List<SmlMessage> smlMessages = smlFile.getMessages();

		for (int i = 0; i < smlMessages.size(); i++) {
			SmlMessage sml_message = smlMessages.get(i);

			EMessageBody tag = EMessageBody.toEnum(sml_message.getMessageBody().getTag().getVal());
			switch (tag) {
			case OPEN_REQUEST:
				SmlPublicOpenReq sml_PublicOpenReq = (SmlPublicOpenReq) sml_message.getMessageBody().getChoice();
				client_id = sml_PublicOpenReq.getClientId();
				break;
			case OPEN_RESPONSE:
				//				System.out.println("Got OpenResponse");
				break;
			case CLOSE_REQUEST:
				// System.out.println("Got CloseRequest");
				break;
			case CLOSE_RESPONSE:
				//				System.out.println("Got CloseResponse");
				break;
			case GET_PROFILE_PACK_REQUEST:
				// System.out.println("Got GetProfilePackRequest");
				break;
			case GET_PROFILE_PACK_RESPONSE:
				// System.out.println("Got GetProfilePackResponse");
				break;
			case GET_PROFILE_LIST_REQUEST:
				// System.out.println("Got GetProfileListRequest");
				break;
			case GET_PROFILE_LIST_RESPONSE:
				// System.out.println("Got GetProfileListResponse");
				break;
			case GET_PROC_PARAMETER_REQUEST:
				// System.out.println("Got GetProcParameterRequest");
				break;
			case GET_PROC_PARAMETER_RESPONSE:
				// System.out.println("Got GetProcParameterResponse");
				break;
			case SET_PROC_PARAMETER_REQUEST:
				System.out.println("Got SetProcParameterRequest");
				break;
			case SET_PROC_PARAMETER_RESPONSE:
				// System.out.println("Got SetProcParameterResponse");
				break;
			case GET_LIST_REQUEST:
				// System.out.println("Got GetListRequest");
				break;
			case GET_LIST_RESPONSE:

				SmlGetListRes resp = (SmlGetListRes) sml_message.getMessageBody().getChoice();
				server_id = resp.getServerId();
				actSensorTime = resp.getActSensorTime();

				SmlList smlList = resp.getValList();
				SmlListEntry[] list = smlList.getValListEntry();

				for (SmlListEntry entry : list) {
					// System.out.println("Unit: " + entry.getUnit().getVal());
					if(entry.getUnit().getVal() == 30 || entry.getUnit().getVal() == 27)
					{
						int unit = entry.getUnit().getVal();
						SmlValue sml_value = entry.getValue();
						// System.out.println("Unit: " + entry.getUnit().getVal());
						// System.out.println("Unit: " + entry.getObjName().toString());
						ASNObject obj = sml_value.getChoice();
						// System.out.println("Type = " + obj.getClass().getName());
						long value = 0;
						if (obj.getClass().equals(Integer8.class)) {
							value = ((Integer8) obj).getVal();
						}
						else if (obj.getClass().equals(Integer16.class)) {
							value = ((Integer16) obj).getVal();
						}
						else if (obj.getClass().equals(Integer32.class)) {
							value = ((Integer32) obj).getVal();
						}
						else if (obj.getClass().equals(Integer64.class)) {
							value = ((Integer64) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned8.class)) {
							value = ((Unsigned8) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned16.class)) {
							value = ((Unsigned16) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned32.class)) {
							value = ((Unsigned32) obj).getVal();
						}
						else if (obj.getClass().equals(Unsigned64.class)) {
							value = ((Unsigned64) obj).getVal();
						}
						else if (obj.getClass().equals(OctetString.class)) {
							//							OctetString strVal = (OctetString) obj;
						}
						else {
							//							System.out.println(obj.getClass().getName());
						}
						if(entry.getUnit().getVal() == 30)
						{
							// System.out.println(entry.getObjName().toString());
							if(entry.getObjName().toString().equals("01 00 01 08 00 FF"))
							{
								// System.out.println("Unit: " + entry.getUnit().getVal());
								energy_180 = value;
							}
							else if(entry.getObjName().toString().equals("01 00 01 08 01 FF"))
							{
								// System.out.println("Unit: " + entry.getUnit().getVal());
								energy_181 = value;
							}
							else if(entry.getObjName().toString().equals("01 00 01 08 02 FF"))
							{
								// System.out.println("Unit: " + entry.getUnit().getVal());
								energy_182 = value;
							}
							else if(entry.getObjName().toString().equals("01 00 02 08 00 FF"))
							{
								// System.out.println("Unit: " + entry.getUnit().getVal());
								energy_280 = value;
							}
							//							else
							//							{
							//								//System.out.println("Unit: " + entry.getUnit().getVal());
							//								energy_00 = value;
							//							}
						}
						else
						{
							power = value;
							hasExtendedRecord = true;
						}
					}
				}
				break;
			case ATTENTION_RESPONSE:
				System.err.println("Got AttentionResponse");
				break;
			default:
				System.err.println("type not found");
			}
		}
	}

	public SmlTime getActSensorTime()
	{
		return actSensorTime;
	}

	public float getEnergy_180()
	{
		if(energy_180 > -1)
		{
			if(hasExtendedRecord)
				return (((float)energy_180)/10000);
			else
				return energy_180;
		}
		return -1;
	}

	public float getEnergy_181()
	{
		if(energy_181 > -1)
		{
			if(hasExtendedRecord)
				return (((float)energy_181)/10000);
			else
				return energy_181;
		}
		return -1;
	}

	public float getEnergy_182()
	{
		if(energy_182 > -1)
		{
			if(hasExtendedRecord)
				return (((float)energy_182)/10000);
			else
				return energy_182;
		}
		return -1;
	}

	public float getEnergy_280()
	{
		if(energy_280 > -1)
		{
			if(hasExtendedRecord)
				return (((float)energy_280)/10000);
			else
				return energy_280;
		}
		return -1;
	}

	public long getPower()
	{
		return power;
	}

	public String getServerID()
	{
		if(server_id != null)
			return server_id.toHexString();
		return "";
	}
}
