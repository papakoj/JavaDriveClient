import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
//import org.apache.http.message.BasicNameValuePair;
//import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.collect.ImmutableMap;

public class GoogleOauthServer implements Runnable {

	private Server server = new Server(8089);

	private final String clientId = "674687944100-u4kb04n3he0bf1ov7i9lvbjvdjibnfth.apps.googleusercontent.com";
	private final String clientSecret = "LpktuURsaOVJ9C3bLt0cn0FV";
	
	private static final java.io.File DATA_STORE_DIR = new java.io.File(
			System.getProperty("user.home"), ".credentials/drive-java-client");

	public static boolean isDone = false;
	
	public static void main(String[] args) throws Exception {
		new GoogleOauthServer().startJetty();
	}

	public String startJetty() throws Exception {

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("./");
		server.setHandler(context);
		server.setStopTimeout(3000L);

		// map servlets to endpoints
		context.addServlet(new ServletHolder(new SigninServlet()),"/signin");        
		context.addServlet(new ServletHolder(new CallbackServlet()),"/callback");        

		server.start();
		openWebpage(URI.create("http:localhost:8089/signin"));
//		server.join();
		while (!isDone) { Thread.sleep(1000); }
		System.out.println("finished");
		server.stop();
		return "finished";
	}
	
    public static void openWebpage(URI uri) {
    	if (Desktop.isDesktopSupported()) {
    		try {
				Desktop.getDesktop().browse(uri);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	} else {
    		Runtime runtime = Runtime.getRuntime();
    		try {
				runtime.exec("xdg-open http://" + uri.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}	
    }

	class SigninServlet extends HttpServlet {
		List<String> scopes = Arrays.asList("openid", "email", "https://www.googleapis.com/auth/drive.appdata", "https://www.googleapis.com/auth/drive.scripts", "https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/drive.metadata.readonly", "https://www.googleapis.com/auth/drive.readonly", "https://www.googleapis.com/auth/drive", "https://www.googleapis.com/auth/drive.metadata", "https://www.googleapis.com/auth/drive.photos.readonly");
		
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,IOException {
			// redirect to google for authorization
			StringBuilder oauthUrl = new StringBuilder().append("https://accounts.google.com/o/oauth2/auth")
					.append("?client_id=").append(clientId) // the client id from the api console registration
					.append("&response_type=code")
					.append("&redirect_uri=http://localhost:8089/callback") // the servlet that google redirects to after authorization
//					.append("&state=this_can_be_anything_to_help_correlate_the_response%3Dlike_session_id")
					.append("&scope=openid%20profile%20email%20https://www.googleapis.com/auth/drive%20https://www.googleapis.com/auth/drive.metadata%20https://www.googleapis.com/auth/drive.file%20https://www.googleapis.com/auth/drive.metadata")
					.append("&access_type=offline") // here we are asking to access to user's data while they are not signed in
					.append("&approval_prompt=force"); // this requires them to verify which account to use, if they are already signed in

			resp.sendRedirect(oauthUrl.toString());
		} 
	}

	class CallbackServlet extends HttpServlet {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,IOException {
			// google redirects with
			//http://localhost:8089/callback?state=this_can_be_anything_to_help_correlate_the_response%3Dlike_session_id&code=4/ygE-kCdJ_pgwb1mKZq3uaTEWLUBd.slJWq1jM9mcUEnp6UAPFm0F2NQjrgwI&authuser=0&prompt=consent&session_state=a3d1eb134189705e9acf2f573325e6f30dd30ee4..d62c

			// if the user denied access, we get back an error, ex
			// error=access_denied&state=session%3Dpotatoes

			if (req.getParameter("error") != null) {
				resp.getWriter().println(req.getParameter("error"));
				return;
			}

			// google returns a code that can be exchanged for a access token
			String code = req.getParameter("code");

			// get the access token by post to Google
			String body = post("https://accounts.google.com/o/oauth2/token", ImmutableMap.<String,String>builder()
					.put("code", code)
					.put("client_id", clientId)
					.put("client_secret", clientSecret)
					.put("redirect_uri", "http://localhost:8089/callback")
					.put("grant_type", "authorization_code").build());
			System.out.println(body);
			
			try(  PrintWriter out = new PrintWriter(DATA_STORE_DIR)  ){
			    out.println(body);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			JSONObject jsonObject = null;

			// get the access token from json and request info from Google
			try {
				jsonObject = (JSONObject) new JSONParser().parse(body);
			} catch (ParseException e) {
				throw new RuntimeException("Unable to parse json " + body);
			}

			// google tokens expire after an hour, but since we requested offline access we can get a new token without user involvement via the refresh token
			String accessToken = (String) jsonObject.get("access_token");

			// you may want to store the access token in session
			req.getSession().setAttribute("access_token", accessToken);

			// get some info about the user with the access token
			String json = get(new StringBuilder("https://www.googleapis.com/oauth2/v1/userinfo?access_token=").append(accessToken).toString());

			// now we could store the email address in session

			// return the json of the user's basic info
			
			resp.getWriter().println("Thanks for authorizing. You can close this tab now.");
		    isDone = true;
		}
	}

	// makes a GET request to url and returns body as a string
	public String get(String url) throws ClientProtocolException, IOException {
		return execute(new HttpGet(url));
	}

	// makes a POST request to url with form parameters and returns body as a string
	public String post(String url, Map<String,String> formParameters) throws ClientProtocolException, IOException { 
		HttpPost request = new HttpPost(url);

		List <NameValuePair> nvps = new ArrayList <NameValuePair>();

		for (String key : formParameters.keySet()) {
			nvps.add(new BasicNameValuePair(key, formParameters.get(key))); 
		}

		request.setEntity(new UrlEncodedFormEntity(nvps));

		return execute(request);
	}

	// makes request and checks response code for 200
	private String execute(HttpRequestBase request) throws ClientProtocolException, IOException {
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse response = httpClient.execute(request);

		HttpEntity entity = response.getEntity();
		String body = EntityUtils.toString(entity);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Expected 200 but got " + response.getStatusLine().getStatusCode() + ", with body " + body);
		}

		return body;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			main(null);
			while (!Thread.currentThread().isInterrupted()) {}
			System.out.println("done");
			return;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}