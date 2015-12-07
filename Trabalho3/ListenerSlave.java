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
			
			if(messageReceived.contains("CREATEFILEMSG")){
				String mensagemTotal = messageReceived.substring(14);
				
				String resto = mensagemTotal.split("&&&")[1];
				String txtNome= mensagemTotal.split("&&&")[0];
				
				String slaves = resto.split("@@@")[1];
				String client = resto.split("@@@")[0];
				
				String slaveAtual = slaves.split("###")[0];
				String isEnd = slaves.split("###")[1];
				
				if(nome.equals(slaveAtual)){
					files.criarTxt(txtNome);
					
					if(isEnd.equals("end")){
						String mensagemConfirm = "FINISHMSG&&&"+client;
						
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.setData(mensagemConfirm.getBytes());
						msg2.addGroup("clients");
						
						connection.multicast(msg2);
					}
					else{
						int primeira = slaves.indexOf("###");
						String restanteMsg = slaves.substring(primeira+3);
						
						String msgForward = "CREATEFILEMSG "+txtNome+"&&&"+client+"@@@" +restanteMsg;
						System.out.println(restanteMsg  +"  /// "+ msgForward);
						
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.setData(msgForward.getBytes());
						msg2.addGroup("slaves");
						
						connection.multicast(msg2);
					}
				}
				
			}
			
			if(messageReceived.contains("DELETEFILEMSG")){
				String mensagemTotal = messageReceived.substring(14);
				
				String resto = mensagemTotal.split("&&&")[1];
				String txtNome= mensagemTotal.split("&&&")[0];
				
				String slaves = resto.split("@@@")[1];
				String client = resto.split("@@@")[0];
				
				String slaveAtual = slaves.split("###")[0];
				String isEnd = slaves.split("###")[1];
				
				if(nome.equals(slaveAtual)){
					files.deletarTxt(txtNome);
					
					if(isEnd.equals("end")){
						String mensagemConfirm = "FINISHMSG&&&"+client;
						
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.setData(mensagemConfirm.getBytes());
						msg2.addGroup("clients");
						
						connection.multicast(msg2);
					}
					
					else{
						int primeira = slaves.indexOf("###");
						String restanteMsg = slaves.substring(primeira+3);
						
						String msgForward = "DELETEFILEMSG "+txtNome+"&&&"+client+"@@@" +restanteMsg;
						
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.setData(msgForward.getBytes());
						msg2.addGroup("slaves");
						
						connection.multicast(msg2);
					}
				}
			}
			
			//Write
			if(messageReceived.contains("WRITEFILEMSG")){
				String mensagemTotal = messageReceived.substring(13);
								
				String resto = mensagemTotal.split("&&&")[1];
				String txtNome= mensagemTotal.split("&&&")[0];
				
				String slaves = resto.split("@@@")[1];
				String client = resto.split("@@@")[0];
				
				String slaves2 = slaves.split("%%%")[1];
				String linha = slaves.split("%%%")[0];
						
				String slaveAtual = slaves2.split("###")[0];
				String isEnd = slaves2.split("###")[1];
				
				
				if(nome.equals(slaveAtual)){
					
					files.writeTxt(txtNome, linha);
					
					if(isEnd.equals("end")){
						String mensagemConfirm = "FINISHMSG&&&"+client;
						
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.setData(mensagemConfirm.getBytes());
						msg2.addGroup("clients");
						
						connection.multicast(msg2);
					}
					
					else{
						int primeira = slaves2.indexOf("###");
						String restanteMsg = slaves2.substring(primeira+3);
						
						String msgForward = "WRITEFILEMSG "+txtNome+"&&&"+client+"@@@"+linha+"%%%" +restanteMsg;
						
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.setData(msgForward.getBytes());
						msg2.addGroup("slaves");
						
						connection.multicast(msg2);
					}
				}
			}
			
			if(messageReceived.contains("LERFILEMSG")){
				String mensagemTotal = messageReceived.substring(11);
				
				String nomeClient = mensagemTotal.split("&&&")[1];
				String txtNome= mensagemTotal.split("&&&")[0];
				
				String nomeSlave = nomeClient.split("###")[1];
				nomeClient = nomeClient.split("###")[0];
				
				if(nome.equals(nomeSlave)){
					
					String texto = files.getTxt(txtNome);
					
					String mensagem = "CANREADTXTMSG "+txtNome+"&&&"+nomeClient+"###"+texto;
					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.setData(mensagem.getBytes());
					msg2.addGroup("clients");
					
					connection.multicast(msg2);
				}
			}
			
			/*else if(messageReceived.contains("READTXTMSG")){
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
			}*/
			
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
