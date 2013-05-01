package PageRank;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;


public class PageRankReducer extends Reducer<Text, Text, Text, Text>{

    @Override
    protected void reduce(Text key, Iterable<Text> values, 
            Context context)
            throws IOException, InterruptedException {
        Iterator<Text> itr = values.iterator();
        Text input = new Text();
        String[] inputTokens = null;
        Double pageRank = (Double) 0.0;
        Double oldPageRank = (Double) 0.0;
        Double residual = (Double) 0.0;
        Double threshold = (Double) 0.001;
        ArrayList<String> edgeList = new ArrayList<String>();
        String output = "";
        
        while (itr.hasNext()) {
            input = itr.next();
            String inputStr = input.toString();
            inputTokens = inputStr.split("\t");
            if (inputTokens.length == 1){
            	pageRank = pageRank + Double.parseDouble(inputTokens[0]);
            } else {
            	oldPageRank = Double.parseDouble(inputTokens[0]);
            	for (int i=0 ; i < inputTokens.length ; i++){
            		edgeList.add(i, new String(inputTokens[i]));
            	}
            }
            
        }
        
        //calculate the new page rank
        pageRank = ((1.0 - 0.85)/685229.0) + (0.85)*pageRank;
        residual = Math.abs((oldPageRank - pageRank))/pageRank;
        
    	Long residualCount = (long) (residual * 10000);
    	context.getCounter(PAGE_RANK_COUNTER.RESIDUAL).increment(residualCount);
        
        if (edgeList.size() > 0) {
        	edgeList.remove(0);
        }
        edgeList.add(0, new String(String.valueOf(pageRank)));
        
        for (int i = 0 ; i < edgeList.size() - 1 ; i++){
        	output += edgeList.get(i)+"\t";
        }
        output += edgeList.get(edgeList.size() - 1);
        
        Text outputText = new Text(output);
        
        context.write(key, outputText);
        cleanup(context);
    }

}
