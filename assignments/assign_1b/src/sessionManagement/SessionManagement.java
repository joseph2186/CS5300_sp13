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
import java.util.Currency;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/sessionManagement")
public class SessionManagement extends HttpServlet {
	protected ServerSingleton serverInstance = null;
	private SessionCleaner _sessionCleaner = null;
	private RpcServerStub _serverStub = null;

	/**
	 * 
	 * This is the default constructor. Will be called only once until the
	 * servlet container trashes the servlet.
	 * 
	 */
	public SessionManagement (){
		serverInstance = ServerSingleton.getInstance();

		// instantiate the daemon thread for session table cleanup
		_sessionCleaner = new SessionCleaner();
		_sessionCleaner.start();

		_serverStub = RpcServerStub.getInstance(serverInstance);

		// Initialize the callID
		if(serverInstance.get_callId() == 0)
			serverInstance.set_callId(_serverStub.get_rpcSocket()
					.getLocalPort() * 10000);

		// Get the rpc socket
		serverInstance.setRpcSocket(_serverStub.get_rpcSocket());

		_serverStub.start();

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
	protected void doGet(HttpServletRequest req,HttpServletResponse resp)
			throws ServletException,IOException{
		String[] sessionInfo = null;
		// to prevent caching - for the issue with back button on browser
		resp.setHeader("Cache-Control",
				"private, no-store, no-cache, must-revalidate");
		resp.setHeader("Pragma","no-cache");
		// set the content type for the page to get rendered as HTML
		resp.setContentType("text/html");

		// Check if a cookie exists - returns null on not finding
		Cookie currCookie = checkCookieExists(req);

		// currCookie will be null if the user visits for the first time or has
		// visited earlier and logged out
		if(currCookie == null){
			// create the new cookie
			currCookie = createCookie(resp,req);

			// set default text
			req.setAttribute("NewText",
					ServerSingleton.CONST_STR_DEF_MSG_HELLO_USER);

			showFormData(req,currCookie,null,false,resp);

			return;
		}
		// Get the session info either from the local datastructure or from a
		// remote server
		sessionInfo = getSessionInfo(currCookie,req);

		sessionManager(currCookie,req,resp,sessionInfo);

	}

	// TODO: need to add the discard time logic everywhere

	private void sessionManager(Cookie cookie,HttpServletRequest req,
			HttpServletResponse resp,String[] sessionInfo){
		// handling the response
		// output[0] - found version
		// output[1] - message
		// output[2] - expiry time

		// If sessionInfo was returned null then the session is invalid - return
		// a error
		if(sessionInfo == null){
			// remove the cookie - similar to dologout()
			ServerSingleton.mbrSet.remove(Util.getPrimaryIpp(cookie));
			ServerSingleton.mbrSet.remove(Util.getBackupIpp(cookie));
			doLogout(cookie,resp);
			showFormData(req,cookie,"Session failed!",true,resp);
			return;
		}

		// update the local session table
		serverInstance.sessionInfoCMap.put(Util.getSessionId(cookie),
				Util.combine(sessionInfo));

		// Check for cookie validity
		boolean cookieExpired =
				checkCookieExpired(sessionInfo[Util.EXPIRATION_TIME]);

		if(cookieExpired && req.getParameter("cmd") != null
				&& !req.getParameter("cmd").equalsIgnoreCase("LogOut")){
			String newSessionID = createSessionState(req);

			if(req.getParameter("cmd").equalsIgnoreCase("Replace"))
				doReplace(req,cookie,resp);
			else if(req.getParameter("cmd").equalsIgnoreCase("Refresh"))
				doRefresh(cookie,resp);

			showFormData(req,cookie,null,false,resp);

			// write to the backup server
			updateBackupServer(newSessionID,sessionInfo,cookie);
			cookie.setValue(newSessionID);
			resp.addCookie(cookie);

			return;
		}

		// client reopens expired session in browser
		if(cookieExpired && req.getParameter("cmd") == null){
			String newSessionID = createSessionState(req);

			// write to the backup server
			updateBackupServer(newSessionID,sessionInfo,cookie);
			cookie.setValue(newSessionID);
			resp.addCookie(cookie);
		}

		// Replace request
		if(req.getParameter("cmd") != null
				&& req.getParameter("cmd").equalsIgnoreCase("Replace")){
			doReplace(req,cookie,resp);
		}
		// Refresh request
		else if(req.getParameter("cmd") != null
				&& req.getParameter("cmd").equalsIgnoreCase("Refresh")){
			doRefresh(cookie,resp);
			doReplace(req,cookie,resp);
		}
		// Logout request
		else if(req.getParameter("cmd") != null
				&& req.getParameter("cmd").equalsIgnoreCase("Logout")){
			if(doLogout(cookie,resp)){
				// pWriter.print("logout successful");
				showFormData(req,cookie,"logout successful",true,resp);
			}else{
				showFormData(req,cookie,"Logout Failed! - try again",false,resp);
			}

			return;
		}

		// The below is used when Refresh operation is performed
		showFormData(req,cookie,null,false,resp);
	}

	private void updateBackupServer(String newSessionID,String[] sessionInfo,
			Cookie cookie){
		// TODO: Add the discard time logic in sessionInfo
		String[] ippList = null;
		int i = 0;
		String response = "";
		String[] ippBackup = new String[2];
		String data = newSessionID + Util.DELIM + Util.combine(sessionInfo);
		do{
			if(i < ServerSingleton.mbrSet.size()){
				ippList = Util.tokenize(ServerSingleton.mbrSet.elementAt(i));
				RpcClientStub clientStub =
						new RpcClientStub(OperationCode.SESSIONWRITE,
								serverInstance.getNextCallId(),ippList,data);
				response = clientStub.RpcClientStubHandler()[0];
				i++ ;
				if(null == response){
					continue;
				}
			}else{
				break;
			}
		}while(response.equalsIgnoreCase(Util.NACK));

		// Case when none of the known servers responded to the backup request
		if(response == null || response.equalsIgnoreCase(Util.NACK)
				|| ippList == null){
			// set the ipp backup to NULL
			ippBackup[0] = Util.NULL_IP;
			ippBackup[1] = Util.NULL_PORT;

		}else{
			ippBackup = ippList;
		}
		Util.updateIppBackup(cookie,ippBackup);
	}

	private String[] getSessionInfo(Cookie currCookie,HttpServletRequest req){
		String[] sessionInfo = null;
		String sessionId = Util.getSessionId(currCookie);
		String[] ippList = Util.getIppList(currCookie);
		String version = Util.getVersionNumber(currCookie);
		String data = sessionId + Util.DELIM + version;

		// Check if the IPP in the cookie is not equal to the servers IPP
		if( !Util.isIppMine(currCookie,getIPP(req))){
			// update the mbrset
			if( !ServerSingleton.mbrSet
					.contains(Util.getPrimaryIpp(currCookie))){
				serverInstance.mbrSet.add(Util.getPrimaryIpp(currCookie));
			}
			if( !ServerSingleton.mbrSet.contains(Util.getBackupIpp(currCookie))){
				serverInstance.mbrSet.add(Util.getBackupIpp(currCookie));
			}

			int callId = serverInstance.getNextCallId();

			// send a read request to primary and the backup
			RpcClientStub clientStub =
					new RpcClientStub(OperationCode.SESSIONREAD,callId,ippList,
							data);

			sessionInfo = clientStub.RpcClientStubHandler();

			if(sessionInfo == null){
				// Error: could not fetch the session info from both the primary
				// as well as the backup
				// Return a timeout and delets the cookie

			}

		}else{
			String sessionInfoLocal =
					serverInstance.sessionInfoCMap.get(sessionId);
			sessionInfo = Util.tokenize(sessionInfoLocal);
		}
		return sessionInfo;
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
	protected void
			showFormData(HttpServletRequest req,Cookie currCookie,
					String additionalData,boolean prefixSuffix,
					HttpServletResponse resp){
		// initialize the printwriter with the object from response
		PrintWriter pWriter;
		String sessionVal = getSessionValue(currCookie);
		String[] sessionValTokens = Util.tokenize(sessionVal);
		try{
			pWriter = resp.getWriter();
			if(additionalData != null && additionalData.length() != 0){
				if(prefixSuffix){
					pWriter.println(additionalData);

				}else{

					if(sessionValTokens == null){
						pWriter.println(additionalData);
					}else{

						pWriter.println("<!DOCTYPE html>\n"
								+ "<html>\n"
								+ "<head>\n"
								+ "<title>CS 5300 Project 1B</title>\n"
								+ "</head>\n"
								+ "<body bgcolor=\"#FDF5E6\">\n"
								+ "<FORM ACTION=\"sessionManagement\">\n"
								+ "<fieldset>\n"
								+ "<legend>Project 1B - By gmv33, sc2466, jkb243</legend>\n"
								+ "<h1>"
								+ (sessionValTokens[Util.MESSAGE])
								+ "</h1>"
								+ "<ul>\n"
								+ "<li>\n"
								+ "<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;\n"
								+ "</li>\n"
								+ "<li>\n"
								+ "<input type=submit name=cmd value=Refresh> \n"
								+ "</li>\n"
								+ "<li>\n"
								+ "<input type=submit name=cmd value=LogOut>\n"
								+ "</li>\n"
								+ "</ul>\n"
								+ "</fieldset>\n"
								+ "<p>Server IP and Port is:: "
								+ req.getLocalAddr()
								+ ":"
								+ req.getLocalPort()
								+ "</p>\n"
								+ "</FORM>\n"
								+ "</body>\n"
								+ "</html>");

						pWriter.println("\nExpiration Time: "
								+ (sessionValTokens[Util.EXPIRATION_TIME])
								+ "\n");

						pWriter.println(additionalData);
					}
				}
			}else{
				pWriter.println("<!DOCTYPE html>\n"
						+ "<html>\n"
						+ "<head>\n"
						+ "<title>CS 5300 Project 1B</title>\n"
						+ "</head>\n"
						+ "<body bgcolor=\"#FDF5E6\">\n"
						+ "<FORM ACTION=\"sessionManagement\">\n"
						+ "<fieldset>\n"
						+ "<legend>Project 1B - By gmv33, sc2466, jkb243</legend>\n"
						+ "<h1>"
						+ (sessionValTokens[Util.MESSAGE])
						+ "</h1>"
						+ "<ul>\n"
						+ "<li>\n"
						+ "<input type=submit name=cmd value=Replace>&nbsp;&nbsp;<input type=text name=NewText size=40 maxlength=512>&nbsp;&nbsp;\n"
						+ "</li>\n"
						+ "<li>\n"
						+ "<input type=submit name=cmd value=Refresh> \n"
						+ "</li>\n"
						+ "<li>\n"
						+ "<input type=submit name=cmd value=LogOut>\n"
						+ "</li>\n"
						+ "</ul>\n"
						+ "</fieldset>\n"
						+ "<p>Server IP and Port is:: "
						+ req.getLocalAddr()
						+ ":"
						+ req.getLocalPort()
						+ "</p>\n"
						+ "</FORM>\n"
						+ "</body>\n" + "</html>");
				pWriter.println("\nExpiration Time: "
						+ (sessionValTokens[Util.EXPIRATION_TIME]) + "\n");

			}
		}catch(IOException e){
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
	public boolean checkCookieExpired(String expiryTime){
		try{
			SimpleDateFormat sdf =
					new SimpleDateFormat(
							ServerSingleton.CONST_STRING_SDF_FORMAT);
			if(sdf.parse(expiryTime).before(Calendar.getInstance().getTime())){
				return true;
			}else{
				return false;
			}
		}catch(ParseException e){
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
	public Cookie createCookie(HttpServletResponse resp,HttpServletRequest req){
		String sessionID = createSessionState(req);

		// TODO: write apis to parse the data
		// TODO: need to get the backup and primary IPPs

		// Cookie value - sessionID_version_location
		String location = getIPP(req);

		// initial value of version is 0
		String cookieValue =
				sessionID + Util.DELIM + "0" + Util.DELIM + location
						+ Util.DELIM + Util.NULL_IP + Util.DELIM
						+ Util.NULL_PORT;

		Cookie cookie =
				new Cookie(ServerSingleton.CONST_STR_COOKIE_NAME,cookieValue);

		// update the backup server
		updateBackupServer(sessionID,
				Util.tokenize(ServerSingleton.sessionInfoCMap.get(sessionID)),
				cookie);
		resp.addCookie(cookie);
		return cookie;
	}

	/**
	 * 
	 * @param req
	 * @return
	 */
	protected String createSessionState(HttpServletRequest req){
		// TODO: change all the _ to ._
		Calendar calTimeOut = Calendar.getInstance();
		calTimeOut.add(Calendar.SECOND,
				ServerSingleton.CONST_INT_SESSION_TIMEOUT_VAL);

		Calendar calDiscardTime = Calendar.getInstance();
		calDiscardTime
				.add(Calendar.SECOND,
						(2 * ServerSingleton.CONST_DELTA_TIMEOUT_VAL + ServerSingleton.CONST_GAMMA_TIMEOUT_VAL));

		// Session ID is now session ID and IPP pair
		String sessionID = "";
		int sessionNumber = serverInstance.getNextSessionNumber();
		sessionID = sessionNumber + Util.DELIM + getIPP(req);

		String sessionValue =
				"0" + Util.DELIM + ServerSingleton.CONST_STR_DEF_MSG_HELLO_USER
						+ Util.DELIM + calTimeOut.getTime().toString()
						+ Util.DELIM + calDiscardTime.getTime().toString();
		serverInstance.sessionInfoCMap.put(sessionID,sessionValue);
		return sessionID;
	}

	/**
	 * get the IPP - IP and port string
	 * 
	 * @return
	 */
	private String getIPP(HttpServletRequest req){
		String IPP = "";
		IPP = req.getLocalAddr() + Util.DELIM + req.getLocalPort();
		return IPP;
	}

	private String getSessionValue(Cookie cookie){
		String sessionID = Util.getSessionId(cookie);
		return serverInstance.getSessionInfo().get(sessionID);
	}

	/**
	 * Checks if a cookie for this user exists or not.
	 * 
	 * @param req
	 * @return true if cookie found irrespective of whether expired or not
	 */
	public Cookie checkCookieExists(HttpServletRequest req){
		if(req.getCookies() != null && req.getCookies().length > 0){
			for(Cookie ckey : req.getCookies()){
				if(ckey.getName().equalsIgnoreCase(
						ServerSingleton.CONST_STR_COOKIE_NAME))
					return ckey;
			}
		}
		return null;
	}

	/**
	 * Performs the Refresh operation
	 * 
	 * @param cookie
	 * @param resp
	 */
	public void doRefresh(Cookie cookie,HttpServletResponse resp){
		Calendar calTimeOut = Calendar.getInstance();
		calTimeOut.add(Calendar.SECOND,
				ServerSingleton.CONST_INT_SESSION_TIMEOUT_VAL);

		Calendar calDiscardTime = Calendar.getInstance();
		calDiscardTime
				.add(Calendar.SECOND,
						(2 * ServerSingleton.CONST_DELTA_TIMEOUT_VAL + ServerSingleton.CONST_GAMMA_TIMEOUT_VAL));

		String sessionVal = getSessionValue(cookie);

		String[] sessionInfoArr = Util.tokenize(sessionVal);
		// sessionInfoArr[Util.MESSAGE]=ServerSingleton.CONST_STR_DEF_MSG_HELLO_USER;
		sessionInfoArr[Util.EXPIRATION_TIME] = calTimeOut.getTime().toString();
		sessionInfoArr[Util.DISCARD_TIME] = calDiscardTime.getTime().toString();

		sessionVal = Util.combine(sessionInfoArr);

		serverInstance.sessionInfoCMap
				.put(Util.getSessionId(cookie),sessionVal);

		resp.addCookie(cookie);
	}

	/**
	 * Performs the Replace Operation
	 * 
	 * @param req
	 */
	public void doReplace(HttpServletRequest req,Cookie currCookie,
			HttpServletResponse resp){
		String text = req.getParameter("NewText");

		Calendar calTimeOut = Calendar.getInstance();
		calTimeOut.add(Calendar.SECOND,
				ServerSingleton.CONST_INT_SESSION_TIMEOUT_VAL);

		Calendar calDiscardTime = Calendar.getInstance();
		calDiscardTime
				.add(Calendar.SECOND,
						(2 * ServerSingleton.CONST_DELTA_TIMEOUT_VAL + ServerSingleton.CONST_GAMMA_TIMEOUT_VAL));
		String[] sessionValArr = Util.tokenize(getSessionValue(currCookie));

		if(text != null && text.length() != 0){
			sessionValArr[Util.MESSAGE] = text;
		}
		sessionValArr[Util.EXPIRATION_TIME] = calTimeOut.getTime().toString();
		sessionValArr[Util.DISCARD_TIME] = calDiscardTime.getTime().toString();

		String cookieVal = Util.combine(sessionValArr);

		serverInstance.sessionInfoCMap.remove(currCookie.getValue());
		serverInstance.sessionInfoCMap.put(Util.getSessionId(currCookie),
				cookieVal);
		// currCookie.setValue(cookieVal);
		resp.addCookie(currCookie);

	}

	/**
	 * Performs the logout operation
	 * 
	 * @param cookie
	 * @param resp
	 */
	public boolean doLogout(Cookie cookie,HttpServletResponse resp){
		try{
			cookie.setMaxAge(0);
			resp.addCookie(cookie);
			// remove the session id
			if(getSessionValue(cookie) != null
					&& getSessionValue(cookie).length() != 0)
				serverInstance.sessionInfoCMap.remove(getSessionValue(cookie));
			return true;
		}catch(Exception e){
			return false;
		}
	}

	/**
	 * To handle the post http request
	 * 
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void doPost(HttpServletRequest req,HttpServletResponse resp)
			throws ServletException,IOException{
		doGet(req,resp);
	}

}
