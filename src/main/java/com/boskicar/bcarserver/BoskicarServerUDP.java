package com.boskicar.bcarserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service(value="BoskicarServerUDP")
public class BoskicarServerUDP
{
	private static final Logger LOGGER = LoggerFactory.getLogger(BoskicarServerUDP.class);

	public BoskicarServerUDP() {
		super();
	}
	
	@Autowired
	private ObjectMapper objectMapper;
	
	@Autowired
	private BoskicarServerAPI boskicarServerAPI;
	
	private long lasDate;
	
	public synchronized void handleMessage(Message <byte[]>message)
	{
		try 
		{
			String payload = new String(message.getPayload());
			JoystickMoveMessage jmm = objectMapper.readValue(payload, JoystickMoveMessage.class);
			
			if(lasDate < jmm.getDate())
			{
				boskicarServerAPI.joystick(jmm.getAngle(), jmm.getStrength());
				lasDate = jmm.getDate();
			}
			else
			{
				LOGGER.error("handleMessage: Outdated message=[{}]", payload);
			}
		} 
		catch (Exception e) 
		{
			LOGGER.error("handleMessage: Error handling message", e);
		}
	}
}