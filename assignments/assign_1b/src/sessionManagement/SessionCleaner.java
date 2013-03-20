package sessionManagement;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class SessionCleaner extends Thread
{
	ServerSingleton serverInstance = null;
	
	public SessionCleaner()
	{
		serverInstance = ServerSingleton.getInstance();
	}
	
	@Override
	public void run()
	{
		//TODO Auto-generated method stub
		while(true)
		{
			Calendar cal=Calendar.getInstance();
			Date timeVal=new Date();
			for(Entry<String,String> cMapEntrySet : serverInstance.sessionInfo.entrySet())
			{
				String sessionValue=cMapEntrySet.getValue();
				SimpleDateFormat sdf=new SimpleDateFormat(ServerSingleton.CONST_STRING_SDF_FORMAT);

				if(sessionValue!=null&&sessionValue.length()!=0)
				{
					try
					{
						timeVal=sdf.parse(sessionValue.split(",")[3]);
						if(timeVal.before(cal.getTime()))
						{
							System.out.println("Value deleted by Daemon Thread from Session Table :: "+cMapEntrySet.getKey());
							((ConcurrentHashMap<String,String>)serverInstance.sessionInfo).remove(cMapEntrySet.getKey());
						}
					}
					catch(ParseException e1)
					{
						e1.printStackTrace();
					}
				}
				else
				{
					((ConcurrentHashMap<String,String>)serverInstance.sessionInfo).remove(cMapEntrySet.getKey());
				}
			}
			try
			{
				Thread.sleep(ServerSingleton.CONST_LONG_DAEMON_RUN_FREQ);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

}