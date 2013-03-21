package sessionManagement;

import java.net.DatagramSocket;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSingleton {
	private static ServerSingleton _instance = null;

	// Maintain all the server datastructures here
	static Map<String,String> sessionInfoCMap;
	static Vector<String> mbrSet;

	// milliseconds
	public static final int CONST_LONG_DAEMON_RUN_FREQ = 50000;
	public static final String CONST_STR_COOKIE_NAME = "CS5300PROJ1SESSION";
	// seconds
	public static final int CONST_INT_SESSION_TIMEOUT_VAL = 60;
	public static final String CONST_STR_DEF_MSG_HELLO_USER = "Hello User!";
	public static final String CONST_STRING_SDF_FORMAT =
			"EEE MMM dd HH:mm:ss zzz yyyy";

	private static int _sessionNumber = 0;
	private static int _callId = 0;

	// time constants in msec
	public static int CONST_DELTA_TIMEOUT_VAL = 200;
	public static int CONST_GAMMA_TIMEOUT_VAL = 100;

	private static DatagramSocket rpcSocket = null;

	public class InBuf {
		OperationCode opCode;
		int callId;
		String data = null;

		public String getData(){
			return data;
		}

		public void setData(String data){
			this.data = data;
		}

		public OperationCode getOpCode(){
			return opCode;
		}

		public void setOpCode(OperationCode opCode){
			this.opCode = opCode;
		}

		public int getCallId(){
			return callId;
		}

		public void setCallId(int callId){
			this.callId = callId;
		}
	}

	public static DatagramSocket getRpcSocket(){
		return rpcSocket;
	}

	public static void setRpcSocket(DatagramSocket rpcSocket){
		ServerSingleton.rpcSocket = rpcSocket;
	}

	public static int get_callId(){
		return _callId;
	}

	public static void set_callId(int _callId){
		ServerSingleton._callId = _callId;
	}

	public static int getNextCallId(){
		_callId = _callId + 1;
		return _callId;
	}

	public static Map<String,String> getSessionInfo(){
		return sessionInfoCMap;
	}

	public static void setSessionInfo(Map<String,String> sessionInfo){
		ServerSingleton.sessionInfoCMap = sessionInfo;
	}

	public static Vector getMbrSet(){
		return mbrSet;
	}

	public static void setMbrSet(Vector mbrSet){
		ServerSingleton.mbrSet = mbrSet;
	}

	public static int get_sessionNumber(){
		return _sessionNumber;
	}

	public static void set_sessionNumber(int _sessionNumber){
		ServerSingleton._sessionNumber = _sessionNumber;
	}

	public synchronized int getNextSessionNumber(){
		_sessionNumber = _sessionNumber + 1;
		return _sessionNumber;
	}

	private ServerSingleton (){
		sessionInfoCMap = new ConcurrentHashMap<String,String>();
		mbrSet = new Vector<String>();
	}

	public static ServerSingleton getInstance(){
		if(_instance == null){
			_instance = new ServerSingleton();
		}
		return _instance;
	}

}
