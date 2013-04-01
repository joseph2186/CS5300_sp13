package sessionManagement;

import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSingleton implements Serializable {
	private static ServerSingleton _instance = null;

	// Maintain all the server data structures here
	static Map<String,String> sessionInfoCMap;
	static Vector<String> mbrSet;

	// milliseconds
	public static final int CONST_LONG_DAEMON_RUN_FREQ = 50000;
	public static final String CONST_STR_COOKIE_NAME = "CS5300PROJ1SESSION";
	// seconds
	public static final int CONST_INT_SESSION_TIMEOUT_VAL = 60;
	public static final int CONST_SOCKET_TIMEOUT_VAL = 10000;
	public static final String CONST_STR_DEF_MSG_HELLO_USER = "Hello User!";
	public static final String CONST_STRING_SDF_FORMAT =
			"EEE MMM dd HH:mm:ss zzz yyyy";

	// the timeout for the crash is set to 10 minutes
	public static final int CONST_CRASH_TIME_MS = 1000 * 60 * 10;

	private static int _sessionNumber = 0;
	private static int _callId = 0;

	// time constants in msec
	public static int CONST_DELTA_TIMEOUT_VAL = 200;
	public static int CONST_GAMMA_TIMEOUT_VAL = 100;

	public static boolean CRASH_FLAG = true;

	private static DatagramSocket rpcSocket = null;
	public static String ipReceived = "";
	public static boolean IPP_FLAG = false;

	public class InBuf implements Serializable {
		OperationCode opCode;
		int callId;
		String data = "";
		String senderRpcINetAddress;

		public String getSenderRpcINetAddress(){
			return senderRpcINetAddress;
		}

		public void setSenderRpcINetAddress(String senderRpcINetAddress){
			this.senderRpcINetAddress = senderRpcINetAddress;
		}

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

	public synchronized static DatagramSocket getRpcSocket(){
		return rpcSocket;
	}

	public synchronized static void setRpcSocket(DatagramSocket rpcSocket){
		ServerSingleton.rpcSocket = rpcSocket;
	}

	public synchronized static int get_callId(){
		return _callId;
	}

	public synchronized static void set_callId(int _callId){
		ServerSingleton._callId = _callId;
	}

	public synchronized static int getNextCallId(){
		_callId = _callId + 1;
		return _callId;
	}

	public static Map<String,String> getSessionInfo(){
		return sessionInfoCMap;
	}

	public synchronized static void setSessionInfo(Map<String,String> sessionInfo){
		ServerSingleton.sessionInfoCMap = sessionInfo;
	}

	public static Vector<String> getMbrSet(){
		return mbrSet;
	}

	public synchronized static void setMbrSet(Vector<String> mbrSet){
		ServerSingleton.mbrSet = mbrSet;
	}

	public synchronized static int get_sessionNumber(){
		return _sessionNumber;
	}

	public synchronized static void set_sessionNumber(int _sessionNumber){
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
