



import java.util.HashMap;
import java.util.Map;

import spread.MembershipInfo;
import spread.SpreadConnection;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class ListenerSlave extends Thread implements Runnable {
	private SpreadConnection connection;
    public  boolean threadSuspended;
    public Map<String,Integer> hash;
    private String nome;
    private int prio;
    FilesWork files;
    int numReader, numWriters;
    
	public ListenerSlave(SpreadConnection aConn) {
		connection=aConn;
	}

	public ListenerSlave(SpreadConnection aConn,HashMap<String,Integer> hashMap){
		connection=aConn;
		hash = hashMap;
	}

	public ListenerSlave(SpreadConnection aConn,HashMap<String,Integer> hashMap,String name, int prioridade){
		connection=aConn;
		hash = hashMap;
		nome = name;
		prio = prioridade;
		files = new FilesWork(nome);
		numReader = 0;
		numWriters = 0;
	}
	
	
	private void DisplayMessage(SpreadMessage msg){
		try{
			
			byte dataMessage[] = msg.getData();
			String messageReceived = new String(dataMessage);
			
			if(messageReceived.contains("CREATETXTMSG")){
				String slaveResponsavel = messageReceived.split("&&&")[0];
				String txtNome= messageReceived.split("&&&")[1];			
				
				slaveResponsavel = slaveResponsavel.substring(13);
				
				if(nome.equals(slaveResponsavel)){
					files.criarTxt(txtNome);
				}
			}
			
			else if(messageReceived.contains("DELETETXTMSG")){
				String slaveResponsavel = messageReceived.split("&&&")[0];
				String txtNome= messageReceived.split("&&&")[1];	
				
				slaveResponsavel = slaveResponsavel.substring(13);
				
				if(nome.equals(slaveResponsavel)){
					if(numReader ==0 && numWriters==0){
						files.deletarTxt(txtNome);
					}
					else{
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.addGroup("clients");
						
						String mensagem = "ERRORMSG";
						msg2.setData(mensagem.getBytes());
						
						connection.multicast(msg2);
					}
				}
			}
			
			else if(messageReceived.contains("READTXTMSG")){
				String slaveResponsavel = messageReceived.split("&&&")[0]; 
				String txtNome= messageReceived.split("&&&")[1];
				
				String nomeClient = txtNome.split("###")[1];
				txtNome = txtNome.split("###")[0];
				
				slaveResponsavel = slaveResponsavel.substring(11);
				
				SpreadMessage msg2 = new SpreadMessage();
				msg2.setSafe();
				msg2.addGroup("clients");
				
				if(nome.equals(slaveResponsavel)){
					if(numWriters ==0){
						numReader ++;
						String texto = files.getTxt(txtNome);
						
						String mensagem = "CANREADTXTMSG "+slaveResponsavel+"&&&"+nomeClient+"###"+texto;
						System.out.println(mensagem);
						msg2.setData(mensagem.getBytes());
					}
					else{						
						String mensagem = "ERRORMSG "+nomeClient;
						msg2.setData(mensagem.getBytes());
						
					}
					connection.multicast(msg2);
				}
			}
			
			else if(messageReceived.contains("WRITETXTMSG")){
				String slaveResponsavel = messageReceived.split("&&&")[0]; 
				String txtNome= messageReceived.split("&&&")[1];
				
				String nomeClient = txtNome.split("###")[1];
				txtNome = txtNome.split("###")[0];
				
				slaveResponsavel = slaveResponsavel.substring(12);
				
				SpreadMessage msg2 = new SpreadMessage();
				msg2.setSafe();
				msg2.addGroup("clients");
				
				String mensagemWrite;
				
				if(nome.equals(slaveResponsavel)){
					if(numReader ==0 && numWriters==0){
						numWriters++;
						//files.writeTxt(txtNome);
						mensagemWrite = "CANWRITETXTMSG "+nome+"&&&"+nomeClient+"###"+txtNome;
						msg2.setData(mensagemWrite.getBytes());
					}
					else{
						
						mensagemWrite = "ERRORMSG "+nomeClient;
						msg2.setData(mensagemWrite.getBytes());
						
					}
					connection.multicast(msg2);
				}
			}
			
			else if(messageReceived.contains("FINISHRETXTMSG")){
				String slaveResponsavel = messageReceived.substring(15);
				
				if(nome.equals(slaveResponsavel)){
					numReader--;
				}
			}

			else if(messageReceived.contains("FINISHWRTXTMSG")){
				String slaveResponsavel = messageReceived.split("&&&")[0]; 
				String txtNome= messageReceived.split("&&&")[1];
				
				String linhaAdicionar = txtNome.split("###")[1];
				txtNome = txtNome.split("###")[0];
				
				slaveResponsavel = slaveResponsavel.substring(15);

				
				if(nome.equals(slaveResponsavel)){
					files.writeTxt(txtNome, linhaAdicionar);
					
					numWriters--;
				}
			}
			
			else if( msg.isMembership() ){
				MembershipInfo info = msg.getMembershipInfo();
				SpreadGroup members[] = info.getMembers();
				
				if(info.isCausedByDisconnect()) {
					String slaveLeft = info.getDisconnected().toString();
					slaveLeft = slaveLeft.substring(1);
					slaveLeft = slaveLeft.split("#")[0];
					
					//Mensagem Votacao
					String msgVotacao = "SLAVELEFTMSG "+ slaveLeft;

					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.setData(msgVotacao.getBytes());
					msg2.addGroup("masters");
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