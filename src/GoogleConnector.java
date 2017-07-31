import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

//import utils.IOUtils;

public class GoogleConnector implements Connector<File> {

	private Drive service;

	public GoogleConnector(Drive service) throws IOException {
		this.service = service;
	}


	public List<File> list(String prefix) throws IOException{
		FileList result = this.service.files().list()
				.setQ("name contains '" + prefix + "'")
				.setFields("nextPageToken, files(id, name, parents)")
				.execute();
		List<File> files = result.getFiles();
		if (files == null || files.size() == 0) {
			System.out.println("No files found.");
		} else {
			System.out.println("Files:");
			for (File file : files) {
				//				System.out.printf("%s (%s)\n", file.getName(), file.getId());
			}
		}
		return files;
	}


	public List<File> downloadRootFolders(java.io.File storeDirectory) throws IOException {
		String id = service.files().get("root").setFields("id").execute().getId();
		System.out.println(id);
		FileList result = this.service.files().list()
				.setFields("nextPageToken, files(description,id,kind,mimeType,modifiedTime,name,parents,properties),kind")
				.execute();
		List<File> files = result.getFiles();
		if (files == null || files.size() == 0) {
			System.out.println("No files found.");
		} else {
			System.out.println("Files:");
			for (File file : files) {
				if (file.getParents() != null && file.getParents().toString().contains(id)) {
					System.out.printf("%s (%s)\n", file.getName(), file.getId());
					System.out.println(file.getMimeType());
					if (file.getMimeType().contains("folder")) {
						java.io.File g = new java.io.File(storeDirectory.getAbsolutePath() + "/" + file.getName());
						g.mkdirs();
						downloadFilesInFolder(file.getId(), g);
					} else {
						get(file, file.getId(), new java.io.File(storeDirectory.getAbsolutePath() + "/" + file.getName()), "text/plain");
					}
				}
			}
		}
		return files;
	}

	public List<File> downloadFilesInFolder(String id, java.io.File folderPath) throws IOException {
		FileList result = this.service.files().list()
				.setQ("'" + id + "' in parents")
				.setFields("nextPageToken, files(description,id,kind,mimeType,modifiedTime,name,parents,properties),kind")
				.execute();
		List<File> files = result.getFiles();
		if (files == null || files.size() == 0) {
			System.out.println("No files found.");
		} else {
			System.out.println("Files:");
			for (File file : files) {
				if (file.getMimeType().contains("folder")) {
					java.io.File g = new java.io.File(folderPath.getAbsolutePath() + "/" + file.getName());
					g.mkdirs();
					downloadFilesInFolder(file.getId(), g);
				} else {
					System.out.println(file.getName());
					get(file, file.getId(), new java.io.File(folderPath.getAbsolutePath() + "/" + file.getName()), "text/plain");
				}
			}
		}
		return files;
	}





	public InputStream get1(String key) throws IOException{
		throw new UnsupportedOperationException("Method get1 is not supported by GDrive");
	}

	public InputStream get(String key) throws IOException{
		return service.files().get(key).executeMediaAsInputStream();
	}

	public Long get(File file, String key, java.io.File tempFile, String type) throws IOException{
		System.out.println(service.files().get(key).execute().getMimeType());
		if (!service.files().get(key).execute().getMimeType().contains("google-apps")) {
			try (InputStream input = service.files().get(key).executeMediaAsInputStream()) {
				try (OutputStream outputStream = new FileOutputStream(tempFile)) {
					return (long) IOUtils.copy(input, outputStream);
				}
			} catch (IOException e) {
				// Log something
				throw e;
			}
		} else {
			return getGDocs(file, key, tempFile, type);
		}
	}

	public Long getGDocs(File file, String key, java.io.File tempFile, String type) throws IOException {	
		if (file.getMimeType().equals("application/vnd.google-apps.document")) {type = "application/vnd.oasis.opendocument.text";}
		if (file.getMimeType().equals("application/vnd.google-apps.drawing")) {type = "image/jpeg";}
		if (file.getMimeType().equals("application/vnd.google-apps.script")) {type = "application/vnd.google-apps.script+json";}
		if (file.getMimeType().equals("application/vnd.google-apps.presentation")) {type = "application/vnd.oasis.opendocument.presentation";}
		if (file.getMimeType().equals("application/vnd.google-apps.spreadsheet")) {type = "application/x-vnd.oasis.opendocument.spreadsheet";}
		try (InputStream input = service.files().export(key, type).executeMediaAsInputStream()) {
			try (OutputStream outputStream = new FileOutputStream(tempFile)) {
				return (long) IOUtils.copy(input, outputStream);
			}
		} catch (IOException e) {
			// Log something
			throw e;
		}
	}

	public String put(java.io.File localFile) throws IOException{
		File newFile = new File().setName(localFile.getName());
		FileContent mediaContent = new FileContent(null, localFile);
		String newID = service.files().create(newFile, mediaContent)
				.setFields("id")
				.execute().getId();
		System.out.println(newID);
		return newID;

	}  

	public String update(String key, java.io.File localFile) throws IOException{
		delete(key);
		return put(localFile);
	}


	public void delete(String key) throws IOException{
		try {
			this.service.files().delete(key).execute();
		} catch (GoogleJsonResponseException e) {
			System.out.println("Error: File not found");
		}

	}


	@Override
	public Long get(String key, java.io.File tempFile, String type) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Long getGDocs(String key, java.io.File tempFile, String type) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}