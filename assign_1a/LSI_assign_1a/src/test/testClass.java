package test;

/**
 * This is the main Servlet class that handles both GET and POST requests and
 * performs session management
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@SuppressWarnings("serial")
@WebServlet("/testClass")
public class testClass extends HttpServlet
{
	private static final String CONST_STR_DEF_MSG_HELLO_USER="Hello User!";
	private static final String CONST_STRING_SDF_FORMAT="EEE MMM dd HH:mm:ss zzz yyyy";
	Calendar cal;
	HttpSession session;
	SimpleDateFormat sdf;
	PrintWriter pWriter;
	static int i=0;
	String sessionID;
	static ConcurrentHashMap<String,String> sessionInfo;

	/**
	 * 
	 * This is the default constructor.
	 * 
	 */
	public testClass()
	{
		sdf=new SimpleDateFormat(CONST_STRING_SDF_FORMAT);
		sessionID = new String();
		sessionInfo = new ConcurrentHashMap<>();
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
		i++;
		System.out.println("i is :: "+i);

		//to prevent caching - for the issue with back button on browser
		resp.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
		resp.setHeader("Pragma","no-cache");
		// get the latest time instance for each request
		cal=Calendar.getInstance();

		// get the response writer object
		pWriter=resp.getWriter();

		// Check if a cookie exists
		Cookie currCookie=checkCookieExists(req);

		// currCookie will be null if the user visits for the first time or has
		// visited earlier and logged out
		if(currCookie==null||getValueCookie(currCookie)==null||getValueCookie(currCookie).length()==0)
		{
			System.out.println("back after logout!");
			// create the new cookie
			currCookie=createCookie(resp, req);
			// set default text
			req.setAttribute("New Text","Hello, User!");
		}

		// Check for cookie validity
		boolean cookieExpired=checkCookieExpired(currCookie);

		if(cookieExpired&&(req.getParameter("cmd")==null||(!req.getParameter("cmd").equalsIgnoreCase("Refresh"))))
		{
			// session expired and something other than Refresh is requested
			// this implies that only the Refresh button can be used to refresh
			// the session after it has expired
			String additionalData="\nSession Expired. Please Refresh your session using the Refresh Button on the Home Page!\n";
			showFormData(req,currCookie,additionalData,false);
			return;
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
				pWriter.print("logout successful");
			}
			else
			{
				showFormData(req,currCookie,"Logout Failed! - try again",false);
			}

			return;
		}

		showFormData(req,currCookie,null,false);
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
	protected void showFormData(HttpServletRequest req,Cookie currCookie,String additionalData,boolean prefixSuffix)
	{
		if(additionalData!=null&&additionalData.length()!=0)
		{
			if(prefixSuffix)
			{
				pWriter.println(additionalData);
				// pWriter.print("<!DOCTYPE html>\n"+"<html>\n"+"<head>\n"+"<title>CS 5300 Project 1A</title>\n"+"</head>\n"+"<body bgcolor=\"#FDF5E6\">\n"+"<FORM ACTION=\"testClass\">\n"+"<fieldset>\n"+"<legend>Project 1A - By gmv33, sc2466, jkb243 - February 23, 2013</legend>\n"+"<h1>"+(currCookie.getValue().split(","))[2]+"</h1>"+"<ul>\n"+"<li>\n"+"<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;\n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=Refresh> \n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=LogOut>\n"+"</li>\n"+"</ul>\n"+"</fieldset>\n"+"<p>Server IP and Port is:: "+req.getLocalAddr()+":"+req.getLocalPort()+"</p>\n"+"</FORM>\n"+"</body>\n"+"</html>");
				// pWriter.print("\nExpiration Time: "+(currCookie.getValue().split(","))[3].toString());
			}
			else
			{
				pWriter.println("<!DOCTYPE html>\n"+"<html>\n"+"<head>\n"+"<title>CS 5300 Project 1A</title>\n"+"</head>\n"+"<body bgcolor=\"#FDF5E6\">\n"+"<FORM ACTION=\"testClass\">\n"+"<fieldset>\n"+"<legend>Project 1A - By gmv33, sc2466, jkb243 - February 23, 2013</legend>\n"+"<h1>"+(getValueCookie(currCookie).split(","))[2]+"</h1>"+"<ul>\n"+"<li>\n"+"<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;\n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=Refresh> \n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=LogOut>\n"+"</li>\n"+"</ul>\n"+"</fieldset>\n"+"<p>Server IP and Port is:: "+req.getLocalAddr()+":"+req.getLocalPort()+"</p>\n"+"</FORM>\n"+"</body>\n"+"</html>");
				pWriter.println("\nExpiration Time: "+(getValueCookie(currCookie).split(","))[3].toString()+"\n");
				pWriter.println("\nID: "+(getValueCookie(currCookie).split(","))[0].toString()+"\n");
				pWriter.println(additionalData);
			}
		}
		else
		{
			pWriter.println("<!DOCTYPE html>\n"+"<html>\n"+"<head>\n"+"<title>CS 5300 Project 1A</title>\n"+"</head>\n"+"<body bgcolor=\"#FDF5E6\">\n"+"<FORM ACTION=\"testClass\">\n"+"<fieldset>\n"+"<legend>Project 1A - By gmv33, sc2466, jkb243 - February 23, 2013</legend>\n"+"<h1>"+(getValueCookie(currCookie).split(","))[2]+"</h1>"+"<ul>\n"+"<li>\n"+"<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;\n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=Refresh> \n"+"</li>\n"+"<li>\n"+"<input type=submit name=cmd value=LogOut>\n"+"</li>\n"+"</ul>\n"+"</fieldset>\n"+"<p>Server IP and Port is:: "+req.getLocalAddr()+":"+req.getLocalPort()+"</p>\n"+"</FORM>\n"+"</body>\n"+"</html>");
			pWriter.println("\nExpiration Time: "+(getValueCookie(currCookie).split(","))[3].toString());
			pWriter.println("\nID: "+(getValueCookie(currCookie).split(","))[0].toString());
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
			if(sdf.parse((getValueCookie(ckey).split(","))[3]).before(Calendar.getInstance().getTime()))
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
	public Cookie createCookie(HttpServletResponse resp, HttpServletRequest req)
	{
		cal.add(Calendar.SECOND,60);
		//String cookieValue=session.getId()+",0,"+CONST_STR_DEF_MSG_HELLO_USER+","+cal.getTime().toString()+",US";
		String sessionID = getUniqueSessionId(req);
		String sessionValue=sessionID+",0,"+CONST_STR_DEF_MSG_HELLO_USER+","+cal.getTime().toString()+",US";
		sessionInfo.put(sessionID,sessionValue);
		//SessionTable.put(sessionID,sessionValue);
		Cookie ckey=new Cookie("testClassCookie",sessionID);
		resp.addCookie(ckey);
		return ckey;
	}

	private String getUniqueSessionId(HttpServletRequest req)
	{
		String uniqueId = req.getRemoteAddr() +"-"+  cal.getTime().toString();
		return uniqueId;
	}

	private static String getValueCookie(Cookie ckey){
		String sessionID = ckey.getValue();
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
		return getCookieFromSession(req,"testClassCookie");
	}

	/**
	 * Performs the Refresh operation
	 * 
	 * @param ckey
	 * @param resp
	 */
	public void doRefresh(Cookie ckey,HttpServletResponse resp)
	{
		cal.add(Calendar.SECOND,60);
		String sessionVal=getValueCookie(ckey);
		String[] sessionInfoArr=sessionVal.split(",");
		sessionInfoArr[2]=CONST_STR_DEF_MSG_HELLO_USER;
		sessionInfoArr[3]=cal.getTime().toString();
		sessionVal=getCookieStringFromArray(sessionInfoArr);
		sessionInfo.remove(ckey.getValue());
		sessionInfo.put(ckey.getValue(),sessionVal);
		//ckey.setValue(cookieValue);
		resp.addCookie(ckey);
	}

	/**
	 * Performs the Replace Operation
	 * 
	 * @param req
	 */
	public void doReplace(HttpServletRequest req,Cookie currCookie,HttpServletResponse resp)
	{
		/*
		 * if(req.getParameter("NewText")==null||req.getParameter("NewText").length
		 * ()==0) req.setAttribute("NewText","Hello, User!"); else
		 * req.setAttribute("NewText",req.getParameter("NewText"));
		 */
		String textFldValue=req.getParameter("NewText");
		if(textFldValue!=null&&textFldValue.length()!=0)
		{
			String[] sessionValArr=getValueCookie(currCookie).split(",");
			sessionValArr[2]=textFldValue;
			String cookieVal=getCookieStringFromArray(sessionValArr);
			sessionInfo.remove(currCookie.getValue());
			sessionInfo.put(currCookie.getValue(),cookieVal);
			//currCookie.setValue(cookieVal);
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
			//remove the session id
			sessionInfo.remove(getValueCookie(ckey));
			System.out.println("do logout");
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
				return (getValueCookie(cookie));
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
			for(Cookie ckey : req.getCookies())
			{
				if(ckey.getName().equalsIgnoreCase(cookieName))
					return ckey;
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
}
