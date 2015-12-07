
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

import spread.BasicMessageListener;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;


public class Client implements BasicMessageListener
{
	
	static Sleep conditional;
	
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
	
	private ListenerClient rt;
		
	private void PrintMenu(){
		// Print the menu.
		//////////////////
		System.out.print("\n" +
						 "==========\n" +
						 "Client Menu:\n" +
						 "==========\n" +
						 "\n" +
						 "\tm show current proxy\n"+ 
						 "\ts show current slaves\n" +
						 "\tc create a file\n"+ 
						 "\td delete a file\n"+ 
						 "\tw write a file\n"+ 
						 "\tr read a file\n"+ 
						 "\n");
	}
	
	public void autoJoin(){
		// Join the group.
		
		group = new SpreadGroup();
		try {
			group.join(connection, "clients");
			System.out.println("Joined " + group + ".");				
				
			inGroup = true;
		}catch (SpreadException e) {
		e.printStackTrace();
		}
	}
	
	private void ServerCommand(){
		Scanner s = new Scanner(System.in);
		
		// Get the input.
		/////////////////
		char command[] = new char[1024];
		int num = 0;
		try
		{
			num = keyboard.read(command);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		// Setup a tokenizer for the input.
		///////////////////////////////////
		StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(new String(command, 1, num - 1)));
		
		// Check what it is.
		////////////////////
		SpreadMessage msg;
		char buffer[];
		try{
			switch(command[0]){
			//whos proxy
			case 'm':
				//Manda mensagem pra os masters
				msg = new SpreadMessage();
				msg.setSafe();
				msg.addGroup("masters");

				String mensagem = "WHOSMASTERMSG "+name;
				msg.setData(mensagem.getBytes());
				// Send it.
				///////////
				connection.multicast(msg);
				break;
				
			//Show slaves
			case 's':
				//Manda mensagem pra os masters
				msg = new SpreadMessage();
				msg.setSafe();
				msg.addGroup("masters");

				String mensagem2 = "SHOWDIRECTORIESMSG "+name;
				msg.setData(mensagem2.getBytes());
				// Send it.
				///////////
				connection.multicast(msg);
				break;
				
			//Create
			case 'c':
				System.out.println("Digite o nome do arquivo que voce quer criar:");
				String nomeCreate = s.next();
				
				System.out.println("Digite quantas copias voce quer criar:");
				String numCopias = s.next();
				
				//Manda mensagem pra os masters
				msg = new SpreadMessage();
				msg.setSafe();
				msg.addGroup("masters");

				String mensagemCreate = "CREATEFILEMSG "+nomeCreate + "&&&"+numCopias+"###"+name;
				msg.setData(mensagemCreate.getBytes());
				// Send it.
				///////////
				connection.multicast(msg);
				
				conditional.doWait();
				break;
				
			//Delete
			case 'd':
				System.out.println("Digite o nome do arquivo que voce quer deletar:");
				String nomeDelete = s.next();
				
				//Manda mensagem pra os masters
				msg = new SpreadMessage();
				msg.setSafe();
				msg.addGroup("masters");

				String mensagemDelete = "DELETEFILEMSG "+nomeDelete + "&&&"+name;
				msg.setData(mensagemDelete.getBytes());
				// Send it.
				///////////
				connection.multicast(msg);
				
				conditional.doWait();
				break;
			//Read
			case 'r':
				System.out.println("Digite o nome do arquivo que voce quer ler:");
				String nomeLeitura = s.next();
				
				//Manda mensagem pra os masters
				msg = new SpreadMessage();
				msg.setSafe();
				msg.addGroup("masters");

				String mensagemLeitura = "LERFILEMSG "+nomeLeitura + "&&&"+name;
				msg.setData(mensagemLeitura.getBytes());
				// Send it.
				///////////
				connection.multicast(msg);
				
				conditional.doWait();
				break;
			//Write
			case 'w':
				System.out.println("Digite o nome do arquivo que voce quer escrever:");
				String nomeEscrita = s.next();
				
				//Manda mensagem pra os masters
				msg = new SpreadMessage();
				msg.setSafe();
				msg.addGroup("masters");

				String mensagemEscrita = "WRITEFILEMSG "+nomeEscrita + "&&&"+name;
				msg.setData(mensagemEscrita.getBytes());
				// Send it.
				///////////
				connection.multicast(msg);
				
				conditional.doWait();
				
				break;
				
			//interact slave
			/*case 'i':
				System.out.println("Digite o nome do slave que voce quer acessar:");
				String comando = s.next();
				
				msg = new SpreadMessage();
				msg.setSafe();
				msg.addGroup("masters");

				String mensagem3 = "SLAVEACESSOMSG "+ comando +"&&&"+name;
				msg.setData(mensagem3.getBytes());
					
				connection.multicast(msg);
				
				//Espera Listener
				conditional.doWait();
				
				break;*/
			default:
			// Unknown command.
			///////////////////
			System.out.println("Unknown command");
			
			// Show the menu again.
			///////////////////////
			PrintMenu();
			}
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	public Client(String Server, String address, int port,int prioridade){
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
		
		conditional = new Sleep();
		
		rt = new ListenerClient(connection,hash,name,priority,conditional);
		rt.start();
		
		// Joins Servers
		//////////////////////
		autoJoin();
		PrintMenu();
		
		while(true)
		{
			ServerCommand();
		}
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
		
		conditional = new Sleep();
				
		Client u = new Client(Server, address, port,priority);
	}

	@Override
	public void messageReceived(SpreadMessage arg0) {
		System.out.println("k");
		
	}
}