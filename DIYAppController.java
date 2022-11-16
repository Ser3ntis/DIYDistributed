import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
/**
 * 
 * @author Timur Maistrenko
 * 
 * Simple program demonstrating the working principle of a distributed application
 * This file contains a server utilizing multiple threads to serve multiple clients
 * in a concurrent fashion
 * 
 * The server reads the data set file of double type values, breaks it down to smaller packets, sends those packets to multiple clients
 * for them to calculate the artioal sum of the packet they received, and adds each client's partial sum to the total sum.
 * Once all data set entries have been accounted for, the server displays the total sum.
 * 
 * The server consists of the main thread that preloads data and is responsible for program's initial setup and shutdown,
 * "Receptionist" thread that accepts new connections and creates "Waiter" threads,
 * and multiple "Waiter" threads that serve clients by sending them data packets and accepting and adding the calculated partial sum to the total sum
 *
 */
public class DIYAppController 
{ 
	// Max number of clients that can be served at once. The limit is capped to avoid memory and performance issues
	private static final double maxClients=10;
	// Max size of the queue of preloaded packets
	private static final int queueMax=50;
	// Max size of each packet (how many double values each packet can contain)
	private static int packetSize=50;
	// Total sum we are trying to calculate
	private static volatile double  sum = 0;
	// Client counter
	private static AtomicInteger clients = new AtomicInteger(0);
	// Queue of preloaded packets
	private static LinkedBlockingQueue<LinkedList<String>> queue=new LinkedBlockingQueue<LinkedList<String>>();
	// EOF flag for the data set file
	private static volatile boolean eof=false;	
	/**
	 * Main method is responsible for the program setup, shutdown, data display, and data reading
	 * 
	 * @param args: must accept 2 arguments: portNumber and datasetFileName
	 */
	
	public static void main(String [] args) throws Exception 
	{
		ServerSocket sock;
		Scanner infile;
		// Mutex lock for the packet queue. Currently unused, but may be used in the future to solve possible issues
		ReentrantLock queueLock=new ReentrantLock();
		// Mutex lock for the total sum.
		ReentrantLock sumLock=new ReentrantLock();
		
		if(args.length < 2 || args.length > 3) 
			
		{
			System.out.println("Error. Arguments to be provided: portNum fileName (optional)packetSize");
			return;
		}
		
		//Create a socket
		try 
		{
			sock=new ServerSocket(Integer.parseInt(args[0]));
		}
		catch(Exception e)
		{
			System.out.println("Error while creating a socket.\n"+ e);
			return;
		}
		
		//Open a file
		try
		{
			infile=new Scanner(new File(args[1]));
		}
		catch(Exception e)
		{
			System.out.println("File wasn't found or is unaccessable.\n"+ e);
			return;
		}
		
		if (args.length == 3)
		{
			packetSize=Integer.parseInt(args[2]);
		}
		//Preload data to improve performance
		System.out.println("Preloading data");
		while(queue.size()<queueMax&&infile.hasNext()) 
		{
			LinkedList<String> packet = new LinkedList<String>();
			while (packet.size()<packetSize&&infile.hasNext())
				packet.add(infile.nextLine());
			queue.put(packet);
		}
		eof=!infile.hasNext();
		System.out.println("Data preloaded successfully");
		
		// Create a "Receptioninst" thread
		Receptionist receptionist=new Receptionist(sock,queueLock,sumLock);
		receptionist.start();
		
		// Keep reading data from the dataset
		while(!eof) 
		{
			if (queue.size()<queueMax)
			{
			LinkedList<String> packet = new LinkedList<String>();
			while (packet.size()<packetSize&&infile.hasNext())
				packet.add(infile.nextLine());
			queue.put(packet);
			eof=!infile.hasNext();
			}
		}
		
		//Wait for all clients to finish their work
		while (queue.size()>0||clients.get()>0)
		{
			Thread.sleep(100);
		}
		//Print total sum
		System.out.println("The total sum is: " + sum);
		Thread.sleep(100);
		System.exit(0);;
	}
	
