package PageRankBlocked;

public class BCData {
	private double R;
	private String fromNode;
	
	public BCData(String bcDataValue)
	{
		this.R = Double.parseDouble(bcDataValue.split(PageRankReducerBlocked.DELIMITER)[2]);
		this.fromNode = bcDataValue.split(PageRankReducerBlocked.DELIMITER)[0];
	}
	
	public double getR() {
		return R;
	}
	public String getFromNode() {
		return fromNode;
	}
}
