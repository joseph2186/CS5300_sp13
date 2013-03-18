package sessionManagement;

/**
 * This is the main Servlet class that handles both GET and POST requests and
 * performs session management
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/sessionManagement")
public class SessionManagement extends HttpServlet
{
	// milliseconds
	private static final int CONST_LONG_DAEMON_RUN_FREQ=50000;
	private static final String CONST_STR_COOKIE_NAME="CS5300PROJ1SESSION";
	// seconds
	private static final int CONST_INT_SESSION_TIMEOUT_VAL=60;
	private static final String CONST_STR_DEF_MSG_HELLO_USER="Hello User!";
	private static final String CONST_STRING_SDF_FORMAT="EEE MMM dd HH:mm:ss zzz yyyy";
	static Map<String,String> sessionInfo;

	/**
	 * 
	 * This is the default constructor. Will be called only once until the
	 * servlet container trashes the servlet.
	 * 
	 */
	public SessionManagement()
	{
		sessionInfo=new ConcurrentHashMap<String, String>();

		// instantiate the daemon thread for session table cleanup
		new Thread(new Runnable()
		{
			public void run()
			{
				while(true)
				{
					Calendar cal=Calendar.getInstance();
					Date timeVal=new Date();
					for(Entry<String,String> cMapEntrySet : sessionInfo.entrySet())
					{
						String sessionValue=cMapEntrySet.getValue();
						SimpleDateFormat sdf=new SimpleDateFormat(CONST_STRING_SDF_FORMAT);

						if(sessionValue!=null&&sessionValue.length()!=0)
						{
							try
							{
								timeVal=sdf.parse(sessionValue.split(",")[3]);
								if(timeVal.before(cal.getTime()))
								{
									System.out.println("Value deleted by Daemon Thread from Session Table :: "+cMapEntrySet.getKey());
									((ConcurrentHashMap<String,String>)sessionInfo).remove(cMapEntrySet.getKey());
								}
							}
							catch(ParseException e1)
							{
								e1.printStackTrace();
							}
						}
						else
						{
							((ConcurrentHashMap<String,String>)sessionInfo).remove(cMapEntrySet.getKey());
						}
					}
					try
					{
						Thread.sleep(CONST_LONG_DAEMON_RUN_FREQ);
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	/**
	 * This method handles incoming GET requests from the Form
	 * 
	 * @param req
	 *            the request object
	 * @param resp
	 *            the response object
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws ServletException,IOException
	{
		// to prevent caching - for the issue with back button on browser
		resp.setHeader("Cache-Control","private, no-store, no-cache, must-revalidate");
		resp.setHeader("Pragma","no-cache");

		// Check if a cookie exists
		Cookie currCookie=checkCookieExists(req);

		// currCookie will be null if the user visits for the first time or has
		// visited earlier and logged out
		if(currCookie==null)
		{
			// create the new cookie
			currCookie=createCookie(resp,req);
			// set default text
			req.setAttribute("NewText","Hello, User!");
		}

		setVersionID(req,currCookie);

		// Check for cookie validity
		boolean cookieExpired=checkCookieExpired(currCookie);
		if(cookieExpired&&req.getParameter("cmd")!=null&&!req.getParameter("cmd").equalsIgnoreCase("LogOut"))
		{
			String newSessionID=createSessionState(req);
			currCookie.setValue(newSessionID);
			resp.addCookie(currCookie);

			if(req.getParameter("cmd").equalsIgnoreCase("Replace"))
				doReplace(req,currCookie,resp);
			else if(req.getParameter("cmd").equalsIgnoreCase("Refresh"))
				doRefresh(currCookie,resp);

			showFormData(req,currCookie,null,false,resp);

			return;
		}
		
		// client reopens expired session in browser
		if(cookieExpired && req.getParameter("cmd")==null)
		{
			String newSessionID=createSessionState(req);
			currCookie.setValue(newSessionID);
			resp.addCookie(currCookie);
		}
		
		// Replace request
		if(req.getParameter("cmd")!=null&&req.getParameter("cmd").equalsIgnoreCase("Replace"))
		{
			doReplace(req,currCookie,resp);
		}
		// Refresh request
		else if(req.getParameter("cmd")!=null&&req.getParameter("cmd").equalsIgnoreCase("Refresh"))
		{
			doRefresh(currCookie,resp);
			doReplace(req,currCookie,resp);
		}
		// Logout request
		else if(req.getParameter("cmd")!=null&&req.getParameter("cmd").equalsIgnoreCase("Logout"))
		{
			if(doLogout(currCookie,resp))
			{
				// pWriter.print("logout successful");
				showFormData(req,currCookie,"logout successful",true,resp);
			}
			else
			{
				showFormData(req,currCookie,"Logout Failed! - try again",false,resp);
			}

			return;
		}

		// The below is used when Refresh operation is performed
		showFormData(req,currCookie,null,false,resp);
	}

	/**
	 * 
	 * 
	 * @param req
	 * @param currCookie
	 * @param additionalData
	 *            additional html data to be printed on screen
	 * @param prefixSuffix
	 *            if additionalData exists, then whether to prefix it or suffix
	 *            it; true indicates prefix
	 */
	protected void showFormData(HttpServletRequest req,Cookie currCookie,String additionalData,boolean prefixSuffix,HttpServletResponse resp)
	{
		// initialize the printwriter with the object from response
		PrintWriter pWriter;
		try
		{
			pWriter=resp.getWriter();
			if(additionalData!=null&&additionalData.length()!=0)
			{
				if(prefixSuffix)
				{
					pWriter.println(additionalData);
					// pWriter.print("<!DOCTYPE html>\n"+"<html>\n"+"<head>\n"+"<title>CS 5300 Project 1A</title>\n"+"</head>\n"+"<body bgcolor=\"#FDF5E6\">\n"+"<FORM ACTION=\"sessionManagement\">\n"+"<fieldset>\n"+"<legend>Project 1A - By gmv33, sc2466, jkb243</legend>\n"+"<h1>"+(currCookie.getValue().split(","))[2]+"</h1>"+"<ul>\n"+"<li>\n"+"<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;\n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=Refresh> \n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=LogOut>\n"+"</li>\n"+"</ul>\n"+"</fieldset>\n"+"<p>Server IP and Port is:: "+req.getLocalAddr()+":"+req.getLocalPort()+"</p>\n"+"</FORM>\n"+"</body>\n"+"</html>");
					// pWriter.print("\nExpiration Time: "+(currCookie.getValue().split(","))[3].toString());
				}
				else
				{
					String sessionVal=getSessionValue(currCookie);
					if(sessionVal==null)
					{
						pWriter.println(additionalData);
					}
					else
					{
						pWriter.println("<!DOCTYPE html>\n"+"<html>\n"+"<head>\n"+"<title>CS 5300 Project 1A</title>\n"+"</head>\n"+"<body bgcolor=\"#FDF5E6\">\n"+"<FORM ACTION=\"sessionManagement\">\n"+"<fieldset>\n"+"<legend>Project 1A - By gmv33, sc2466, jkb243</legend>\n"+"<h1>"+(sessionVal.split(","))[2]+"</h1>"+"<ul>\n"+"<li>\n"+"<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;\n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=Refresh> \n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=LogOut>\n"+"</li>\n"+"</ul>\n"+"</fieldset>\n"+"<p>Server IP and Port is:: "+req.getLocalAddr()+":"+req.getLocalPort()+"</p>\n"+"</FORM>\n"+"</body>\n"+"</html>");
						pWriter.println("\nExpiration Time: "+(sessionVal.split(","))[3].toString()+"\n");
						// pWriter.println("\nID: "+(sessionVal.split(","))[0].toString()+"\n");
						pWriter.println(additionalData);
					}
				}
			}
			else
			{
				pWriter.println("<!DOCTYPE html>\n"+"<html>\n"+"<head>\n"+"<title>CS 5300 Project 1A</title>\n"+"</head>\n"+"<body bgcolor=\"#FDF5E6\">\n"+"<FORM ACTION=\"sessionManagement\">\n"+"<fieldset>\n"+"<legend>Project 1A - By gmv33, sc2466, jkb243</legend>\n"+"<h1>"+(getSessionValue(currCookie).split(","))[2]+"</h1>"+"<ul>\n"+"<li>\n"+"<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;\n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=Refresh> \n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=LogOut>\n"+"</li>\n"+"</ul>\n"+"</fieldset>\n"+"<p>Server IP and Port is:: "+req.getLocalAddr()+":"+req.getLocalPort()+"</p>\n"+"</FORM>\n"+"</body>\n"+"</html>");
				pWriter.println("\nExpiration Time: "+(getSessionValue(currCookie).split(","))[3].toString());
				// pWriter.println("\nID: "+(getSessionValue(currCookie).split(","))[0].toString());
			}
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Compares the current cookies expiration time stamp from cookie value with
	 * current timestamp to check if the cookie is expired or not
	 * 
	 * @param ckey
	 * @return
	 */
	public boolean checkCookieExpired(Cookie ckey)
	{
		try
		{
			SimpleDateFormat sdf=new SimpleDateFormat(CONST_STRING_SDF_FORMAT);
			String sessionVal=getSessionValue(ckey);
			if(sessionVal==null||sdf.parse((sessionVal.split(","))[3]).before(Calendar.getInstance().getTime()))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		catch(ParseException e)
		{
			e.printStackTrace();
			return true;
		}
	}

	/**
	 * Creates a new Cookie
	 * 
	 * @param resp
	 * @return
	 */
	public Cookie createCookie(HttpServletResponse resp,HttpServletRequest req)
	{
		String sessionID=createSessionState(req);
		Cookie ckey=new Cookie(CONST_STR_COOKIE_NAME,sessionID);
		resp.addCookie(ckey);
		return ckey;
	}

	/**
	 * 
	 * @param req
	 * @return
	 */
	protected String createSessionState(HttpServletRequest req)
	{
		Calendar cal=Calendar.getInstance();
		cal.add(Calendar.SECOND,CONST_INT_SESSION_TIMEOUT_VAL);
		String sessionID=getUniqueSessionId(req);
		String sessionValue=sessionID+",1,"+CONST_STR_DEF_MSG_HELLO_USER+","+cal.getTime().toString()+",US";
		sessionInfo.put(sessionID,sessionValue);
		return sessionID;
	}

	private String getUniqueSessionId(HttpServletRequest req)
	{
		Calendar cal=Calendar.getInstance();
		String uniqueId=req.getRemoteAddr()+"-"+cal.getTime().toString();
		return uniqueId;
	}

	private static String getSessionValue(Cookie ckey)
	{
		String sessionID=ckey.getValue();
		return sessionInfo.get(sessionID);
	}

	/**
	 * Checks if a cookie for this user exists or not.
	 * 
	 * @param req
	 * @return true if cookie found irrespective of whether expired or not
	 */
	public Cookie checkCookieExists(HttpServletRequest req)
	{
		return getCookieFromSession(req,CONST_STR_COOKIE_NAME);
	}

	/**
	 * Performs the Refresh operation
	 * 
	 * @param ckey
	 * @param resp
	 */
	public void doRefresh(Cookie ckey,HttpServletResponse resp)
	{
		Calendar cal=Calendar.getInstance();
		cal.add(Calendar.SECOND,CONST_INT_SESSION_TIMEOUT_VAL);
		String sessionVal=getSessionValue(ckey);
		String[] sessionInfoArr=sessionVal.split(",");
		sessionInfoArr[2]=CONST_STR_DEF_MSG_HELLO_USER;
		sessionInfoArr[3]=cal.getTime().toString();
		sessionVal=getCookieStringFromArray(sessionInfoArr);
		// sessionInfo.remove(ckey.getValue());
		sessionInfo.put(ckey.getValue(),sessionVal);
		// ckey.setValue(cookieValue);
		resp.addCookie(ckey);
	}

	/**
	 * Performs the Replace Operation
	 * 
	 * @param req
	 */
	public void doReplace(HttpServletRequest req,Cookie currCookie,HttpServletResponse resp)
	{
		String textFldValue=req.getParameter("NewText");
		Calendar cal=Calendar.getInstance();
		if(textFldValue!=null&&textFldValue.length()!=0)
		{
			cal.add(Calendar.SECOND,CONST_INT_SESSION_TIMEOUT_VAL);
			String[] sessionValArr=getSessionValue(currCookie).split(",");
			sessionValArr[2]=textFldValue;
			sessionValArr[3]=cal.getTime().toString();
			String cookieVal=getCookieStringFromArray(sessionValArr);
			sessionInfo.remove(currCookie.getValue());
			sessionInfo.put(currCookie.getValue(),cookieVal);
			// currCookie.setValue(cookieVal);
			resp.addCookie(currCookie);
		}
	}

	/**
	 * Performs the logout operation
	 * 
	 * @param ckey
	 * @param resp
	 */
	public boolean doLogout(Cookie ckey,HttpServletResponse resp)
	{
		try
		{
			ckey.setMaxAge(0);
			resp.addCookie(ckey);
			// remove the session id
			if(getSessionValue(ckey)!=null&&getSessionValue(ckey).length()!=0)
				sessionInfo.remove(getSessionValue(ckey));
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}

	/**
	 * Gets the value component of the cookie name from the array of cookies
	 * passed in
	 * 
	 * @param cookies
	 * @param cookieName
	 * @return
	 */
	public static String getCookieValue(Cookie[] cookies,String cookieName)
	{
		for(int i=0;i<cookies.length;i++)
		{
			Cookie cookie=cookies[i];
			if(cookieName.equals(cookie.getName()))
				return (getSessionValue(cookie));
		}
		return null;
	}

	/**
	 * This method returns the cookie from the http request object
	 * 
	 * @param req
	 * @param cookieName
	 * @return
	 */
	public static Cookie getCookieFromSession(HttpServletRequest req,String cookieName)
	{
		if(req.getCookies()!=null&&req.getCookies().length>0)
		{
			for(Cookie ckey : req.getCookies())
			{
				if(ckey.getName().equalsIgnoreCase(cookieName))
					return ckey;
			}
		}
		return null;
	}

	/**
	 * Creates and returns a comma separated list of values from the array of
	 * values passed in
	 * 
	 * @param cookieValString
	 * @return
	 */
	public String getCookieStringFromArray(String[] cookieValString)
	{
		StringBuilder concatenatedStr=new StringBuilder();
		for(String string : cookieValString)
		{
			concatenatedStr=concatenatedStr.append(string).append(",");
		}
		return concatenatedStr.substring(0,concatenatedStr.length()-1);
	}

	/**
	 * To handle the post http request
	 * 
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws ServletException,IOException
	{
		doGet(req,resp);
	}

	/**
	 * Sets the version ID on each request for the same session
	 */
	protected void setVersionID(HttpServletRequest req,Cookie cookie)
	{
		String sessionID=cookie.getValue();
		String sessionVal=sessionInfo.get(sessionID);
		if(sessionVal!=null&&sessionVal.length()!=0)
		{
			String[] sessionValArr=sessionVal.split(",");
			sessionValArr[1]=String.valueOf((Integer.parseInt(sessionValArr[1])+1));
			sessionInfo.put(sessionID,getCookieStringFromArray(sessionValArr));
		}
	}
}
