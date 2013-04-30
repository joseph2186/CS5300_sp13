package PageRankBlocked;

public class BCData {

    private double R;
    private int fromNode;

    public BCData(String bcDataValue) {
        String[] values = bcDataValue.split(PageRankReducerBlocked.DELIMITER);
        this.R = Double.parseDouble(values[2]);
        this.fromNode = Integer.parseInt(values[0]);
    }

//    public double getR() {
//        return R;
//    }
//
//    public int getFromNode() {
//        return fromNode;
//    }
}
