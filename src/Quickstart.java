import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;

import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;

import com.google.api.services.drive.Drive;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Quickstart {


	/** Application name. */
	private static final String APPLICATION_NAME =
			"Drive API Java Quickstart";

	/** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(
			System.getProperty("user.home"), ".credentials/drive-java-client");

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY =
			JacksonFactory.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	/** Global instance of the scopes required by this quickstart.
	 *
	 * If modifying these scopes, delete your previously saved credentials
	 * at ~/.credentials/drive-java-client
	 */
	private static final java.util.Collection<String> SCOPES =
			DriveScopes.all();
	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	private static Drive service;
	private static GoogleConnector gC;
	

	public Quickstart() {
		try {
			this.service = getDriveService();
			this.gC = new GoogleConnector(service);
		} catch (IOException e) {
			System.out.println("welp we failed");;
		}
	}

	private static final String clientId = "674687944100-u4kb04n3he0bf1ov7i9lvbjvdjibnfth.apps.googleusercontent.com";
	private static final String clientSecret = "LpktuURsaOVJ9C3bLt0cn0FV";

	/**
	 * Creates an authorized Credential object.
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	public static Credential authorize() throws IOException {

		JSONParser parser = new JSONParser();

		Object obj = null;
		try {
			obj = parser.parse(new FileReader(DATA_STORE_DIR));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("No credentials found");
			System.out.println("Please re-authenticate the application");
		}
		JSONObject jsonObject = (JSONObject) obj;
		System.out.println(jsonObject);

		String accessToken = (String) jsonObject.get("access_token");

		String refreshToken = (String) jsonObject.get("refresh_token");

		GoogleCredential credential = 
				new GoogleCredential.Builder()
				.setTransport(HTTP_TRANSPORT)
				.setJsonFactory(JSON_FACTORY)
				.setClientSecrets(clientId, clientSecret).build();
		credential.setAccessToken(accessToken);
		credential.setRefreshToken(refreshToken);
		//		Credential credential = new AuthorizationCodeInstalledApp(
		//				flow, new LocalServerReceiver()).authorize("user");
		System.out.println(
				"Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
		return credential;
	}

	/**
	 * Build and return an authorized Drive client service.
	 * @return an authorized Drive client service
	 * @throws IOException
	 */
	public static Drive getDriveService() throws IOException {
		Credential credential = authorize();
		return new Drive.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME)
				.build();
	}

	public static void index() {
		gC.indexFiles();
	}
	
	public static void syncDrive(java.io.File storeDirectory) {
		try {
			List<File> rootList = gC.downloadRootFolders(storeDirectory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static void main(String[] args) throws IOException {
		// Build a new authorized API client service.
		//
		Drive service = getDriveService();

		// Print the names and IDs for up to 10 files.
		FileList result = service.files().list()
				.setFields("nextPageToken, files(createdTime,description,id,kind,modifiedTime,name,parents),kind")
				.execute();

		result.getClass();
		List<File> files = result.getFiles();
		if (files == null || files.size() == 0) {
			System.out.println("No files found.");
		} else {
			System.out.println("Files:");
			for (File file : files) {
				if (file.getParents() != null && file.getParents().contains("root"))
					System.out.printf("%s  %s (%s)\n", file.getName(), file.getParents(), file.getId());
			}
		}

		//        // Create a folder
		//        File folderMetadata = new File();
		//        folderMetadata.setName("RealTestFolder");
		//        folderMetadata.setMimeType("application/vnd.google-apps.folder");
		//
		//        File folder = service.files().create(folderMetadata)
		//        .setFields("id")
		//        .execute();
		//        String folderID = folder.getId(); 
		//        System.out.println("Folder ID: " + folderID);
		//              
		//        
		//        //Create file
		//        File fileMetadata = new File();
		//        fileMetadata.setMimeType("text/plain"); 
		//        fileMetadata.setName("text.txt");
		//        
		//        //Put file inside folder
		//        fileMetadata.setParents(Collections.singletonList(folderID));
		//        //Pick file to be uploaded
		//        java.io.File fileContent = new java.io.File("testFile.txt");
		//        FileContent mediaContent = new FileContent("text/csv", fileContent);
		//        File file = service.files().create(fileMetadata, mediaContent)
		//        .setFields("id, parents")
		//        .execute();
		//        String fileID = file.getId();
		//        System.out.println("File ID: " + fileID);
		//        
		//        //Download file
		//        String fileId = "0B9m8PB8iMl4hVU5oUENLejA3ak0";
		//        OutputStream outputStream = new FileOutputStream("rah.txt");
		//        service.files().get(fileId)
		//        .executeMediaAndDownloadTo(outputStream);   
		//    	GoogleConnector gC = new GoogleConnector();	
		//    	List<File> testList = gC.list("ACSU");
		//    	java.io.File test = new java.io.File("test.pdf");
		//    	gC.get("1MX1EkMRfcYtpRg58LBKqaRA3qYjVP_8FjfvjGaWwqio", test, "application/pdf");
	}
}