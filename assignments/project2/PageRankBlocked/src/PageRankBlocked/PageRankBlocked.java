package PageRankBlocked;

import java.io.File;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class PageRankBlocked {
	private static Double residualThreshold = 0.001;

	public static boolean deleteDirectory(File directory) {
		if (directory.exists()) {
			File[] files = directory.listFiles();
			if (null != files) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						deleteDirectory(files[i]);
					} else {
						files[i].delete();
					}
				}
			}
		}
		return (directory.delete());
	}

	public static void main(String[] args) throws Exception {

		String input = "/home/joe/edges-output.txt";
		String output = "/home/joe/output_residual";
		Counter c = null;
		int count = 0;
		Double residual = 1.0;

		//while (Double.compare(residual, residualThreshold) > 0) {
		while (count < 1) {
			// Create a new job
			Job job = new Job();

			// Set job name to locate it in the distributed environment
			job.setJarByClass(PageRankBlocked.class);
			job.setJobName("Page Rank");

			// Set input and output Path, note that we use the default input
			// format
			// which is TextInputFormat (each record is a line of input)
			if (count == 0) {
				FileInputFormat.addInputPath(job, new Path(input));
			} else {
				FileInputFormat.addInputPath(job, new Path(output + "_"
						+ new Integer(count - 1).toString()));
			}

			FileOutputFormat.setOutputPath(job, new Path(output + "_"
					+ new Integer(count).toString()));

			// Set Mapper and Reducer class
			job.setMapperClass(PageRankMapperBlocked.class);
			job.setReducerClass(PageRankReducerBlocked.class);

			// Set Output key and value
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);

			// System.exit(job.waitForCompletion(true) ? 0 : 1);
			job.waitForCompletion(true);
			Counters c1 = job.getCounters();
			c = c1.findCounter(PAGE_RANK_COUNTER.RESIDUAL);
			System.out.println("residual-get value-->" + c.getValue());
			residual = (double) (c.getValue() / (67.0 * 10000.0));
			System.out.println("residual-->" + residual);
			count++;
			System.out.println("number of passes-->" + count);
		}
	}
}
