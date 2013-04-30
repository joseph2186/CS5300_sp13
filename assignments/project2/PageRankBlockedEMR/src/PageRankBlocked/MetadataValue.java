package PageRankBlocked;
public class MetadataValue {
	public static final int CONST_INT_NODES_SEPARATOR = 4;
	private String nodeID;
	private String pageRank;
	private int outdegree;
	private String listOfNodes;

	public MetadataValue(String values)
	{
		String[] valuesMinusNode = values.split(PageRankReducerBlocked.DELIMITER, CONST_INT_NODES_SEPARATOR);
		this.nodeID = valuesMinusNode[0];
		this.pageRank = valuesMinusNode[1];
		this.outdegree = Integer.parseInt(valuesMinusNode[2]);
		this.listOfNodes = valuesMinusNode[3];
	}
	
	public String getNodeID() {
		return nodeID;
	}

	public String getPageRank() {
		return pageRank;
	}

	public int getOutdegree() {
		return outdegree;
	}

	public String getListOfNodes() {
		return listOfNodes;
	}

	public void setPageRank(String pageRank) {
		this.pageRank = "";
		this.pageRank = pageRank;
	}

	public void setPageRankFromDouble(double prDouble)
	{
		this.pageRank = "";
		this.pageRank = String.valueOf(prDouble);
	}
	
	public double getPageRankAsDouble()
	{
		return Double.parseDouble(this.pageRank);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getPageRank()).append(PageRankReducerBlocked.DELIMITER).append(getOutdegree())
				.append(PageRankReducerBlocked.DELIMITER).append(getListOfNodes());

		return sb.toString();
	}

	public double getResidue(double newPR) {
		return Math.abs((this.getPageRankAsDouble() - newPR)/newPR);
	}

}
