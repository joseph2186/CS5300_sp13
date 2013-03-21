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

public class RpcServerStub extends Thread
{
	private static RpcServerStub _instance=null;
	private DatagramSocket _rpcSocket=null;
	private int _serverPort=0;
	private ServerSingleton _serverInstance=null;

	private RpcServerStub(ServerSingleton serverInstance)
	{
		try
		{
			_serverInstance=serverInstance;
			_rpcSocket=new DatagramSocket();
			_serverPort=_rpcSocket.getLocalPort();
		}
		catch(SocketException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static RpcServerStub getInstance(ServerSingleton serverInstance)
	{
		if(_instance==null)
		{
			_instance=new RpcServerStub(serverInstance);
		}
		return _instance;
	}

	public DatagramSocket get_rpcSocket()
	{
		return _rpcSocket;
	}

	public String SessionRead(String SID,int version)
	{
		String output=null;

		return output;
	}

	public String SessionWrite(String SID,int version,String data,String discardTime)
	{
		String output=null;

		return output;

	}

	public String SessionDelete(String SID,int version)
	{
		String output=null;

		return output;

	}

	public String GetMembers(int size)
	{
		String output=null;

		return output;

	}

	@Override
	public void run()
	{
		while(true)
		{
			ServerSingleton.InBuf inBuf=null;
			ObjectOutput out=null;
			ObjectInput in=null;
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			ByteArrayInputStream bis=null;
			InetAddress returnAddress=null;
			int returnPort=0;
			OperationCode opCode=null;
			int callId=0;
			String data=null;
			String[] tokens=null;
			String output=null;

			try
			{
				// converting the object type of inBuf to byte[]
				out=new ObjectOutputStream(bos);
				out.writeObject(inBuf);

				byte[] inBufBytes=bos.toByteArray();
				DatagramPacket recvPkt=new DatagramPacket(inBufBytes,inBufBytes.length);

				_rpcSocket.receive(recvPkt);

				returnAddress=recvPkt.getAddress();
				returnPort=recvPkt.getPort();

				// converting byte[] to object type inBuf
				bis=new ByteArrayInputStream(inBufBytes);
				in=new ObjectInputStream(bis);
				Object obj=in.readObject();
				inBuf=(ServerSingleton.InBuf)obj;

				// check for the opcode and the call id
				opCode=inBuf.getOpCode();
				callId=inBuf.getCallId();
				data=inBuf.getData();
				tokens=Util.tokenize(data);

				switch(opCode)
				{
					case SESSIONREAD:
						output=SessionRead(tokens[0],Integer.parseInt(tokens[1]));
						break;
					case SESSIONWRITE:
						output=SessionWrite(tokens[0],Integer.parseInt(tokens[1]),tokens[2],tokens[3]);
						break;
					case SESSIONDELETE:
						output=SessionDelete(tokens[0],Integer.parseInt(tokens[1]));
						break;
					case GETMEMBER:
						output=GetMembers(Integer.parseInt(tokens[0]));
						break;
				}

			}
			catch(IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch(ClassNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				try
				{
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
			}
		}
	}

}
