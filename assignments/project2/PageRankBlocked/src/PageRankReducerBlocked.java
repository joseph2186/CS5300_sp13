import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;


public class PageRankReducerBlocked extends Reducer<Text, Text, Text, Text>{
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
        Double threshold = (Double) 0.01;
        ArrayList<String> edgeList = new ArrayList<String>();
        String output = "";
        
        while (itr.hasNext()) {
            input = itr.next();
            String inputStr = input.toString();
            inputTokens = inputStr.split(" ");
            if (inputTokens.length == 1){
            	pageRank = pageRank + Double.parseDouble(inputTokens[0]);
            } else {
            	oldPageRank = Double.parseDouble(inputTokens[0]);
            	for (int i=0 ; i < inputTokens.length ; i++){
            		edgeList.add(i, new String(inputTokens[i]));
            	}
            }
            
        }
        if (Double.compare(pageRank, threshold) > 1){
        	residual = (oldPageRank - pageRank)/pageRank;
        } else {
        	//System.out.println("error!!!");
        }
        if (Double.compare(threshold, residual) > 1){
        	//System.out.println("converged!!");
        	pageRank = oldPageRank;
        }
        
        //TODO: check if this condition is correct
        if (edgeList.size() > 0) {
        	edgeList.remove(0);
        }
        edgeList.add(0, new String(String.valueOf(pageRank)));
        for (int i = 0 ; i < edgeList.size() - 1 ; i++){
        	output += edgeList.get(i)+"\t";
        }
        output += edgeList.get(edgeList.size() - 1);
        
        Text outputText = new Text(output);
        context.getCounter(PAGE_RANK_COUNTER.RESIDUAL).increment(1);
        //System.out.println("counter-->"+context.getCounter(TEST_COUNTER.COUNTER_1).getValue());
        
        context.write(key, outputText);
        cleanup(context);
    }

}
