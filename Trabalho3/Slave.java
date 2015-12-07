
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import spread.BasicMessageListener;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;


public class Slave implements BasicMessageListener
{
	//THe fileSystem
	String location = "/home/ricardo/FIleSystem";
	
	// The Spread Connection.
	/////////////////////////
	private SpreadConnection connection;
	
	// The keyboard input.
	//////////////////////
	private InputStreamReader keyboard;
	
	// A group.
	///////////
	private SpreadGroup group;
	
	// The number of messages sent.
	///////////////////////////////
	private int numSent;

	// True if there is a listening thread.
	///////////////////////////////////////
	private boolean listening;
	
	//Priority of the file
	private int priority;
	
	//Name of the server
	private String name;
	
	//The processes discovered in the group
	private HashMap<String,Integer> hash = new HashMap<String,Integer>();
	 
	//Boolean to check if it is in a group
	private boolean inGroup = false;
	
	private ListenerSlave rt;
		
	public void autoJoin(){
		// Join the group.
		SpreadMessage msg;
		
		group = new SpreadGroup();
		try {
			group.join(connection, "slaves");
			System.out.println("Joined " + group + ".");
				
			//Manda mensagem pra os masters
			msg = new SpreadMessage();
			msg.setSafe();
			msg.addGroup("masters");

			String mensagem = "SLAVEMSG "+name;
			msg.setData(mensagem.getBytes());
			// Send it.
			///////////
			connection.multicast(msg);
				
			inGroup = true;
		}catch (SpreadException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		}
	}

	public Slave(String Server, String address, int port,int prioridade)
	{
		name = Server;
		//Sets the priority
		priority = prioridade;
		
		//Add himself to the HashMap
		hash.put(name, priority);
		
		// Setup the keyboard input.
		////////////////////////////
		keyboard = new InputStreamReader(System.in);
		
		// Establish the spread connection.
		///////////////////////////////////
		try
		{
			connection = new SpreadConnection();
			connection.connect(InetAddress.getByName(address), port, Server, false, true);
		}
		catch(SpreadException e)
		{
			System.err.println("There was an error connecting to the daemon.");
			e.printStackTrace();
			System.exit(1);
		}
		catch(UnknownHostException e)
		{
			System.err.println("Can't find the daemon " + address);
			System.exit(1);
		}
		
		rt = new ListenerSlave(connection,hash,name,priority);
		rt.start();
		
		// Joins Servers
		//////////////////////
		autoJoin();
		
		//Cria a pasta para o slave
		new File("/home/ricardo/FIleSystem/"+name).mkdirs();
	}
	
	public final static void main(String[] args)
	{			
		// Default values.
		//////////////////
		String Server = new String("Server");
		String address = null;
		int port = 0;
		int priority = 0;
		// Check the args.
		//////////////////
		for(int i = 0 ; i < args.length ; i++)
		{
			// Check for Server.
			//////////////////
			if((args[i].compareTo("-u") == 0) && (args.length > (i + 1)))
			{
				// Set Server.
				////////////
				i++;
				Server = args[i];
			}
			// Check for server.
			////////////////////
			else if((args[i].compareTo("-s") == 0) && (args.length > (i + 1)))
			{
				// Set the server.
				//////////////////
				i++;
				address = args[i];
			}
			// Check for port.
			//////////////////
			else if((args[i].compareTo("-p") == 0) && (args.length > (i + 1)))
			{
				// Set the port.
				////////////////
				i++;
				port = Integer.parseInt(args[i]);
			}
			else
			{
				System.out.print("Usage: Server\n" + 
								 "\t[-u <Server name>]   : unique Server name\n" + 
								 "\t[-s <address>]     : the name or IP for the daemon\n" + 
								 "\t[-p <port>]        : the port for the daemon\n"
								 );
				System.exit(0);
			}
		}
		
		Slave u = new Slave(Server, address, port,priority);
	}

	@Override
	public void messageReceived(SpreadMessage arg0) {
		System.out.println("k");
		
	}
}