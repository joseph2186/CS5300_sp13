package PageRank;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class PageRankMapper extends Mapper<LongWritable, Text, Text, Text> {
	private Text word = new Text();
	private String delimiter = "\t";
	private String dummyList = "-1";

	@Override
	protected void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {
		String line = value.toString();
		Integer degree = 0;
		String[] words = line.split("\t");

		try {
			degree = new Integer(words[2]);
		} catch (ArrayIndexOutOfBoundsException e) {
			degree = 0;
		}
		// get pagerank
		Double pageRank = new Double(words[1]);

		// set the reducer node
		Text Node = new Text(words[0]);

		// if the outdegree of a node is 0, it does not share its pagerank
		if (degree != 0) {
			pageRank = (Double) pageRank / degree;

			Text edgeList = new Text();
			String tempList = "";
			for (int i = 1; i < words.length - 1; i++) {
				tempList += words[i] + delimiter;
			}
			tempList += words[words.length - 1];
			edgeList = new Text(tempList);

			// The metadata
			context.write(Node, edgeList);
			cleanup(context);

			// The transaction tuple
			Text nodeOut = new Text();
			Text pageRankText = new Text(String.valueOf(pageRank));
			for (int i = 3; i < words.length; i++) {
				nodeOut = new Text(words[i]);
				context.write(nodeOut, pageRankText);
			}
			cleanup(context);
		} else {
			Text nodeOut = new Text(words[0]);
			String output = pageRank.toString()
					+ delimiter + degree.toString() + delimiter + dummyList;
			Text tupleValue = new Text(output);
			context.write(nodeOut, tupleValue);
			cleanup(context);
		}
	}

}
