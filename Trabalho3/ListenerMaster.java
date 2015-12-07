import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

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
    public HashMap<String,ArrayList<ArquivoSlave>> arquivos;
    public HashMap<String,Integer> lock;  //0- livre  //1- leitura //2- escrita
    public PriorityQueue<ArquivoSlave> arqSlaves;
    
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
		arquivos = new HashMap<String,ArrayList<ArquivoSlave>>();
		numMembros = 0;
		votantes = 0;
		meusVotos = 0;
		
		lock = new HashMap<String,Integer>();
		arqSlaves = new PriorityQueue<ArquivoSlave>(11,new Compar());
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
	public void retirarPriority(String slaveLeft){
		ArrayList<ArquivoSlave> array = new ArrayList<ArquivoSlave>();
		System.out.println(arqSlaves.size());
		
		while(arqSlaves.size()>0){
			ArquivoSlave temp = arqSlaves.poll();
			if(temp.nomeSlave.equals(slaveLeft)){
				
			}
			else{
				array.add(temp);
			}
		}
		
		for(int i=0;i<array.size();i++){
			arqSlaves.add(array.get(i));
		}
		System.out.println(arqSlaves.size());
	}
	
	public void retirarHashMap(String slaveLeft){
	    Iterator it = arquivos.entrySet().iterator();
	    
	    //System.out.println("tamanho"+arquivos.size());
	    ArrayList<String> chaves = new ArrayList<String>();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        
	        String chave = (String) pair.getKey();
	        ArrayList<ArquivoSlave> valor = (ArrayList<ArquivoSlave>)pair.getValue();
	        
	       // System.out.println(chave + " = " + valor.toString());
	        for(int i=0;i<valor.size();i++){
	        	if(valor.get(i).nomeSlave.equals(slaveLeft)){
	        		valor.remove(i);
	        	}
	        }
	        //System.out.println(chave + " = " + valor.toString());
	        
	        if(valor.size()>0){
	        	arquivos.put(chave, valor);
	        }
	        else{
	        	chaves.add(chave);
	        }	        
	    }
	    
	    for(int i=0;i<chaves.size();i++){
	    	arquivos.remove(chaves.get(i));
	    }
	    
	    //System.out.println("tamanho"+arquivos.size());
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
				
				ArquivoSlave temp = new ArquivoSlave(messageReceived.substring(9),0);
				arqSlaves.add(temp);
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

			else if(messageReceived.contains("SLAVELEFTMSG")){
				String slaveLeft = messageReceived.substring(13);
				
				//Tira da lista de slaves
				for(int i=0;i<slaves.size();i++){
					if(slaves.get(i).equals(slaveLeft)){
						slaves.remove(i);
					}
				}
				
				//Tira da priorityList
				retirarPriority(slaveLeft);
				
				//Tira da lista de arquivos
				retirarHashMap(slaveLeft);
										
			}
			
			
			else if(messageReceived.contains("CREATEFILEMSG")){
		
				if(nome.equals(currentMaster)){
					String mensagemTotal = messageReceived.substring(14);
					
					String nomeArquivo = mensagemTotal.split("&&&")[0];
					String clientResponsavel = mensagemTotal.split("&&&")[1];
					
					String numeroArquivos = clientResponsavel.split("###")[0];
					clientResponsavel = clientResponsavel.split("###")[1];
					
					System.out.println(nomeArquivo +"//"+ clientResponsavel +"//" +numeroArquivos);
					
					if(slaves.size() >= Integer.parseInt(numeroArquivos)){
						ArrayList<ArquivoSlave> array = new ArrayList<ArquivoSlave>();
						
						String mensagem = "CREATEFILEMSG "+nomeArquivo+"&&&"+clientResponsavel+"@@@";
						
						for(int i=0;i<Integer.parseInt(numeroArquivos);i++){
							array.add(arqSlaves.poll());
							mensagem = mensagem.concat(array.get(i).nomeSlave + "###");
						}					
						mensagem = mensagem.concat("end");
												
						for(int i=0;i<array.size();i++){
							System.out.println(array.get(i).nomeSlave + " // " + array.get(i).numArquivos);
							array.get(i).setNumArquivos(array.get(i).getNumArquivos()+1);
							arqSlaves.add(array.get(i));
						}
						
						arquivos.put(nomeArquivo, array);
						lock.put(nomeArquivo, 0);

						//manda a mensagem
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.setData(mensagem.getBytes());
						msg2.addGroup("slaves");
						
						connection.multicast(msg2);
					}
					
					else{						
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.addGroup("clients");
						
						String mensagem = "ERRORMSG "+clientResponsavel+"&&&Nao existem slaves suficientes para essa quantidade de replicacoes";
						msg2.setData(mensagem.getBytes());
						
						connection.multicast(msg2);
					}
				}
			}
			
			else if(messageReceived.contains("DELETEFILEMSG")){
				String mensagemTotal = messageReceived.substring(14);
				String nomeArquivo = mensagemTotal.split("&&&")[0];
				String nomeClient = mensagemTotal.split("&&&")[1];
				
				if(nome.equals(currentMaster)){										
					if(arquivos.containsKey(nomeArquivo)){
						if(lock.get(nomeArquivo) == 0){
							lock.put(nomeArquivo, 2);
							
							ArrayList<ArquivoSlave> array = arquivos.get(nomeArquivo);
							arquivos.remove(nomeArquivo);
							lock.remove(nomeArquivo);
							
							String mensagemDelete = "DELETEFILEMSG "+nomeArquivo+"&&&"+nomeClient+"@@@";
							for(int i=0;i<array.size();i++){
								array.get(i).setNumArquivos(array.get(i).getNumArquivos()-1);
								mensagemDelete = mensagemDelete.concat(array.get(i).nomeSlave + "###");							
							}
							mensagemDelete = mensagemDelete.concat("end");
	
							//manda a mensagem
							SpreadMessage msg2 = new SpreadMessage();
							msg2.setSafe();
							msg2.setData(mensagemDelete.getBytes());
							msg2.addGroup("slaves");
							System.out.println(mensagemDelete);
							
							connection.multicast(msg2);
						}
						else{
							SpreadMessage msg2 = new SpreadMessage();
							msg2.setSafe();
							msg2.addGroup("clients");
							
							String mensagem = "ERRORMSG "+nomeClient+"&&&O arquivo esta sendo utilizado em uma outra operacao";
							msg2.setData(mensagem.getBytes());
							
							connection.multicast(msg2);
						}
					}
					else{
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.addGroup("clients");
						
						String mensagem = "ERRORMSG "+nomeClient+"&&&O arquivo nao existe";
						msg2.setData(mensagem.getBytes());
						
						connection.multicast(msg2);
					}
				}
			}
			
			
			else if(messageReceived.contains("LERFILEMSG")){
				String mensagemTotal = messageReceived.substring(11);				
				String nomeArquivo = mensagemTotal.split("&&&")[0];
				String nomeClient = mensagemTotal.split("&&&")[1];
				
				if(nome.equals(currentMaster)){		
					if(arquivos.containsKey(nomeArquivo)){
						if(lock.get(nomeArquivo) < 2){
							lock.put(nomeArquivo, 1);
							ArquivoSlave as = arquivos.get(nomeArquivo).get(0);
							
							String mensagemLer = "LERFILEMSG "+nomeArquivo+"&&&"+nomeClient+"###"+as.nomeSlave;
							
							//manda a mensagem
							SpreadMessage msg2 = new SpreadMessage();
							msg2.setSafe();
							msg2.setData(mensagemLer.getBytes());
							msg2.addGroup("slaves");
							System.out.println(mensagemLer);
							
							connection.multicast(msg2);
						}
						else{
							SpreadMessage msg2 = new SpreadMessage();
							msg2.setSafe();
							msg2.addGroup("clients");
							
							String mensagem = "ERRORMSG "+nomeClient+"&&&O arquivo esta sendo utilizado em uma outra operacao";
							msg2.setData(mensagem.getBytes());
							
							connection.multicast(msg2);
						}
					}
					else{
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.addGroup("clients");
						
						String mensagem = "ERRORMSG "+nomeClient+"&&&O arquivo nao existe";
						msg2.setData(mensagem.getBytes());
						
						connection.multicast(msg2);
					}
				}
			}
			
			else if(messageReceived.contains("FINISHRETXTMSG")){
				String arquivoNome = messageReceived.split("&&&")[1];
				
				System.out.println("///" + arquivoNome);
				
				lock.put(arquivoNome, 0);
			}

			
			else if(messageReceived.contains("WRITEFILEMSG")){
				String mensagemTotal = messageReceived.substring(13);				
				String nomeArquivo = mensagemTotal.split("&&&")[0];
				String nomeClient = mensagemTotal.split("&&&")[1];
				
				if(nome.equals(currentMaster)){							
					if(arquivos.containsKey(nomeArquivo)){
						if(lock.get(nomeArquivo) == 0){
							lock.put(nomeArquivo, 2);

							String mensagem = "CANWRITETXTMSG "+nomeClient+"###"+nomeArquivo;
							//manda a mensagem
							SpreadMessage msg2 = new SpreadMessage();
							msg2.setSafe();
							msg2.setData(mensagem.getBytes());
							msg2.addGroup("clients");
							System.out.println(mensagem);
							
							connection.multicast(msg2);
						}
						else{
							SpreadMessage msg2 = new SpreadMessage();
							msg2.setSafe();
							msg2.addGroup("clients");
							
							String mensagem = "ERRORMSG "+nomeClient+"&&&O arquivo esta sendo utilizado em uma outra operacao";
							msg2.setData(mensagem.getBytes());
							
							connection.multicast(msg2);
						}
					}
					else{
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.addGroup("clients");
						
						String mensagem = "ERRORMSG "+nomeClient+"&&&O arquivo nao existe";
						msg2.setData(mensagem.getBytes());
						
						connection.multicast(msg2);
					}
				}
			}
			
			else if(messageReceived.contains("FINISHWRTXTMSG")){
				 
				String mensagemTotal = messageReceived.substring(15);	
				
				String nomeArquivo = mensagemTotal.split("###")[0];
				String nomeCliente = mensagemTotal.split("###")[1];
				
				String linha = nomeCliente.split("@@@")[0];
				nomeCliente = nomeCliente.split("@@@")[1];
				
				System.out.println(nomeArquivo + " /// "+ linha + " /// "+ nomeCliente );
				if(nome.equals(currentMaster)){	
					ArrayList<ArquivoSlave> array = arquivos.get(nomeArquivo);
					
					String mensagem = "WRITEFILEMSG "+nomeArquivo+"&&&"+nomeCliente+"@@@"+linha+"%%%";
					
					for(int i=0;i<array.size();i++){
						mensagem = mensagem.concat(array.get(i).nomeSlave +"###");
					}					

					mensagem = mensagem.concat("end");											

					System.out.println(mensagem);
					
					lock.put(nomeArquivo, 0);
					//manda a mensagem
					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.setData(mensagem.getBytes());
					msg2.addGroup("slaves");
					
					connection.multicast(msg2);
				}
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