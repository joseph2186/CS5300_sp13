package PageRank;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;


public class Test_utility {
private HashMap<Integer, Integer> BlockMap = null;
	
	public static void blockMapper(){
		try {
			FileInputStream fis = new FileInputStream("/home/joe/nodes.txt");
			DataInputStream dis = new DataInputStream(fis);
			BufferedReader bufRead = new BufferedReader(new InputStreamReader(dis));
			String line = "";
			while((line = bufRead.readLine()) != null){
				String formatted = line.replaceAll("[ ]+", ";");
				String[] tokens = formatted.split(";");
				for (int i = 0 ; i < tokens.length ; i++) {
					System.out.println(i+"-->"+tokens[i]);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public static void main(String[] args) throws Exception {
		blockMapper();
	}
}
