import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 
 * @author Timur Maistrenko
 * 
 * Simple program demonstrating the working principle of a distributed application
 * This file contains a client that connects to a server, and cyclically accepts a packet of double values, calculates their partial sum, and sends that sum to a client. 
 * 
 * Client operates in a serial fashion and is terminated once it receives null from a server
 *
 */
public class DIYAppWorker 
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception 
	{
		// Socket used to connect to a server
		Socket sock;
		ObjectInputStream in = null;
		PrintWriter out;
		// Partial sum accumulator
		double sum = 0;
		// Server packet format
		LinkedList<String> packet;


		if(args.length != 2) 
		{
			System.out.println("Error. Address and PortNum have to be specified as arguments.");
			return;
		}
		// Create socket
		try 
		{
			sock = new Socket(InetAddress.getByName(args[0]),Integer.parseInt(args[1]));
		}
		catch(Exception e)
		{
			System.out.println("Error while connecting to server");
			System.out.println(e);
			return;
		}
		
		System.out.println("Connected to server");
		
		try 
		{
			in = new ObjectInputStream(sock.getInputStream());
			out = new PrintWriter(sock.getOutputStream(),true);
			while(true)
			{
				sum=0;
				// Accepts server packet and checks whether its a termination signal
				Object uncheckedPacket=in.readObject();
				System.out.println("Packet received");
				if (uncheckedPacket==null)
					break;
				// Calculate packet's partial sum
				packet=(LinkedList<String>) uncheckedPacket;
				for(String str:packet) 
				{
					if(str != null) 
						sum += Double.parseDouble(str);			
				}
				System.out.println("Sending partial sum: "+sum);
				out.println(sum);
				
				 
				
			}
			System.out.println("Closing connection");
			sock.close();
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();

		}
		
	}
}
