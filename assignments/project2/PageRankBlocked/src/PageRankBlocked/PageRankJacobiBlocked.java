package PageRankBlocked;

import java.io.File;
import java.text.DecimalFormat;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class PageRankBlocked {
	private static Double residualThreshold = 0.001;

	private static Double roundDoubles(double d) {
		DecimalFormat twoDForm = new DecimalFormat("#.####");
		return Double.valueOf(twoDForm.format(d));
	}

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
		
		//geet test
		String input = "/home/joe/processed-output-new.txt";
		String output = "/home/joe/output_random";
		
		Counter c = null;
		int count = 0;
		Double residual = 1.0;
		Long blockCount = (long) 0;

		while (Double.compare(residual, residualThreshold) > 0) {
			// while (count < 1) {
			// Create a new job
			Job job = new Job();

			// Set job name to locate it in the distributed environment
			job.setJarByClass(PageRankBlocked.class);
			job.setJobName("Page Rank");

			// Set input and output Path, note that we use the default input
			// format
			// which is TextInputFormat (each record is a line of input)
			if (count == 0) {
				//FileInputFormat.addInputPath(job, new Path(args[1]));
				FileInputFormat.addInputPath(job, new Path(input));
			} else {
				FileInputFormat.addInputPath(job, new Path(output + "_"
						+ new Integer(count - 1).toString()));
			}

			FileOutputFormat.setOutputPath(job, new Path(output + "_"
					+ new Integer(count).toString()));

			// Set Mapper and Reducer class
			job.setMapperClass(PageRankMapperBlocked.class);
			job.setReducerClass(PageRankReducerJacobiBlocked.class);

			// Set Output key and value
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);

			job.waitForCompletion(true);

			blockCount = job.getCounters()
					.findCounter(PAGE_RANK_COUNTER.BLOCK_COUNT).getValue();

			System.out.println("block count-->" + blockCount);
			System.out.println("node count-->"
					+ job.getCounters()
							.findCounter(PAGE_RANK_COUNTER.NODE_COUNT)
							.getValue());

			Double blockResidual = (double) job.getCounters()
					.findCounter(PAGE_RANK_COUNTER.RESIDUAL).getValue();

			residual = (double) (blockResidual / (job.getCounters()
					.findCounter(PAGE_RANK_COUNTER.NODE_COUNT).getValue() * 10000.0));
			System.out.println("residual-->" + residual);
			count++;
			System.out.println("number of passes-->" + count);
			System.out.println("Average block iterations-->"
					+ job.getCounters()
							.findCounter(PAGE_RANK_COUNTER.BLOCK_ITERATION)
							.getValue() / 68.0);

		}
	}
}
