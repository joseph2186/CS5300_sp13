package PageRankBlocked;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class PageRankReducerBlocked extends Reducer<Text, Text, Text, Text> {

	public static final int CONST_INT_NO_OF_ITERATIONS = 6;
	public static final double CONST_TOTAL_NODES = 685229.0;
	public static final double CONST_DOUBLE_DAMPING_FACTOR = 0.85;
	public static final String DELIMITER = "\t";
	public static final Double CONST_DOUBLE_THRESHOLD = 0.001;

	HashMap<String, ArrayList<String>> BEMap = new HashMap<String, ArrayList<String>>();
	HashMap<String, ArrayList<BCData>> bcDataMap = new HashMap<String, ArrayList<BCData>>();
	HashMap<String, Double> nprMap = new HashMap<String, Double>();
	HashMap<String, MetadataValue> mdMap = new HashMap<String, MetadataValue>();
	HashSet<String> setOfNodes = new HashSet<String>();

	@Override
	protected void reduce(Text key, Iterable<Text> values, Context context)
			throws IOException, InterruptedException {

		MetadataValue mDV = null;
		BCData bcData = null;
		Iterator<Text> iterator = values.iterator();

		while (iterator.hasNext()) {
			Text line = (Text) iterator.next();
			String[] lineTokens = line.toString().split(DELIMITER);

			if (lineTokens.length == 2) {
				// BE inputs

				if (!BEMap.containsKey(lineTokens[1])) {
					ArrayList<String> list = new ArrayList<String>();
					BEMap.put(lineTokens[1], list);
				}
				BEMap.get(lineTokens[1]).add(lineTokens[0]);
			} else if (lineTokens.length == 3) {
				bcData = new BCData(line.toString());
				if (!bcDataMap.containsKey(lineTokens[1])) {
					ArrayList<BCData> list = new ArrayList<BCData>();
					bcDataMap.put(lineTokens[1], list);
				}
				bcDataMap.get(lineTokens[1]).add(bcData);
			} else {
				setOfNodes.add(lineTokens[0]);
				mDV = new MetadataValue(line.toString());
				mdMap.put(lineTokens[0], mDV);
			}
		}

		// Calculate new PR values
		// Logic!!?
		double residual = 0;
		int counter = 0;
		do {
			residual = iterateBlockOnce();
			++counter;
			residual = (double)(residual / (double)setOfNodes.size());
//			System.out.println("residual after division-->"+residual);
		} while (Double.compare(residual, CONST_DOUBLE_THRESHOLD) > 0
				&& counter < CONST_INT_NO_OF_ITERATIONS);
		Long residualLong = (long) (residual * 10000);
		context.getCounter(PAGE_RANK_COUNTER.RESIDUAL).increment(residualLong);

		String output = "";
		Text outputTextVal = null;
		Text outputTextKey = null;

		for (MetadataValue mdv : mdMap.values()) {
			output = mdv.toString();
//			System.out.println("key-->"+mdv.getNodeID());
//			System.out.println("output-->"+output);
			outputTextVal = new Text(output);
			outputTextKey = new Text(mdv.getNodeID());
			context.write(outputTextKey, outputTextVal);
		}

		cleanup(context);
	}

	private double iterateBlockOnce() {
		double residual = 0d;
		nprMap = new HashMap<String, Double>();
		for (String v : setOfNodes) {
			nprMap.put(v, 0d);
			ArrayList<String> list = BEMap.get(v);
			//bug fix
			if (list != null) {
				for (String u : BEMap.get(v)) {
					double newPR = mdMap.get(u).getPageRankAsDouble()
							/ (double) mdMap.get(u).getOutdegree();
					newPR += nprMap.get(v);
					nprMap.put(v, newPR);
				}
			}

			//bug fix
			ArrayList<BCData> bcdList = bcDataMap.get(v);
			if (bcdList != null) {
				for (BCData bcd : bcDataMap.get(v)) {
					double newPR = bcd.getR();
					newPR += nprMap.get(v);
					nprMap.put(v, newPR);
				}
			}
			double newPR = CONST_DOUBLE_DAMPING_FACTOR * nprMap.get(v)
					+ (1d - CONST_DOUBLE_DAMPING_FACTOR) / CONST_TOTAL_NODES;
			nprMap.put(v, newPR);

			residual += mdMap.get(v).getResidue(newPR);

			// we do it here since we need the old value in the above
			// computation
			mdMap.get(v).setPageRankFromDouble(newPR);
		}
//		System.out.println("residual->"+residual);
		return residual;
	}
}
