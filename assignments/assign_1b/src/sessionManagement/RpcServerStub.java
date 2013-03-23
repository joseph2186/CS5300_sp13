package sessionManagement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class RpcServerStub extends Thread {
	private static RpcServerStub _instance = null;
	private DatagramSocket _rpcSocket = null;
	private int _serverPort = 0;
	private ServerSingleton _serverInstance = null;

	private RpcServerStub (ServerSingleton serverInstance){
		try{
			_serverInstance = serverInstance;
			_rpcSocket = new DatagramSocket();
			_serverPort = _rpcSocket.getLocalPort();
		}catch(SocketException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int get_serverPort(){
		return _serverPort;
	}

	public static RpcServerStub getInstance(ServerSingleton serverInstance){
		if(_instance == null){
			_instance = new RpcServerStub(serverInstance);
		}
		return _instance;
	}

	public DatagramSocket get_rpcSocket(){
		return _rpcSocket;
	}

	public String SessionRead(String SID,int version){
		// get the session info from the session table
		ServerSingleton serverInstance = ServerSingleton.getInstance();
		String sessionInfo = serverInstance.sessionInfoCMap.get(SID);

		System.out.println("Inside session read SID = " + SID + ":"
				+ sessionInfo);
		// the check for sessionInfo being null should be done by the client
		return sessionInfo;
	}

	public String SessionWrite(String SID,int version,String sessionInfo,
			String discardTime){
		ServerSingleton serverInstance = ServerSingleton.getInstance();
		serverInstance.sessionInfoCMap.put(SID,sessionInfo);
		
		System.out.println("Inside session write SID = " + SID + ":"
				+ sessionInfo);

		return Util.ACK;

	}

	public String SessionDelete(String SID,int version){
		ServerSingleton serverInstance = ServerSingleton.getInstance();
		serverInstance.sessionInfoCMap.remove(SID);

		return Util.ACK;

	}

	public String GetMembers(int size){
		ServerSingleton serverInstance = ServerSingleton.getInstance();
		String output = "";
		try{
			for(int i = 0;i < size;i++ ){
				output += serverInstance.mbrSet.get(i);
			}
		}catch(ArrayIndexOutOfBoundsException e){
			output = Util.ACK;
		}

		return output;

	}

	@Override
	public void run(){
		ServerSingleton.InBuf inBuf = ServerSingleton.getInstance().new InBuf();
		ObjectOutput out = null;
		ObjectInput in = null;
		ByteArrayOutputStream bos = null;
		ByteArrayInputStream bis = null;
		InetAddress returnAddress = null;
		int returnPort = 0;
		OperationCode opCode = null;
		int callId = 0;
		String data = null;
		String[] tokens = null;
		String output = null;
		String sessionId = "";
		while(true){
			try{
				byte[] inBufBytes = new byte[512];
				DatagramPacket recvPkt =
						new DatagramPacket(inBufBytes,inBufBytes.length);

				_rpcSocket.receive(recvPkt);

				returnAddress = recvPkt.getAddress();
				returnPort = recvPkt.getPort();

				// converting byte[] to object type inBuf
				bis = new ByteArrayInputStream(inBufBytes);
				in = new ObjectInputStream(bis);
				Object obj = in.readObject();
				inBuf = (ServerSingleton.InBuf)obj;

				// check for the opcode and the call id
				opCode = inBuf.getOpCode();
				callId = inBuf.getCallId();
				data = inBuf.getData();
				tokens = Util.tokenize(data);
				
				
				in.close();
				bis.close();

				switch(opCode){
					case SESSIONREAD:
						sessionId =
								tokens[0] + Util.DELIM + tokens[1] + Util.DELIM
										+ tokens[2];
						output =
								SessionRead(sessionId,
										Integer.parseInt(tokens[3]));
						break;
					case SESSIONWRITE:
						sessionId =
								tokens[0] + Util.DELIM + tokens[1] + Util.DELIM
										+ tokens[2];
						String sessionInfo =
								tokens[Util.VERSION_ID + 3] + Util.DELIM
										+ tokens[Util.MESSAGE + 3] + Util.DELIM
										+ tokens[Util.EXPIRATION_TIME + 3]
										+ Util.DELIM
										+ tokens[Util.DISCARD_TIME + 3];

						output =
								SessionWrite(sessionId,
										Integer.parseInt(tokens[3]),
										sessionInfo,
										tokens[Util.DISCARD_TIME + 3]);
						break;
					case SESSIONDELETE:
						sessionId =
								tokens[0] + Util.DELIM + tokens[1] + Util.DELIM
										+ tokens[2];
						output =
								SessionDelete(sessionId,
										Integer.parseInt(tokens[3]));
						break;
					case GETMEMBER:
						output = GetMembers(Integer.parseInt(tokens[0]));
						break;
				}
				
				//inBuf = ServerSingleton.getInstance()
				inBuf.setData(output);
				inBuf.setCallId(callId);
				inBuf.setOpCode(opCode);
				
				System.out.println("server stub after callid ="+inBuf.getCallId());
				bos = new ByteArrayOutputStream();
				out = new ObjectOutputStream(bos);
				out.writeObject(inBuf);
				byte[] outBufBytes = bos.toByteArray();
				DatagramPacket _sendPkt =
						new DatagramPacket(outBufBytes,outBufBytes.length,
								returnAddress,returnPort);
				_rpcSocket.send(_sendPkt);
				
				outBufBytes = null;				
				inBuf = null;
				
				out.close();
				bos.close();

			}catch(IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch(ClassNotFoundException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
