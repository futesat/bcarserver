package com.boskicar.bcarserver;

import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.integration.support.MessageBuilder;

public class Test {

	public static void main(String[] args) {
		
		UnicastSendingMessageHandler handler =
			      new UnicastSendingMessageHandler("localhost", 4444);

			String payload = "Hello world";
			handler.handleMessage(MessageBuilder.withPayload(payload).build());
	}

}
