import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class FilesWork {
	static String location = "/home/ricardo/FIleSystem";
	
	public FilesWork(String nome){
		location = location + "/"+nome;
	}
	
	public void criarTxt(String nome){
		
		//Cria arquivo
		File file = new File(location+"/"+nome);
		
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			System.out.println("Arquivo ja existe");
		}
	}
	
	public void deletarTxt(String nome){
		File file = new File(location+"/"+nome);
		Path path = file.toPath();
		
		try {
		    Files.delete(file.toPath());
		} catch (NoSuchFileException x) {
		    System.err.format("%s: no such" + " file or directory%n", path);
		} catch (DirectoryNotEmptyException x) {
		    System.err.format("%s not empty%n", path);
		} catch (IOException x) {
		    // File permission problems are caught here.
		    System.err.println(x);
		}
	}
	
	public String getTxt(String nome){
		String text = "a";
		
		try (BufferedReader br = new BufferedReader(new FileReader(location + "/"+nome))){

			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null) {
				text.concat(sCurrentLine);
				System.out.println(sCurrentLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		return text;
	}
	
	public void writeTxt(String nome,String content){
		File file = new File(location+"/"+nome);
		
		try {

			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
