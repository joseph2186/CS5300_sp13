import java.io.File;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class PageRank {
	public static boolean deleteDirectory(File directory) {
	    if(directory.exists()){
	        File[] files = directory.listFiles();
	        if(null!=files){
	            for(int i=0; i<files.length; i++) {
	                if(files[i].isDirectory()) {
	                    deleteDirectory(files[i]);
	                }
	                else {
	                    files[i].delete();
	                }
	            }
	        }
	    }
	    return(directory.delete());
	}
	public static void main(String[] args) throws Exception {        

        String input = "/home/joe/edges-output.txt";
        String output = "/home/joe/output";
        Counter  c= null;
        int count = 0;

        while(count < 30) {
	        // Create a new job
	        Job job = new Job();
	
	        // Set job name to locate it in the distributed environment
	        job.setJarByClass(PageRank.class);
	        job.setJobName("Page Rank");
	
	        // Set input and output Path, note that we use the default input format
	        // which is TextInputFormat (each record is a line of input)
	        if(count == 0)
	        	FileInputFormat.addInputPath(job, new Path(input));
	        else
	        	FileInputFormat.addInputPath(job, new Path(output+"_"+new Integer(count-1).toString()));
	        
	        FileOutputFormat.setOutputPath(job, new Path(output+"_"+new Integer(count).toString()));
	
	        // Set Mapper and Reducer class
	        job.setMapperClass(PageRankMapper.class);
	        job.setReducerClass(PageRankReducer.class);
	
	        // Set Output key and value
	        job.setOutputKeyClass(Text.class);
	        job.setOutputValueClass(Text.class);
	
	        //System.exit(job.waitForCompletion(true) ? 0 : 1);
	        job.waitForCompletion(true);
	        Counters c1 = job.getCounters();
	        c = c1.findCounter(PAGE_RANK_COUNTER.RESIDUAL);
	        System.out.println("residual-get value-->"+c.getValue());
	        System.out.println("residual-->"+(double)(c.getValue()/(685229.0*10000.0)));
	        count++;
        }
    }
}
