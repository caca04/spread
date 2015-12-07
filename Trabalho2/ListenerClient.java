import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import spread.SpreadConnection;
import spread.SpreadMessage;

public class ListenerClient extends Thread implements Runnable {
	private SpreadConnection connection;
    public  boolean threadSuspended;
    public Map<String,Integer> hash;
    private String nome;
    private int prio;
    private Sleep conditional;
    private ArrayList<String> files = new ArrayList<String>();
    String slaveName = "error";
    
	public ListenerClient(SpreadConnection aConn) {
		connection=aConn;
	}

	public ListenerClient(SpreadConnection aConn,HashMap<String,Integer> hashMap){
		connection=aConn;
		hash = hashMap;
	}

	public ListenerClient(SpreadConnection aConn,HashMap<String,Integer> hashMap,String name, int prioridade){
		connection=aConn;
		hash = hashMap;
		nome = name;
		prio = prioridade;
	}
	
	public ListenerClient(SpreadConnection aConn,HashMap<String,Integer> hashMap,String name, int prioridade,Sleep cond){
		connection=aConn;
		hash = hashMap;
		nome = name;
		prio = prioridade;
		conditional = cond;
	}
	
	private void DisplayOpcoes(){
		System.out.print("\n" +
				 "==========\n" +
				 "Client Menu:\n" +
				 "==========\n" +
				 "\n" +
				 "\tc create text file\n"+ 
				 "\td delete text file\n" +
				 "\tr read a text file\n"+
				 "\tw write a text file\n" +
				 "\ts show current text files in slave\n" +
				 "\n");
	}
	
	private void DisplayMessage(SpreadMessage msg){
		try{
			byte dataMessage[] = msg.getData();
			String messageReceived = new String(dataMessage);
			Scanner scan = new Scanner(System.in);
			
			if(messageReceived.contains("MASTERNAMEMSG")){
				String clientName = messageReceived.split("&&&")[1];
				String masterName = messageReceived.split("&&&")[0];
				
				if(nome.equals(clientName)){
					System.out.println(masterName.substring(13));
				}
			}
			
			else if(messageReceived.contains("SHOWDIRECTORIESMSG")){
				String clientName = messageReceived.split("&&&")[0];
				String masterName = messageReceived.split("&&&")[1];				
				clientName = clientName.substring(19);
				
				if(nome.equals(clientName)){
					System.out.println(masterName);
				}
			}			

			else if(messageReceived.contains("HASMSG")){
				if(messageReceived.contains("false")){
					System.out.println("Slave nao existe");
				}
				else{
					slaveName = messageReceived.substring(7);
					
					String clientName = slaveName.split("&&&")[1];
					slaveName = slaveName.split("&&&")[0];
					
					if(nome.equals(clientName)){
						String mensagem;
						
						SpreadMessage msg2 = new SpreadMessage();
						msg2.setSafe();
						msg2.addGroup("slaves");
						
						DisplayOpcoes();
						
						String comando = scan.next();
						
						switch(comando){
							case "c":
								System.out.println("Digite o nome do txt a ser criado");
								String nomeInsert = scan.next();
								mensagem = "CREATETXTMSG "+slaveName+"&&&"+nomeInsert;
																
								msg2.setData(mensagem.getBytes());
								
								conditional.doNotify();
								break;
							case "d":
								System.out.println("Digite o nome do txt a ser deletado");
								String nomeDelete = scan.next();
								
								mensagem = "DELETETXTMSG "+slaveName+"&&&"+nomeDelete;
								msg2.setData(mensagem.getBytes());
								
								conditional.doNotify();
								break;
							case "r":
								System.out.println("Digite o nome do txt a ser lido");
								String nomeRead = scan.next();
								
								mensagem = "READTXTMSG "+slaveName+"&&&"+nomeRead + "###"+nome;
								msg2.setData(mensagem.getBytes());
								break;
							case "w":
								System.out.println("Digite o nome do txt a ser lido");
								String nomeWrite = scan.next();
								
								mensagem = "WRITETXTMSG "+slaveName+"&&&"+nomeWrite+ "###"+nome;
								msg2.setData(mensagem.getBytes());
								break;
							/*case "s":
								mensagem = "";
								msg2.setData(mensagem.getBytes());*/
						}
						connection.multicast(msg2);
					}
				}	
			}
			
			else if(messageReceived.contains("CANREADTXTMSG")){
				System.out.println(messageReceived);
				String slaveResponsavel = messageReceived.split("&&&")[0]; 
				String clientResponsavel= messageReceived.split("&&&")[1];
				
				String txtNome = clientResponsavel.split("###")[1];
				clientResponsavel = clientResponsavel.split("###")[0];
				
				slaveResponsavel = slaveResponsavel.substring(14);
				
				if(nome.equals(clientResponsavel)){
					System.out.println("Texto do arquivo:");
					System.out.println(txtNome);
					System.out.println("Digite qualquer coisa para terminar de ler");
					scan.next();
					
					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.addGroup("slaves");
					
					String mensagem = "FINISHRETXTMSG "+slaveName;
					msg2.setData(mensagem.getBytes());
					
					connection.multicast(msg2);
					
					conditional.doNotify();
				}
			}
			
			else if(messageReceived.contains("CANWRITETXTMSG")){
				String slaveResponsavel = messageReceived.split("&&&")[0]; 
				String clientResponsavel= messageReceived.split("&&&")[1];
				
				String txtNome = clientResponsavel.split("###")[1];
				clientResponsavel = clientResponsavel.split("###")[0];
				
				slaveResponsavel = slaveResponsavel.substring(15);
				
				if(nome.equals(clientResponsavel)){
					System.out.println("Digite a linHa a ser adicionada");
					String linhaAAdicionar = scan.next();
					
					SpreadMessage msg2 = new SpreadMessage();
					msg2.setSafe();
					msg2.addGroup("slaves");
					
					String mensagem = "FINISHWRTXTMSG "+slaveName+"&&&"+txtNome+"###"+linhaAAdicionar;
					msg2.setData(mensagem.getBytes());
					
					connection.multicast(msg2);
					
					conditional.doNotify();
				}
			}
			
			else if(messageReceived.contains("ERRORMSG")){
				String clientResponsavel = messageReceived.substring(8);
				conditional.doNotify();
			}
			
		}catch(Exception e){
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