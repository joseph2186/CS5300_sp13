import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;


public class PageRankMapper extends Mapper<LongWritable, Text, Text, Text>{
    private Text word = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        String line = value.toString();
        String[] words = line.split("\t");
        
        Integer degree = new Integer(words[2]);
        Float pageRank = new Float(words[1]);
        
        Text Node = new Text(words[0]);
        
        if (degree == 0){
        	pageRank = (float)1.0;
        } else {
        	pageRank = (float)pageRank/degree;
        }
        
        Text edgeList = new Text();
        String tempList = "";
        for (int i = 1 ; i < words.length -1 ; i++){
        	tempList += words[i]+" ";
        }
        tempList += words[words.length-1];
        edgeList = new Text(tempList);
        
        context.write(Node, edgeList);
        cleanup(context);
        Text nodeOut = new Text();
        Text pageRankText = new Text(String.valueOf(pageRank));
        for (int i = 3 ; i < words.length ; i++) {
            nodeOut = new Text(words[i]);
            context.write(nodeOut, pageRankText);
            cleanup(context);
        }
    }

}
