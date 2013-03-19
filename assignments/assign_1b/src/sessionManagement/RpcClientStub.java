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

public class RpcClientStub extends Thread
{
	private DatagramPacket _sendPkt=null;
	private DatagramPacket _recvPkt=null;
	private String[] _ippList=null;
	private OperationCode _opCode=null;
	private String _data="";
	private int _callId=0;
	private DatagramSocket _rpcSocket=null;

	public RpcClientStub(OperationCode opCode,int callId,String ippList,String data,DatagramSocket rpcSocket)
	{
		_opCode=opCode;
		_callId=callId;
		_ippList=UtilityMethods.tokenize(ippList);
		_data=data;
		_rpcSocket=rpcSocket;
	}

	@Override
	public void run()
	{
		ByteArrayOutputStream bos=null;
		ByteArrayInputStream bis=null;
		ObjectOutput out=null;
		ObjectInput in=null;

		ServerSingleton.InBuf inBuf=null;
		byte[] outBufBytes=null;
		byte[] inBufBytes = null;
		String obj = "";
		String[] tokens = null;

		try
		{
			inBuf.setCallId(_callId);
			inBuf.setOpCode(_opCode);
			inBuf.setData(_data);
			out=new ObjectOutputStream(bos);
			out.writeObject(inBuf);
			outBufBytes=bos.toByteArray();

			for(int i=0;i<_ippList.length;i=i+2)
			{
				_sendPkt=new DatagramPacket(outBufBytes,outBufBytes.length,InetAddress.getByName(_ippList[i]),Integer.parseInt(_ippList[i+1]));
				_rpcSocket.send(_sendPkt);
			}
			
			inBufBytes = new byte[512];
			_recvPkt = new DatagramPacket(inBufBytes,inBufBytes.length);
			do
			{
				_recvPkt.setLength(inBufBytes.length);
				_rpcSocket.receive(_recvPkt);
				bis = new ByteArrayInputStream(inBufBytes);
				in = new ObjectInputStream(bis);
				try
				{
					obj = (String)in.readObject();
				}
				catch(ClassNotFoundException e)
				{
					//TODO Auto-generated catch block
					e.printStackTrace();
				}
				tokens = UtilityMethods.tokenize(obj);
				
			} while(Integer.parseInt(tokens[0]) != _callId);
			
			switch(_opCode)
			{
				case SESSIONREAD:
					break;
				case SESSIONWRITE:
					break;
				case SESSIONDELETE:
					break;
				case GETMEMBER:
					break;
			}
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
