//Nome Ricardo Goes de Meira, baseado nas classe de exemplo so //Spread "recThread"

import java.util.HashMap;
import java.util.Map;

import spread.SpreadConnection;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class recThread extends Thread implements Runnable {
	private SpreadConnection connection;
    public  boolean threadSuspended;
    public Map<String,Integer> hash;
    private String nome;
    private int prio;
    
	public recThread(SpreadConnection aConn) {
		connection=aConn;
	}

	public recThread(SpreadConnection aConn,HashMap<String,Integer> hashMap){
		connection=aConn;
		hash = hashMap;
	}

	public recThread(SpreadConnection aConn,HashMap<String,Integer> hashMap,String name, int prioridade){
		connection=aConn;
		hash = hashMap;
		nome = name;
		prio = prioridade;
	}
	
	private void printHash(){
		int bigestPri = 0;
		String biggestServer = null;
		
		for (Map.Entry<String, Integer> entry : hash.entrySet()) {
		    String key = entry.getKey();
		    int value = entry.getValue();
		    
		    if(value > bigestPri){
		    	bigestPri = value;
		    	biggestServer = key;
		    }
		    System.out.println("Server: "+ key + " // Priority: "+ value);
		}
		
		System.out.println("\nCurrent Master is "+ biggestServer + " with priority " + bigestPri + "\n");
		System.out.println("||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||| \n");
	}
	
	private void DisplayMessage(SpreadMessage msg)
	{
		try
		{
			byte dataMessage[] = msg.getData();
			String messageReceived = new String(dataMessage);
			
			if(messageReceived.contains("PRIORIDADEMSG")){
				String mensagem = "REPASSMSG "+ String.valueOf(prio) +"&&&"+ nome;
				System.out.println(mensagem);
				
				SpreadMessage msg2 = new SpreadMessage();
				msg2.setSafe();
				msg2.setData(mensagem.getBytes());
				msg2.addGroup(msg.getGroups()[0]);
				// Send it.
				///////////
				connection.multicast(msg2);
				//System.out.println(nomeServidor + " joined the group:");
				//printHash();
				
				
			}else if(messageReceived.contains("LEAVINGMSG")){
				String nomeServidor = messageReceived.substring(11);
				hash.remove(nomeServidor);
				
				System.out.println(nomeServidor + " left the group:");
				printHash();
				
			}else if(messageReceived.contains("REPASSMSG")){
				String prioridade = messageReceived.split("&&&")[0];
				String nomeServidor = messageReceived.split("&&&")[1];

				hash.put(nomeServidor, Integer.parseInt(prioridade.substring(10)));
				
			}
			else{
	   		        //System.out.println("*****************RECTHREAD Received Message************");
				if(msg.isRegular())
				{
					System.out.print("Received a ");
					if(msg.isUnreliable())
						System.out.print("UNRELIABLE");
					else if(msg.isReliable())
						System.out.print("RELIABLE");
					else if(msg.isFifo())
						System.out.print("FIFO");
					else if(msg.isCausal())
						System.out.print("CAUSAL");
					else if(msg.isAgreed())
						System.out.print("AGREED");
					else if(msg.isSafe())
						System.out.print("SAFE");
					System.out.println(" message.");
					
					System.out.println("Sent by  " + msg.getSender() + ".");
					
					System.out.println("Type is " + msg.getType() + ".");
					
					if(msg.getEndianMismatch() == true)
						System.out.println("There is an endian mismatch.");
					else
						System.out.println("There is no endian mismatch.");
					
					SpreadGroup groups[] = msg.getGroups();
					System.out.println("To " + groups.length + " groups.");
					
					byte data[] = msg.getData();
					System.out.println("The data is " + data.length + " bytes.");
					
					System.out.println("The message is: " + new String(data));
				}
				else if ( msg.isMembership() )
				{
				} else if ( msg.isReject() )
				{
				        // Received a Reject message 
					System.out.print("Received a ");
					if(msg.isUnreliable())
						System.out.print("UNRELIABLE");
					else if(msg.isReliable())
						System.out.print("RELIABLE");
					else if(msg.isFifo())
						System.out.print("FIFO");
					else if(msg.isCausal())
						System.out.print("CAUSAL");
					else if(msg.isAgreed())
						System.out.print("AGREED");
					else if(msg.isSafe())
						System.out.print("SAFE");
					System.out.println(" REJECTED message.");
					
					System.out.println("Sent by  " + msg.getSender() + ".");
					
					System.out.println("Type is " + msg.getType() + ".");
					
					if(msg.getEndianMismatch() == true)
						System.out.println("There is an endian mismatch.");
					else
						System.out.println("There is no endian mismatch.");
					
					SpreadGroup groups[] = msg.getGroups();
					System.out.println("To " + groups.length + " groups.");
					
					byte data[] = msg.getData();
					System.out.println("The data is " + data.length + " bytes.");
					
					System.out.println("The message is: " + new String(data));
				} else {
				    System.out.println("Message is of unknown type: " + msg.getServiceType() );
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void run() {
	  while(true) {
            try {
	      DisplayMessage(connection.receive());

	      if (threadSuspended) {
                synchronized(this) {
                    while (threadSuspended) {
                        wait();
		    }
                }
	      }
	    } catch(Exception e) {

	    }
	  }
        }
}