// PingClient.java
// Author: Tyler Petrochko
// Class: CS 433 with Yang R. Yang
//
//
// Spec:
//
// 		The output of your client should report the minimum, 
// 		maximum, and average RTTs. It should also report the loss rate.
//
// Usage:
//
// 		java PingClient host port passwd
//

import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;


public class PingClient {
	static final int NUM_PINGS = 10;
	
	private static InetAddress host;
	private static int port;
	private static String passwd;

	public static void main (String [] args){
		if (args.length != 3){
			System.out.println("Usage: java PingClient host port passwd");
			return;
		}
		
		try{
			// Initialize command line args
			host = InetAddress.getByName(args[0]);
			port = Integer.parseInt(args[1]);
			passwd = args[2];

			// Set up I/O socket
			DatagramSocket socket = new DatagramSocket(0);
			socket.setSoTimeout(1000); // maybe set this lower?

			// Let's make some ping messages
			byte [][] messages = new byte[NUM_PINGS][];
			byte [] terminator = (passwd + "\r\n").getBytes("US-ASCII");
			for (int i = 0; i < NUM_PINGS; i++){
				try{
					ByteBuffer messageBuilder = ByteBuffer
						.allocate(14 + terminator.length);

					// first four bytes "ping"
					String PINGstr = "PING";
					byte[] PING = PINGstr.getBytes("US-ASCII");
					messageBuilder.put(PING);

					// then two byte sequence number
					short sequenceNumber = (short) i;
					messageBuilder.putShort(sequenceNumber);

					
					// then eight bytes time
					long timestamp = System.currentTimeMillis();
					messageBuilder.putLong(timestamp);

					// then some number of bytes for password and CRLF
					messageBuilder.put(terminator);
					
					// explicitly store this message as byte array
					messages[i] = messageBuilder.array();
				}catch (Exception e){
					e.printStackTrace();
				}
			}
			
			// Track average, max, min RTT
			long totalRTT = 0;
			long minRTT = Long.MAX_VALUE;
			long maxRTT = Long.MIN_VALUE;
			int numRTTs = 0;
			
			// Send Ping messages
			for(int i = 0; i < NUM_PINGS; i++){
				// Ping request
				DatagramPacket request = new DatagramPacket(messages[i], 
						messages[i].length, host, port);

				// Ping response
				byte [] receiptData = new byte[1024];
				DatagramPacket response = new DatagramPacket(receiptData, 
						receiptData.length);

				// Send and receive
				try{
					socket.send(request);
					socket.receive(response);
				}catch (Exception e){
					continue;	// Request timed out
				}

				// Now interpret data
				ByteBuffer decoder = ByteBuffer.wrap(response.getData());

				// Skip eight-byte PINGECHO
				decoder.get(new byte[8]);

				// Get sequence and send time
				short sequence = decoder.getShort();
				long sendTime = decoder.getLong();

				// TODO we may have to validate sequence, or do something with pass
				long RTT = System.currentTimeMillis() - sendTime;

				// Update RTT info
				numRTTs++;
				totalRTT += RTT;
				minRTT = Math.min(minRTT, RTT);
				maxRTT = Math.max(maxRTT, RTT);

				// Debug (print message)
				String toPrint = new String(response.getData());
				System.out.println("RTT: " + RTT);
			}

			double avgRTT = (double) totalRTT / (double) numRTTs;
			double lossRate = (double) (NUM_PINGS - numRTTs) / (double) NUM_PINGS;

			System.out.println("Average RTT: " + avgRTT);
			System.out.println("Max RTT: " + maxRTT);
			System.out.println("Min RTT: " + minRTT);
			System.out.println("Loss rate: " + lossRate);
		}catch(Exception e){
			e.printStackTrace();
		}
	}// end main
} // end class