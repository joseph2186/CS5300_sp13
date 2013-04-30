import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.commons.collections.map.HashedMap;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;

public class PageRankMapperBlocked extends
		Mapper<LongWritable, Text, Text, Text> {
	private Text word = new Text();
	private static HashMap<String, String> BlockMap = new HashMap<String, String>();
	String delimiter = "\t";
	
//	private void blockMapper(){
	static {
		try {
			System.out.println("block mapper enter!");
			FileInputStream fis = new FileInputStream("/home/joe/nodes.txt");
			DataInputStream dis = new DataInputStream(fis);
			BufferedReader bufRead = new BufferedReader(new InputStreamReader(dis));
			String line = "";
			while((line = bufRead.readLine()) != null){
				line = line.trim();
				String formatted = line.replaceAll("[ ]+", ";");
				String[] tokens = formatted.split(";");
				//tokens[1] : node
				//tokens[2] : block
				BlockMap.put(tokens[0], tokens[1]);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	protected void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {
		String line = value.toString();
		String[] words = line.split("\t");

		//TODO: need to move it to run once
//		blockMapper();
		
		//degree and pagerank
		Integer degree = 0;
		try {
        	degree = new Integer(words[2]);
        } catch(ArrayIndexOutOfBoundsException e){
        	degree = 0;
        }
		Float pageRank = new Float(words[1]);

		//source node
		String source = words[0];

		if (degree != 0) {
			pageRank = (float) pageRank / degree;

			Text edgeList = new Text();
			String tempList = "";
			for (int i = 0; i < words.length - 1; i++) {
				tempList += words[i] + delimiter;
			}
			tempList += words[words.length - 1];
			edgeList = new Text(tempList);
			
			//map the block id for the source node
			Text BlockSource = new Text(BlockMap.get(source));
	
			//Metadata tuple
			context.write(BlockSource, edgeList);
			cleanup(context);
			
			Text nodeOut = new Text();
			Text tupleValue = new Text();
			for (int i = 3; i < words.length; i++) {
				//condition when both the nodes are in the same block - BE
				if (BlockMap.get(source).equalsIgnoreCase(BlockMap.get(words[i]))){
					nodeOut = new Text(BlockMap.get(source));
					tupleValue = new Text(source+delimiter+words[i]);
				}
				//condition when both the nodes are in different blocks - BC
				else
				{
					nodeOut = new Text(BlockMap.get(words[i]));
					tupleValue = new Text(source+delimiter+words[i]+delimiter+String.valueOf(pageRank));
				}
				context.write(nodeOut, tupleValue);
				cleanup(context);
			}
		}
	}
}
