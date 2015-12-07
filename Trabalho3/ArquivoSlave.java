
public class ArquivoSlave {
	public String nomeSlave;
	public int numArquivos;
	
	ArquivoSlave(String nome, int inicial){
		nomeSlave = nome;
		numArquivos = inicial;
	}

	public int getNumArquivos() {
		return numArquivos;
	}

	public void setNumArquivos(int numArquivos) {
		this.numArquivos = numArquivos;
	}
	
	
}
