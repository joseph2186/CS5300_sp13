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
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

public class RpcClientStub {
	private DatagramPacket _sendPkt = null;
	private DatagramPacket _recvPkt = null;
	private String[] _ippList = null;
	private OperationCode _opCode = null;
	private String _data = "";
	private int _callId = 0;
	private String _senderRpcServerInet = "";
	private InetAddress _ipReceived = null;
	private int _portReceived = 0;

	public int get_portReceived(){
		return _portReceived;
	}

	public InetAddress get_ipReceived(){
		return _ipReceived;
	}

	public RpcClientStub (OperationCode opCode, int callId, String[] ippList,
			String data, String senderRpcServerInet){
		_opCode = opCode;
		_callId = callId;
		_ippList = ippList;
		_data = data;
		_senderRpcServerInet = senderRpcServerInet;
	}

	public String[] RpcClientStubHandler(){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ByteArrayInputStream bis = null;
		ObjectOutput out = null;
		ObjectInput in = null;

		ServerSingleton.InBuf inBuf = ServerSingleton.getInstance().new InBuf();
		byte[] outBufBytes = null;
		byte[] inBufBytes = null;
		ServerSingleton.InBuf obj = null;
		String[] tokens = null;
		DatagramSocket rpcSocket = null;
		ServerSingleton serverInstance = ServerSingleton.getInstance();

		try{
			rpcSocket = new DatagramSocket();
			// TODO: check the time out value
			rpcSocket.setSoTimeout(serverInstance.CONST_SOCKET_TIMEOUT_VAL);
			inBuf.setCallId(_callId);
			inBuf.setOpCode(_opCode);
			inBuf.setData(_data);
			inBuf.setSenderRpcINetAddress(_senderRpcServerInet);

			out = new ObjectOutputStream(bos);
			out.writeObject(inBuf);
			outBufBytes = bos.toByteArray();

			// the ippList is populated as string pairs IP_Port
			for(int i = 0;i < _ippList.length;i = i + 2){
				if(Util.isNullIPP(_ippList[i],_ippList[i + 1])){
					continue;
				}
				_sendPkt =
						new DatagramPacket(outBufBytes,outBufBytes.length,
								InetAddress.getByName(_ippList[i]),
								Integer.parseInt(_ippList[i + 1]));
				rpcSocket.send(_sendPkt);
			}

			inBufBytes = new byte[512];
			_recvPkt = new DatagramPacket(inBufBytes,inBufBytes.length);
			do{
				_recvPkt.setLength(inBufBytes.length);
				try{
					rpcSocket.receive(_recvPkt);
					_ipReceived = _recvPkt.getAddress();
					_portReceived = _recvPkt.getPort();
				}catch(SocketTimeoutException e){
					e.printStackTrace();
					return null;
				}
				bis = new ByteArrayInputStream(inBufBytes);
				in = new ObjectInputStream(bis);
				try{
					obj = ((ServerSingleton.InBuf)in.readObject());
				}catch(ClassNotFoundException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				tokens = Util.tokenize(obj.getData());

			}while(obj.getCallId() != _callId);
			if(out != null)
				out.close();
			if(in != null)
				in.close();
			if(bis != null)
				bis.close();
			if(bos != null)
				bos.close();
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			rpcSocket.close();
		}
		return tokens;
	}
}
