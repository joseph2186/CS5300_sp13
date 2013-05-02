package PageRankBlocked;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
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
	private static String nodePath = "s3n://edu-cornell-cs-cs5300s13-jkb243-pagerank/nodes.txt";
	private static String nodePathLocal = "/home/joe/nodes.txt";
	private static boolean EXTRA_CREDIT_FLAG = false;
	private static String dummyList = "-1";
	private static Integer TOTAL_NODES = 685229;
	private static Integer RANDOM = 100;

	private static boolean TEST_FLAG = false;
	private static int[] blockList = { 10328, 20373, 30629, 40645, 50462,
			60841, 70591, 80118, 90497, 100501, 110567, 120945, 130999, 140574,
			150953, 161332, 171154, 181514, 191625, 202004, 212383, 222762,
			232593, 242878, 252938, 263149, 273210, 283473, 293255, 303043,
			313370, 323522, 333883, 343663, 353645, 363929, 374236, 384554,
			394929, 404712, 414617, 424747, 434707, 444489, 454285, 464398,
			474196, 484050, 493968, 503752, 514131, 524510, 534709, 545088,
			555467, 565846, 576225, 586604, 596585, 606367, 616148, 626448,
			636240, 646022, 655804, 665666, 675448, 685230 };
	private static Long blockCount = (long) 0;

	private static int getBlockIdForNode(int[] nodesArray, int nodeId) {
		int low = 0;
		int high = nodesArray.length - 1;
		int mid = (low + high) / 2;
		if (nodeId < nodesArray[0]) {
			return 0;
		}
		while (true) {
			mid = (low + high) / 2;
			if (nodeId > nodesArray[mid]) {
				if (nodeId < nodesArray[mid + 1]) {
					return mid + 1;
				}
				low = mid;
			} else if (nodeId < nodesArray[mid]) {
				if (nodeId > nodesArray[mid - 1]) {
					return mid;
				}
				high = mid;
			} else {
				return mid;
			}
		}
	}

	// private void blockMapper(){
	static {
		if (EXTRA_CREDIT_FLAG == true) {
			for (int i = 0; i <= TOTAL_NODES; i++) {
				BlockMap.put(new Integer(i).toString(),
						new Integer(i % RANDOM).toString());
			}
			blockCount = (long) RANDOM;

		} else if (TEST_FLAG == true) {
			System.out.println("block mapper enter!");
			for (int i = 0; i <= TOTAL_NODES; i++) {
				BlockMap.put(new Integer(i).toString(), new Integer(
						getBlockIdForNode(blockList, i) + 1).toString());
			}
			blockCount = (long) blockList.length;

		} else {
			try {
				System.out.println("block mapper enter!");
				FileInputStream fis = new FileInputStream(nodePathLocal);
				DataInputStream dis = new DataInputStream(fis);
				BufferedReader bufRead = new BufferedReader(
						new InputStreamReader(dis));
				String line = "";
				while ((line = bufRead.readLine()) != null) {
					line = line.trim();
					String formatted = line.replaceAll("[ ]+", ";");
					String[] tokens = formatted.split(";");
					// tokens[1] : node
					// tokens[2] : block
					BlockMap.put(tokens[0], tokens[1]);
				}
				blockCount = (long) 68;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {
		String line = value.toString();
		String[] words = line.split("\t");

		// TODO: need to move it to run once
		// blockMapper();

		// degree and pagerank
		Integer degree = 0;
		try {
			degree = new Integer(words[2]);
		} catch (ArrayIndexOutOfBoundsException e) {
			degree = 0;
		}
		Float pageRank = new Float(words[1]);

		// source node
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

			// map the block id for the source node
			Text BlockSource = new Text(BlockMap.get(source));

			// Metadata tuple
			context.write(BlockSource, edgeList);
			cleanup(context);

			Text nodeOut = new Text();
			Text tupleValue = new Text();
			for (int i = 3; i < words.length; i++) {
				// condition when both the nodes are in the same block - BE
				if (BlockMap.get(source).equalsIgnoreCase(
						BlockMap.get(words[i]))) {
					nodeOut = new Text(BlockMap.get(source));
					tupleValue = new Text(source + delimiter + words[i]);
				}
				// condition when both the nodes are in different blocks - BC
				else {
					nodeOut = new Text(BlockMap.get(words[i]));
					tupleValue = new Text(source + delimiter + words[i]
							+ delimiter + String.valueOf(pageRank));
				}
				context.write(nodeOut, tupleValue);

			}
			cleanup(context);
		} else {
			Text nodeOut = new Text(BlockMap.get(source));
			String output = source + delimiter + pageRank.toString()
					+ delimiter + degree.toString() + delimiter + dummyList;
			Text tupleValue = new Text(output);
			context.write(nodeOut, tupleValue);
			cleanup(context);
		}
		// set the value of the block count
		context.getCounter(PAGE_RANK_COUNTER.BLOCK_COUNT).setValue(blockCount);
	}
}