	/**
	 * "Receptionist" thread that accepts new client connections and creates "Waiter" threads that serve the aforementioned clients
	 * @author Timur Maistrenko
	 *
	 */
	protected static class Receptionist extends Thread
	{
		ServerSocket sock;
		ReentrantLock queueLock;
		ReentrantLock sumLock;
		
		/**
		 * Default constructor for a "Receptionist" thread
		 * @param sock: ServerSocket that will be used to accept new connections
		 * @param queueLock: ReentrantLock for packet queue synchronization
		 * @param sumLock: ReentrantLock for sum calculation synchronization
		 */
		protected  Receptionist(ServerSocket sock,ReentrantLock queueLock,ReentrantLock sumLock) 
		{
			this.sock = sock;
			this.queueLock=queueLock;
			this.sumLock=sumLock;
		}

		
		public void run()
		{
			System.out.println("Receptionist started");
			try
			{
				// Accepts new connections only if there is some data left to be calculated. If all data entries are accounted for - close the socket
				while(!(eof&&queue.isEmpty()))
				{
					//Only accept new users if the connection limit is not hit
					if (maxClients>=clients.get())
					{
						// Accept a connection and start a "Waiter" thread to serve the newly accepted client. Increment client counter
						Socket sockClient = sock.accept();
						System.out.println("New client connected "+sockClient.toString());
						Waiter waiter=new Waiter(sockClient,queueLock,sumLock);
						waiter.start();
						clients.incrementAndGet();
					}
					else
						Thread.sleep(50);
					
				}
				sock.close();
				System.out.println("Closing receptionist");
			}
			catch(Exception e)
			{
				System.out.println(e);
				return;
			}		
		}
	}
	
	/**
	 * "Waiter" serves a client by sending them a packet and receiving a partial sum. Sends a termination command to a client once the entire data set if counted
	 * @author Timur
	 *
	 */
	protected static class Waiter extends Thread
	{
		Socket sock;
		ReentrantLock queueLock;
		ReentrantLock sumLock;
		/**
		 * Default constructor for a "Waiter" thread
		 * @param sock: Socket that is used to talk to a client
		 * @param queueLock: ReentrantLock for packet queue synchronization
		 * @param sumLock: ReentrantLock for sum calculation synchronization
		 */
		protected  Waiter(Socket sock,ReentrantLock queueLock,ReentrantLock sumLock) 
		{
			this.sock = sock;
			this.queueLock=queueLock;
			this.sumLock=sumLock;
		}


		public void run()
		{
			System.out.println("Waiter started with TID "+Thread.currentThread().getId());
			try 
			{
				String buffer="";
				Scanner in = new Scanner(sock.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
				// Sends data to a client for as long as there are any unaccounted data entries
				while(!(eof&&queue.isEmpty())) 
				{
					LinkedList<String> packet = queue.poll();
					if (packet==null)
						Thread.sleep(100);
					else
					{
						buffer = "";
						
						out.writeObject(packet);
						System.out.println(Thread.currentThread().getId()+": Packet sent");
						buffer=in.nextLine();
						
						System.out.println(Thread.currentThread().getId() +": Partial sum received: "+ buffer);
						// Adds received partial sum to the total sum
						sumLock.lock();
						try
						{
							sum += Double.parseDouble(buffer);
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
						finally
						{
							sumLock.unlock();
						}
					}
						

				}
				// Sends null to a client as a shutdown command
				out.writeObject(null);
				System.out.println(Thread.currentThread().getId()+": Shutdown command sent to client");
				sock.close();
				System.out.println(Thread.currentThread().getId()+": Closing the thread");
				
			}
			catch(Exception e) 
			{
				e.printStackTrace();
			}
			finally
			{
				clients.decrementAndGet();
			}
		}
	}
}