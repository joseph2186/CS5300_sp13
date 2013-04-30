package PageRankBlocked;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class PageRankReducerBlocked extends Reducer<Text, Text, Text, Text> {

    public static final int CONST_INT_NO_OF_ITERATIONS = 6;
    public static final double CONST_TOTAL_NODES = 685229.0;
    public static final double CONST_DOUBLE_DAMPING_FACTOR = 0.85;
    public static final String DELIMITER = "\t";
    public static final Double CONST_DOUBLE_THRESHOLD = 0.001;
    HashMap<Integer, ArrayList<Integer>> BEMap = new HashMap<Integer, ArrayList<Integer>>();
    HashMap<Integer, ArrayList<Double>> bcDataMap = new HashMap<Integer, ArrayList<Double>>();
    HashMap<Integer, Double> nprMap = new HashMap<Integer, Double>();
    HashMap<Integer, MetadataValue> mdMap = new HashMap<Integer, MetadataValue>();
    HashMap<Integer, Double> nodesPrMap = new HashMap<Integer, Double>();
//    HashSet<Integer> setOfNodes = new HashSet<Integer>();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        MetadataValue mDV = null;
        BCData bcData = null;
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
//				BEMap.get(lineTokens[1]).add(Integer.parseInt(lineTokens[0]));
            } else if (lineTokens.length == 3) {
//                bcData = new BCData(lineStr);
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
//                setOfNodes.add(v);
                nodesPrMap.put(v, Double.parseDouble(lineTokens[1]));
                mDV = new MetadataValue(lineStr);
                mdMap.put(v, mDV);
            }
        }

        // Calculate new PR values
        // Logic!!?
        double residual = 0;
        int counter = 0;

        //First iteration
        residual = iterateBlockOnce(context);
//        residual = (double) (residual / (double) setOfNodes.size());
        residual = (double) (residual / (double) nodesPrMap.size());
        while (Double.compare(residual, CONST_DOUBLE_THRESHOLD) > 0
                && counter < CONST_INT_NO_OF_ITERATIONS) {
            residual = iterateBlockOnce(null);
            ++counter;
            residual = (double) (residual / (double) nodesPrMap.size());
//			System.out.println("residual after division-->"+residual);
        }


        //Calculate block level residual
        residual = 0d;
        for (Entry entry : nodesPrMap.entrySet()) {
            Integer v = (Integer) entry.getKey();
            Double oldPr = (Double) entry.getValue();
            residual += calculateResidual(oldPr, mdMap.get(v).getPageRankAsDouble());
        }
        residual = residual / ((double) nodesPrMap.size());

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

    private double iterateBlockOnce(Context context) {
        double residual = 0d;
        nprMap = new HashMap<Integer, Double>();
        for (Integer v : nodesPrMap.keySet()) {//setOfNodes) {
            nprMap.put(v, 0d);
            ArrayList<Integer> list = BEMap.get(v);
            //bug fix
            if (list != null) {
                for (Integer u : list) {
                    int outDegree = mdMap.get(u).getOutdegree();
                    if (outDegree != 0) {
                        double newPR = mdMap.get(u).getPageRankAsDouble()
                                / (double) outDegree;
                        newPR += nprMap.get(v);
                        nprMap.put(v, newPR);
                    }
                }
            }

            //bug fix
            ArrayList<Double> bcdList = bcDataMap.get(v);
            if (bcdList != null) {
                for (double r : bcdList) {
                    double newPR = r;//bcd.getR();
                    newPR += nprMap.get(v);
                    nprMap.put(v, newPR);
                }
            }
            double newPR = CONST_DOUBLE_DAMPING_FACTOR * nprMap.get(v)
                    + (1d - CONST_DOUBLE_DAMPING_FACTOR) / CONST_TOTAL_NODES;
            nprMap.put(v, newPR);

            //Perform
            MetadataValue mdValueV = mdMap.get(v);

            residual += mdValueV.getResidue(newPR);

//            if (null != context && bcDataMap.containsKey(v)) {
//                Long residualLong = (long) (residual * 10000);
//                context.getCounter(PAGE_RANK_COUNTER.BLOCK_EDGE_CONVERGENCE).increment(residualLong);
//                context.getCounter(PAGE_RANK_COUNTER.EDGE_NODES_COUNT).increment(residualLong);
//            }

            // we do it here since we need the old value in the above
            // computation
            mdValueV.setPageRankFromDouble(newPR);
        }
//		System.out.println("residual->"+residual);
        return residual;
    }

    private double calculateResidual(double oldPr, double newPr) {
        return Math.abs(oldPr - newPr) / newPr;
    }
}
