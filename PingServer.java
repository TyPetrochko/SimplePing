// PingServer.java
import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;

/* 
 * Server to process ping requests over UDP.
 */
		
public class PingServer
{
	 private static double LOSS_RATE = 0.3;
	 private static int AVERAGE_DELAY = 100; // milliseconds

	 public static void main(String[] args) throws Exception
	 {
			// Get command line argument.
			if (args.length < 2) {
				 System.out.println("Usage: java PingServer port passwd" 
						 + " [-delay delay] [-loss loss]");
				 return;
			}

			// Set options
			for(int i = 2; i < args.length; i += 2){
				if(args[i].equals("-delay")){
					AVERAGE_DELAY = Integer.parseInt(args[i + 1]);
				}else if (args[i].equals("-loss")){
					LOSS_RATE = Double.parseDouble(args[i + 1]);
				}
			}

			// Set port/password
			int port = Integer.parseInt(args[0]);
			String passwd = args[1];

			// Create random number generator for use in simulating
			// packet loss and network delay.
			Random random = new Random();

			// Create a datagram socket for receiving and sending
			// UDP packets through the port specified on the
			// command line.
			DatagramSocket socket = new DatagramSocket(port);

			// Processing loop.
			while (true) {

				 // Create a datagram packet to hold incomming UDP packet.
				 DatagramPacket
						request = new DatagramPacket(new byte[1024], 1024);
	
				 // Block until receives a UDP packet.
				 socket.receive(request);
		 
				 // Print the received data, for debugging
				 printData(request);

				 // Decide whether to reply, or simulate packet loss.
				 if (random.nextDouble() < LOSS_RATE) {
						System.out.println("SERVER: Reply not sent.");
						continue;
				 }

				 // Generate response decoder
				 ByteBuffer decoder = ByteBuffer.wrap(request.getData());

				 // Skip four-byte PING message
				 decoder.get(new byte[4]);

				 // Get sequence and send time
				 short sequence = decoder.getShort();
				 long sendTime = decoder.getLong();

				 // Get remaining bytes to password
				 byte [] passwordBuf = new byte[decoder.remaining() - 2];
				 decoder.get(passwordBuf);
				 String password = new String(passwordBuf, "US-ASCII").trim();

				 // Debug credentials
				 if(!passwd.equals(password)){
						continue; // Incorrect password
				 }

				 // Generate return message
				 byte [] data = new byte[1];
				 byte [] terminator = (passwd + "\r\n").getBytes("US-ASCII");
				 try{
						ByteBuffer messageBuilder = ByteBuffer
							 .allocate(18 + terminator.length);

						// Send PingEcho
						String PINGECHOstr = "PINGECHO";
						byte[] PINGECHO = PINGECHOstr.getBytes("US-ASCII");
						messageBuilder.put(PINGECHO);

						// Send two byte sequence number
						messageBuilder.putShort(sequence);

						// Send eight byte time
						long timestamp = System.currentTimeMillis();
						messageBuilder.putLong(sendTime);

						// Then send password and CRLF
						messageBuilder.put(terminator);
						data = messageBuilder.array();
				 }catch(Exception e){
						e.printStackTrace();
				 }

				 // Simulate prorogation delay.
				 Thread.sleep((int) (random.nextDouble() * 2 * AVERAGE_DELAY));

				 // Send reply.
				 InetAddress clientHost = request.getAddress();
				 int clientPort = request.getPort();
				 DatagramPacket reply = new DatagramPacket(data, data.length, 
							 clientHost, clientPort);
		
				 socket.send(reply);
		
				 System.out.println("SERVER: Reply sent.");
		 } // end of while
	 } // end of main

	 /* 
		* Print ping data to the standard output stream.
		*/
	 private static void printData(DatagramPacket request) 
					 throws Exception

	 {
			// Obtain references to the packet's array of bytes.
			byte[] buf = request.getData();

			// Wrap the bytes in a byte array input stream,
			// so that you can read the data as a stream of bytes.
			ByteArrayInputStream bais 
					= new ByteArrayInputStream(buf);

			// Wrap the byte array output stream in an input 
			// stream reader, so you can read the data as a
			// stream of **characters**: reader/writer handles 
			// characters
			InputStreamReader isr 
					= new InputStreamReader(bais);

			// Wrap the input stream reader in a bufferred reader,
			// so you can read the character data a line at a time.
			// (A line is a sequence of chars terminated by any 
			// combination of \r and \n.)
			BufferedReader br 
					= new BufferedReader(isr);

			// The message data is contained in a single line, 
			// so read this line.
			String line = br.readLine();

			// Print host address and data received from it.
			System.out.println("SERVER: Received from " +         
				request.getAddress().getHostAddress() +
				": " +
				new String(line) );
		 } // end of printData
	 } // end of class
