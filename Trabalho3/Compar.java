import java.util.Comparator;

public class Compar implements Comparator<ArquivoSlave>{

	public int compare(ArquivoSlave as1, ArquivoSlave as2) {
		int quantArquivos1,quantArquivos2;
		
		quantArquivos1 = as1.numArquivos;
		quantArquivos2 = as2.numArquivos;
		
		if(quantArquivos1 > quantArquivos2){
			return 0;
		}
		else{
			return -1;
		}
	}

}
