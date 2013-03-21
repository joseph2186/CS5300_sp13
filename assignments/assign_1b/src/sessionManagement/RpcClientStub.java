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
import java.net.SocketTimeoutException;

public class RpcClientStub
{
	private DatagramPacket _sendPkt=null;
	private DatagramPacket _recvPkt=null;
	private String[] _ippList=null;
	private OperationCode _opCode=null;
	private String _data="";
	private int _callId=0;

	public RpcClientStub(OperationCode opCode,int callId,String[] ippList,String data)
	{
		_opCode=opCode;
		_callId=callId;
		_ippList=ippList;
		_data=data;
	}

	public String[] RpcClientStubHandler()
	{
		ByteArrayOutputStream bos=null;
		ByteArrayInputStream bis=null;
		ObjectOutput out=null;
		ObjectInput in=null;

		ServerSingleton.InBuf inBuf=null;
		byte[] outBufBytes=null;
		byte[] inBufBytes=null;
		String obj="";
		String[] tokens=null;
		DatagramSocket rpcSocket=null;

		try
		{
			rpcSocket=new DatagramSocket();
			// TODO: check the time out value
			rpcSocket.setSoTimeout(200);
			inBuf.setCallId(_callId);
			inBuf.setOpCode(_opCode);
			inBuf.setData(_data);

			out=new ObjectOutputStream(bos);
			out.writeObject(inBuf);
			outBufBytes=bos.toByteArray();

			// the ippList is populated as string pairs IP_Port
			for(int i=0;i<_ippList.length;i=i+2)
			{
				if(Util.isNullIPP(_ippList[i],_ippList[i+1]))
				{
					continue;
				}
				_sendPkt=new DatagramPacket(outBufBytes,outBufBytes.length,InetAddress.getByName(_ippList[i]),Integer.parseInt(_ippList[i+1]));
				rpcSocket.send(_sendPkt);
			}

			inBufBytes=new byte[512];
			_recvPkt=new DatagramPacket(inBufBytes,inBufBytes.length);
			do
			{
				_recvPkt.setLength(inBufBytes.length);
				try
				{
					rpcSocket.receive(_recvPkt);
				}
				catch(SocketTimeoutException e)
				{
					// TODO : we need to return null here but have
					// to change logic at places this is getting called
					e.printStackTrace();
					return null;
				}
				bis=new ByteArrayInputStream(inBufBytes);
				in=new ObjectInputStream(bis);
				try
				{
					obj=(String)in.readObject();
				}
				catch(ClassNotFoundException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				tokens=Util.tokenize(obj);

			}
			while(Integer.parseInt(tokens[0])!=_callId);
			out.close();
			in.close();
			bis.close();
			bos.close();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		rpcSocket.close();
		return tokens;
	}
}
