import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import spread.MembershipInfo;
import spread.SpreadConnection;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class ListenerMaster extends Thread implements Runnable {
	private SpreadConnection connection;
    public  boolean threadSuspended;
    public Map<String,Integer> hash;
    private String nome;
    private int prio;
    public ArrayList<String> slaves;
    public String currentMaster;
    public int numMembros;
    public int repassMembros;
    public int votantes;
    public int meusVotos;
    
	public ListenerMaster(SpreadConnection aConn) {
		connection=aConn;
	}

	public ListenerMaster(SpreadConnection aConn,HashMap<String,Integer> hashMap){
		connection=aConn;
		hash = hashMap;
	}

	public ListenerMaster(SpreadConnection aConn,HashMap<String,Integer> hashMap,String name, int prioridade){
		connection=aConn;
		hash = hashMap;
		nome = name;
		prio = prioridade;
		currentMaster = null;
		slaves = new ArrayList<String>();
		numMembros = 0;
		votantes = 0;
		meusVotos = 0;
	}
	
	private String biggestPriority(){
		int bigestPri = 0;
		String biggestServer = null;
		
		for (Map.Entry<String, Integer> entry : hash.entrySet()) {
		    String key = entry.getKey();
		    int value = entry.getValue();
		    
		    if(value > bigestPri){
		    	bigestPri = value;
		    	biggestServer = key;
		    }
		}
		
		return biggestServer;
	}
	
	public void checarMaster(){
		int bigestPri = 0;
		String biggestServer = null;
		
		for (Map.Entry<String, Integer> entry : hash.entrySet()) {
		    String key = entry.getKey();
		    int value = entry.getValue();
		    
		    if(value > bigestPri){
		    	bigestPri = value;
		    	biggestServer = key;
		    }
		}
		currentMaster = biggestServer;
	}
	
	private void DisplayMessage(SpreadMessage msg)
	{
		try
		{
			byte dataMessage[] = msg.getData();
			String messageReceived = new String(dataMessage);
			
			if(messageReceived.contains("PRIORIDADEMSG")){
				String mensagem = "REPASSMSG "+ String.valueOf(prio) +"&&&"+ nome;
				//System.out.println(mensagem);
				
				SpreadMessage msg2 = new SpreadMessage();
				msg2.setSafe();
				msg2.setData(mensagem.getBytes());
				msg2.addGroup(msg.getGroups()[0]);
				// Send it.
				///////////
				connection.multicast(msg2);		
				
			}			
			
			else if(messageReceived.contains("REPASSMSG")){
				String prioridade = messageReceived.split("&&&")[0];
				String nomeServidor = messageReceived.split("&&&")[1];

				hash.put(nomeServidor, Integer.parseInt(prioridade.substring(10)));
				repassMembros++;
				
				if(repassMembros == numMembros){
					votantes = 0;
					meusVotos = 0;
					currentMaster = null;
					
					String msgVotacao = "VOTACAOMSG "+ biggestPriority();

					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.setData(msgVotacao.getBytes());
					msg2.addGroup(msg.getGroups()[0]);
					// Send it.
					///////////
					connection.multicast(msg2);
				}
			}
			
			else if(messageReceived.contains("VOTACAOMSG")){
				String voto = messageReceived.substring(11);
				votantes++;
				
				System.out.println("voto "+voto+" // nome "+nome);
				if(nome.equals(voto)){
					meusVotos++;
				}

				if(votantes == numMembros){
					System.out.println("Votos "+meusVotos +" // numMembros "+ numMembros);
					if(meusVotos >= (numMembros/2)+1){
						currentMaster = nome;
					}
				}
			}
			
			else if(messageReceived.contains("SLAVEMSG")){
				System.out.println("Slave adicionado // "+ messageReceived.substring(9));
				
				slaves.add(messageReceived.substring(9));
			}
			
			else if(messageReceived.contains("WHOSMASTERMSG")){
				String clientName = messageReceived.substring(14);
				
				String mensagem = "MASTERNAMEMSG "+ biggestPriority() + "&&&"+clientName;
				
				if(nome.equals(currentMaster)){
					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.setData(mensagem.getBytes());
					msg2.addGroup("clients");
					
					connection.multicast(msg2);
				}
			}
			
			else if(messageReceived.contains("SHOWDIRECTORIESMSG")){
				String clientName = messageReceived.substring(19);
				String mensagem = "SHOWDIRECTORIESMSG "+ clientName + "&&&"+slaves.toString();
				
				System.out.println(clientName);
				if(nome.equals(currentMaster)){
					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.setData(mensagem.getBytes());
					msg2.addGroup("clients");
					
					connection.multicast(msg2);
				}
			}
			
			else if(messageReceived.contains("SLAVEACESSOMSG")){
				String nomeSlave = messageReceived.split("&&&")[0];
				String clientName = messageReceived.split("&&&")[1];
				nomeSlave = nomeSlave.substring(15);
				
				if(nome.equals(currentMaster)){
					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.addGroup("clients");
					
					String msgEnvio;
					boolean acesso = false;
					
					for(int i=0;i<slaves.size();i++){
						if(slaves.get(i).equals(nomeSlave)){
							acesso=true;
						}
					}
					
					if(acesso == true){
						msgEnvio= "HASMSG "+ nomeSlave +"&&&"+clientName;
						msg2.setData(msgEnvio.getBytes());
					}
					else{
						msgEnvio= "HASMSG false";
						msg2.setData(msgEnvio.getBytes());
					}
					
					connection.multicast(msg2);
				}
			}
			else if(messageReceived.contains("SLAVELEFTMSG")){
				String slaveLeft = messageReceived.substring(13);
				
				for(int i=0;i<slaves.size();i++){
					if(slaves.get(i).equals(slaveLeft)){
						slaves.remove(i);
					}
				}
				System.out.println(slaveLeft);
			}
			
			else if( msg.isMembership() ){
				MembershipInfo info = msg.getMembershipInfo();
				SpreadGroup members[] = info.getMembers();
				if(info.isCausedByJoin()) {
					numMembros = members.length;
					repassMembros = 0;
				}
				
				else if(info.isCausedByDisconnect()) {
					numMembros --;
					votantes = 0;
					meusVotos = 0;
					currentMaster = null;
					
					String masterLeft = info.getDisconnected().toString();
					masterLeft = masterLeft.substring(1);
					masterLeft = masterLeft.split("#")[0];
					
					hash.remove(masterLeft);
					
					//Mensagem Votacao
					String msgVotacao = "VOTACAOMSG "+ biggestPriority();

					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.setData(msgVotacao.getBytes());
					msg2.addGroup(msg.getGroups()[0]);
					// Send it.
					///////////
					connection.multicast(msg2);
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