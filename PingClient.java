import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public class PingClient {
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
			byte [][] messages = new byte[10][];
			byte [] terminator = (passwd + "\r\n").getBytes("US-ASCII");
			for (int i = 0; i < 10; i++){
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
			
			// Send Ping messages
			for(int i = 0; i < 10; i++){
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
					System.out.println("One timed out...");	
				}

				// Debug (print message)
				String toPrint = new String(response.getData());
				System.out.println("Received some data: " + toPrint);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}// end main
} // end class
