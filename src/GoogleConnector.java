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

	public GoogleConnector() throws IOException {
		this.service = Quickstart.getDriveService();
	}
	

	public List<File> list(String prefix) throws IOException{
		FileList result = this.service.files().list()
				.setQ("name contains '" + prefix + "'")
				.setFields("nextPageToken, files(id, name)")
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

	public InputStream get1(String key) throws IOException{
		throw new UnsupportedOperationException("Method get1 is not supported by GDrive");
	}

	public InputStream get(String key) throws IOException{
		return service.files().get(key).executeMediaAsInputStream();
	}

	public Long get(String key, java.io.File tempFile, String type) throws IOException{
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
			return getGDocs(key, tempFile, type);
		}
	}

	public Long getGDocs(String key, java.io.File tempFile, String type) throws IOException {
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

}