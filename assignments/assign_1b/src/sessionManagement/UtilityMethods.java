package sessionManagement;

public class UtilityMethods
{
	public static String delim = "[_]";
	
	public static String[] tokenize(String str)
	{
		if (str == null)
			return null;
		String[] token = str.split(delim);
		return token;
	}
	

	public static String combine(String[] str)
	{
		String ret = "";
		
		for(int i = 0 ; i < str.length ; i++)
		{
			ret += str[i]+"_";
		}
		
		return ret;
	}
}
