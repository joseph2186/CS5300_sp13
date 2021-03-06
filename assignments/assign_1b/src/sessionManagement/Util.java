package sessionManagement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.Cookie;

public class Util {
	public static String DELIM = "_";
	// Cookie Information
	public static int SESSION_NO = 0;
	public static int IP_CREATOR = 1;
	public static int PORT_CREATOR = 2;
	public static int VERSION = 3;
	public static int IP_PRIMARY = 4;
	public static int PORT_PRIMARY = 5;
	public static int IP_BACKUP = 6;
	public static int PORT_BACKUP = 7;

	// SSTable Entries
	public static int VERSION_ID = 0;
	public static int MESSAGE = 1;
	public static int EXPIRATION_TIME = 2;
	public static int DISCARD_TIME = 3;

	public static String NACK = "NACK";
	public static String ACK = "ACK";
	public static String NULL_IP = "0.0.0.0";
	public static String NULL_PORT = "0";

	public static String[] tokenize(String str){
		if(str == null)
			return null;
		String[] token = str.split(DELIM);
		return token;
	}

	public static String combine(String[] str){
		String ret = "";
		int i = 0;

		for(i = 0;i < str.length - 1;i++ ){
			ret += str[i] + DELIM;
		}
		ret += str[i];

		return ret;
	}

	public static String[] getIppList(Cookie cookie){
		String[] ippList = new String[4];
		String[] cookieValue = Util.tokenize(cookie.getValue());
		ippList[0] = cookieValue[Util.IP_PRIMARY];
		ippList[1] = cookieValue[Util.PORT_PRIMARY];
		ippList[2] = cookieValue[Util.IP_BACKUP];
		ippList[3] = cookieValue[Util.PORT_BACKUP];

		return ippList;
	}

	public static boolean isIppMine(Cookie cookie,String myIpp){
		String[] cookieValue = Util.tokenize(cookie.getValue());
		String[] ippList = Util.tokenize(myIpp);
		if((cookieValue[Util.IP_PRIMARY].equalsIgnoreCase(ippList[0])
				&& (cookieValue[Util.PORT_PRIMARY].equalsIgnoreCase(ippList[1])) || (cookieValue[Util.IP_BACKUP]
				.equalsIgnoreCase(ippList[0]))
				&& (cookieValue[Util.PORT_BACKUP].equalsIgnoreCase(ippList[1]))))
			return true;
		else
			return false;

	}

	public static String getPrimaryIpp(Cookie cookie){
		String[] cookieValue = Util.tokenize(cookie.getValue());
		return cookieValue[Util.IP_PRIMARY] + DELIM
				+ cookieValue[Util.PORT_PRIMARY];
	}

	public static String getBackupIpp(Cookie cookie){
		String[] cookieValue = Util.tokenize(cookie.getValue());
		return cookieValue[Util.IP_BACKUP] + DELIM
				+ cookieValue[Util.PORT_BACKUP];
	}

	public static String getSessionId(Cookie cookie){
		String[] cookieValue = Util.tokenize(cookie.getValue());
		return cookieValue[Util.SESSION_NO] + DELIM
				+ cookieValue[Util.IP_CREATOR] + DELIM
				+ cookieValue[Util.PORT_CREATOR];
	}

	public static String getVersionNumber(Cookie cookie){
		String[] CookieValue = Util.tokenize(cookie.getValue());
		return CookieValue[Util.VERSION];
	}

	public static boolean isNullIPP(String ip,String port){
		if(ip.equalsIgnoreCase(NULL_IP) && port.equalsIgnoreCase(NULL_PORT)){
			return true;
		}else{
			return false;
		}
	}

	public static void updateIppBackup(Cookie cookie,String[] ippBackup){
		String[] cookieValue = Util.tokenize(cookie.getValue());
		cookieValue[Util.IP_BACKUP] = ippBackup[0];
		cookieValue[Util.PORT_BACKUP] = ippBackup[1];
		cookie.setValue(combine(cookieValue));
	}

	public static void updateIppPrimary(Cookie cookie,String[] ippPrimary){
		String[] cookieValue = Util.tokenize(cookie.getValue());
		cookieValue[Util.IP_PRIMARY] = ippPrimary[0];
		cookieValue[Util.PORT_PRIMARY] = ippPrimary[1];
		cookie.setValue(combine(cookieValue));
	}

	public static boolean isNullIPP(String primaryIpp){
		String[] ippList = tokenize(primaryIpp);
		return isNullIPP(ippList[0],ippList[1]);
	}

	public static void incrementVersionInCookie(Cookie cookie){
		String[] cookieValue = Util.tokenize(cookie.getValue());
		cookieValue[Util.VERSION] =
				String.valueOf(Integer.parseInt(cookieValue[Util.VERSION]) + 1);
		cookie.setValue(combine(cookieValue));
	}

	// method used to display the instance id - used while testing to know which
	// server has crashed
	public static String retrieveInstanceId(){
		String EC2Id = "";
		String inputLine;
		try{
			URL EC2MetaData =
					new URL(
							"http://169.254.169.254/latest/meta-data/instance-id");
			URLConnection EC2MD = EC2MetaData.openConnection();
			BufferedReader in =
					new BufferedReader(new InputStreamReader(
							EC2MD.getInputStream()));
			while((inputLine = in.readLine()) != null){
				EC2Id = inputLine;
			}
			in.close();
		}catch(Exception ex){
			return "exception";
		}
		return EC2Id;
	}
}
