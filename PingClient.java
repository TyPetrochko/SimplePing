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
	static final int TIMEOUT = 1000;
	static final int NUM_PINGS = 10;

	private static InetAddress host;
	private static int port;
	private static String passwd;
	
	// Track average, max, min RTT
	public static long totalRTT = 0;
	public static long minRTT = Long.MAX_VALUE;
	public static long maxRTT = Long.MIN_VALUE;
	public static int numRTTs = 0;

	// Track current "ping"
	public static int counter = 0;

	public static void main (String [] args){
		// Check correct params
		if (args.length != 3){
			System.out.println("Usage: java PingClient host port passwd");
			return;
		}
		
		// Initialize command line args
		try{
			host = InetAddress.getByName(args[0]);
			port = Integer.parseInt(args[1]);
			passwd = args[2];
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		try{
			// Set up I/O socket
			final DatagramSocket socket = new DatagramSocket(0);
			socket.setSoTimeout(TIMEOUT); // maybe set this lower?
			
			// Schedule a new timer-task
			final Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run() {
					// Start at 1, not 0
					PingClient.counter++;
					// If task has run enough time, print results
					if(counter > NUM_PINGS){

						// Calculate average round-trip time and loss rate
						double avgRTT = (double) totalRTT / (double) numRTTs;
						double lossRate = ((double) (NUM_PINGS - numRTTs) / 
							(double) NUM_PINGS);

						// Print results
						System.out.println("Average RTT: " + avgRTT);
						System.out.println("Max RTT: " + maxRTT);
						System.out.println("Min RTT: " + minRTT);
						System.out.println("Loss rate: " + lossRate);

						// Cancel the timer and return from run
						t.cancel();
						return;
					}
					
					// Build a message to send
					byte [] data = null;
					try{
						// Calculate length of terminator plus CRLF signal
						byte [] terminator = (passwd + "\r\n").getBytes("US-ASCII");
						ByteBuffer messageBuilder = ByteBuffer
							.allocate(14 + terminator.length);

						// Four bytes "ping"
						String PINGstr = "PING";
						byte[] PING = PINGstr.getBytes("US-ASCII");
						messageBuilder.put(PING);

						// Two byte sequence number
						short sequenceNumber = (short) counter;
						messageBuilder.putShort(sequenceNumber);
						
						// Eight bytes time
						long timestamp = System.currentTimeMillis();
						messageBuilder.putLong(timestamp);

						// Then some number of bytes for password and CRLF
						messageBuilder.put(terminator);
						data = messageBuilder.array();
					}catch (Exception e){
						e.printStackTrace();
					}

					// Make a datagram request
					DatagramPacket request = new DatagramPacket(data, 
							data.length, host, port);

					// Make a response datagram
					byte [] receiptData = new byte[1024];
					DatagramPacket response = new DatagramPacket(receiptData, 
							receiptData.length);

					// Send request
					try{
						socket.send(request);
					} catch(Exception e){
						e.printStackTrace();
					}

					// Start waiting for a response
					long startTime = System.currentTimeMillis();
					while(true){
						try{
							socket.receive(response);
						}catch (Exception e){
							System.out.println("Socket timed out");
							return;	// Request timed out
						}
						
						// Has too much time elapsed?
						long currentTime = System.currentTimeMillis();
						if(currentTime - startTime > TIMEOUT){
							System.out.println("Too much time elapsed: " + (currentTime - startTime));
							return; // Request timed out
						}

						// Now interpret data
						ByteBuffer decoder = ByteBuffer.wrap(response.getData());

						// Skip eight-byte PINGECHO
						decoder.get(new byte[8]);

						// Get sequence and send time
						short sequence = decoder.getShort();
						long sendTime = decoder.getLong();

						// If it's not the right data, keep waiting
						if(counter != sequence){
							System.out.println("Received " + sequence + " while waiting for " + counter);
							continue;
						}

						// Calculate RTT
						long RTT = currentTime - sendTime;

						// Update RTT info
						PingClient.numRTTs++;
						PingClient.totalRTT += RTT;
						PingClient.minRTT = Math.min(minRTT, RTT);
						PingClient.maxRTT = Math.max(maxRTT, RTT);

						// Debug (print message)
						String toPrint = new String(response.getData());
						System.out.println("RTT for ping " + counter + ": " + RTT);
						return;
					}
				}
			}, 0, TIMEOUT);

		}catch(Exception e){
			e.printStackTrace();
		}
	}// end main
} // end class
