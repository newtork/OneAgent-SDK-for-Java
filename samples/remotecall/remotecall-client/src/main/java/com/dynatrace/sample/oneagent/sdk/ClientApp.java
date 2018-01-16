package com.dynatrace.sample.oneagent.sdk;

/*
 * Copyright 2017 Dynatrace LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;

public class ClientApp {

	private final OneAgentSDK oneAgentSdk;

	private ClientApp() {
		oneAgentSdk = OneAgentSDKFactory.createInstance();
		switch (oneAgentSdk.getCurrentState()) {
		case ACTIVE:
			System.out.println("SDK is active and capturing.");
			break;
		case PERMANENTLY_INACTIVE:
			System.err.println(
					"SDK is PERMANENTLY_INACTIVE; Probably no agent injected or agent is incompatible with SDK.");
			break;
		case TEMPORARILY_INACTIVE:
			System.err.println("SDK is TEMPORARILY_INACTIVE; Agent has been deactived - check agent configuration.");
			break;
		default:
			System.err.println("SDK is in unknown state.");
			break;
		}
	}

	public static void main(String args[]) {
		System.out.println("*************************************************************");
		System.out.println("**       Running remote call client                        **");
		System.out.println("*************************************************************");
		int port = 33744;
		for (String arg : args) {
			if (arg.startsWith("port=")) {
				port = Integer.parseInt(arg.substring("port=".length()));
			} else {
				System.err.println("unknown argument: " + arg);
			}
		}
		try {
			new ClientApp().run(port);
			System.out.println("remote call client finished. sleeping a while, so agent is able to send data to server ...");
			Thread.sleep(15000); // we have to wait - so agent is able to send data to server.
		} catch (Exception e) {
			System.err.println("remote call client failed: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void run(int port) throws IOException, ClassNotFoundException {
		System.out.println("clients connecting to server on port " + port);
		Socket socket = new Socket((String) null, port);
		try {
			System.out.println("Connected to " + socket.getInetAddress().getHostName() + ":" + socket.getPort()
					+ " connected");
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			traceCallToServer(out, port);

		} finally {
			socket.close();
		}
	}

	private void traceCallToServer(ObjectOutputStream out, int port) throws IOException {
		OutgoingRemoteCallTracer externalOutgoingRemoteCall = oneAgentSdk.traceOutgoingRemoteCall("myMethod", "myService", "endpoint", ChannelType.IN_PROCESS, "localhost:" + port);
		externalOutgoingRemoteCall.start();
		try {
			String outgoingTag = externalOutgoingRemoteCall.getDynatraceStringTag();
			System.out.println("send tag to server: " + outgoingTag);
			invokeCallToServer(out, outgoingTag);
		} catch (Exception e) {
			externalOutgoingRemoteCall.error(e);
		} finally {
			externalOutgoingRemoteCall.end();
		}
	}

	private void invokeCallToServer(ObjectOutputStream out, String outgoingTag) throws IOException {
		// call the server and send the tag
		out.writeObject(outgoingTag);
	}
	
}