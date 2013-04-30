
public class MetadataValue {

    public static final int CONST_INT_NODES_SEPARATOR = 4;
    private int nodeID;
    private double pageRank;
    private int outdegree;
    private String listOfNodes;

    public MetadataValue(String values) {
        String[] valuesMinusNode = values.split(PageRankReducerBlocked.DELIMITER, CONST_INT_NODES_SEPARATOR);
        this.nodeID = Integer.parseInt(valuesMinusNode[0]);
        this.pageRank = Double.parseDouble(valuesMinusNode[1]);
        this.outdegree = Integer.parseInt(valuesMinusNode[2]);
        this.listOfNodes = valuesMinusNode[3];
    }

    public String getNodeID() {
        return String.valueOf(nodeID);
    }

    public String getPageRank() {
        return String.valueOf(pageRank);
    }

    public int getOutdegree() {
        return outdegree;
    }

    public String getListOfNodes() {
        return listOfNodes;
    }

//	public void setPageRank(String pageRank) {
//		this.pageRank = pageRank;
//	}
    public void setPageRankFromDouble(double prDouble) {
        this.pageRank = prDouble;
    }

    public double getPageRankAsDouble() {
        return this.pageRank;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(pageRank));
        sb.append(PageRankReducerBlocked.DELIMITER);
        sb.append(outdegree);
        sb.append(PageRankReducerBlocked.DELIMITER);
        sb.append(getListOfNodes());
        return sb.toString();
    }

    public double getResidue(double newPR) {
        return Math.abs((this.getPageRankAsDouble() - newPR) / newPR);
    }
}
