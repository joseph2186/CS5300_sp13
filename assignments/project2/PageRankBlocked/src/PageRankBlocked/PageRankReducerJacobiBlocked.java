package PageRankBlocked;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class PageRankReducerJacobiBlocked extends Reducer<Text, Text, Text, Text> {

	public static final int CONST_INT_NO_OF_ITERATIONS = 20;
	public static final double CONST_TOTAL_NODES = 685230.0;
	public static final double CONST_DOUBLE_DAMPING_FACTOR = 0.85;
	public static final String DELIMITER = "\t";
	public static final Double CONST_DOUBLE_THRESHOLD = 0.001;
	HashMap<Integer, ArrayList<Integer>> BEMap = null;
	HashMap<Integer, ArrayList<Double>> bcDataMap = null;
	HashMap<Integer, Double> newPrMapPerIteration = null;
	HashMap<Integer, MetadataValue> mdMap = null;
	HashMap<Integer, Double> initialPassPRMap = null;

	@Override
	protected void reduce(Text key, Iterable<Text> values, Context context)
			throws IOException, InterruptedException {
		BEMap = new HashMap<Integer, ArrayList<Integer>>();
		bcDataMap = new HashMap<Integer, ArrayList<Double>>();
		newPrMapPerIteration = new HashMap<Integer, Double>();
		mdMap = new HashMap<Integer, MetadataValue>();
		initialPassPRMap = new HashMap<Integer, Double>();

		MetadataValue mDV = null;
		Iterator<Text> iterator = values.iterator();

		while (iterator.hasNext()) {
			Text line = (Text) iterator.next();
			String lineStr = line.toString();
			String[] lineTokens = lineStr.split(DELIMITER);

			if (lineTokens.length == 2) {
				// BE inputs
				Integer v = Integer.parseInt(lineTokens[1]);
				if (!BEMap.containsKey(v)) {
					ArrayList<Integer> list = new ArrayList<Integer>();
					list.add(Integer.parseInt(lineTokens[0]));
					BEMap.put(v, list);
				} else {
					BEMap.get(v).add(Integer.parseInt(lineTokens[0]));
				}
			} else if (lineTokens.length == 3) {
				// BC inputs
				Integer v = Integer.parseInt(lineTokens[1]);
				double R = Double.parseDouble(lineTokens[2]);
				if (!bcDataMap.containsKey(v)) {
					ArrayList<Double> list = new ArrayList<Double>();
					list.add(R);
					bcDataMap.put(v, list);
				} else {
					bcDataMap.get(v).add(R);
				}
			} else {
				Integer v = Integer.parseInt(lineTokens[0]);
				initialPassPRMap.put(v, Double.parseDouble(lineTokens[1]));
				mDV = new MetadataValue(lineStr);
				mdMap.put(v, mDV);
			}
		}// end of while

		// Fix for border nodes with out degree 0
		for (Integer v : bcDataMap.keySet()) {
			if (!mdMap.containsKey(v)) {
				Double defaultPR = (double) (1d - CONST_DOUBLE_DAMPING_FACTOR)
						/ CONST_TOTAL_NODES;
				initialPassPRMap.put(v, defaultPR);
				StringBuilder strBuilder = new StringBuilder();
				strBuilder.append(v).append(DELIMITER);
				strBuilder.append(defaultPR.toString()).append(DELIMITER);
				strBuilder.append("0").append(DELIMITER);
				strBuilder.append("-1").append(DELIMITER);
				MetadataValue mdV = new MetadataValue(strBuilder.toString());
				mdMap.put(v, mdV);
			}
		}

		// Calculate new PR values
		// Logic!!?
		double residual = 1;
		int counter = 0;

		while (Double.compare(residual, CONST_DOUBLE_THRESHOLD) > 0) {
			// && counter < CONST_INT_NO_OF_ITERATIONS) {
			residual = iterateBlockOnce();
			++counter;
			residual = (double) (residual / (double) initialPassPRMap.size());
		}

		// Calculate block level residual
		residual = 0d;
		for (Entry entry : initialPassPRMap.entrySet()) {
			Integer v = (Integer) entry.getKey();
			Double oldPr = (Double) entry.getValue();
			residual += calculateResidual(oldPr, mdMap.get(v)
					.getPageRankAsDouble());
		}

		Long nodeCount = (long) mdMap.size();
		context.getCounter(PAGE_RANK_COUNTER.NODE_COUNT).increment(
				(long) nodeCount);

		Double residualBlock = (double) (residual / (double) nodeCount);
		Long residualLong = (long) (residual * 10000);
		context.getCounter(PAGE_RANK_COUNTER.RESIDUAL).increment(
				(long) residualLong);

		context.getCounter(PAGE_RANK_COUNTER.BLOCK_ITERATION).increment(
				(long) counter);

		String output = "";
		Text outputTextVal = null;
		Text outputTextKey = null;

		for (MetadataValue mdv : mdMap.values()) {
			output = mdv.toString();
			outputTextVal = new Text(output);
			outputTextKey = new Text(mdv.getNodeID());
			context.write(outputTextKey, outputTextVal);
		}
		cleanup(context);
	}

	private double iterateBlockOnce() {
		double residual = 0d;
		newPrMapPerIteration = new HashMap<Integer, Double>();

		for (Integer v : initialPassPRMap.keySet()) {
			newPrMapPerIteration.put(v, 0d);

			// nodes for BE
			ArrayList<Integer> list = BEMap.get(v);
			// bug fix - If V is a source
			if (list != null) {
				for (Integer u : list) {
					int outDegree = mdMap.get(u).getOutdegree();
					if (outDegree != 0) {
						double prU = mdMap.get(u).getPageRankAsDouble();
						/*if (newPrMapPerIteration.containsKey(u)) {
							prU = newPrMapPerIteration.get(u);
						}*/
						double newPR = prU / (double) outDegree;
						newPR += newPrMapPerIteration.get(v);
						newPrMapPerIteration.remove(v);
						newPrMapPerIteration.put(v, newPR);
					}
				}
			}// end of for

			// nodes for BC
			ArrayList<Double> bcdList = bcDataMap.get(v);
			// bug fix - if V is a source
			if (bcdList != null) {
				for (double r : bcdList) {
					double newPR = r;
					newPR += newPrMapPerIteration.get(v);
					newPrMapPerIteration.remove(v);
					newPrMapPerIteration.put(v, newPR);
				}
			}

			double newPR = (CONST_DOUBLE_DAMPING_FACTOR * newPrMapPerIteration
					.get(v))
					+ ((1d - CONST_DOUBLE_DAMPING_FACTOR) / CONST_TOTAL_NODES);
			newPrMapPerIteration.remove(v);
			newPrMapPerIteration.put(v, newPR);

			// Perform
			MetadataValue mdValueV = mdMap.get(v);

			residual += mdValueV.getResidue(newPR);

			// we do it here since we need the old value in the above
			// computation
			//mdValueV.setPageRankFromDouble(newPR);
		}
		for(Integer v : mdMap.keySet())
		{
			mdMap.get(v).setPageRankFromDouble(newPrMapPerIteration.get(v));
		}
		return residual;
	}

	private double calculateResidual(double oldPr, double newPr) {
		return Math.abs(oldPr - newPr) / newPr;
	}
}
