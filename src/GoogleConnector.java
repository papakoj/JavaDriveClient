import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

//import utils.IOUtils;

public class GoogleConnector implements Connector<File> {

	private boolean initialDownload;

	private Drive service;

	public HashMap<String, MyFile> index = new HashMap<>();


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
			for (File file : files) {
				System.out.printf("%s (%s)\n", file.getName(), file.getId());
			}
		}
		return files;
	}


	public List<File> downloadRootFolders(java.io.File storeDirectory) throws IOException {
		String id = service.files().get("root").setFields("id").execute().getId();
		FileList result = this.service.files().list()
				.setFields("nextPageToken, files(description,id,kind,mimeType,modifiedTime,name,parents,properties),kind")
				.execute();
		List<File> files = result.getFiles();
		if (files == null || files.size() == 0) {
			System.out.println("No files found.");
		} else {
			for (File file : files) {
				if (file.getParents() != null && file.getParents().toString().contains(id)) {
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
		initialDownload = false;
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
					get(file, file.getId(), new java.io.File(folderPath.getAbsolutePath() + "/" + file.getName()), "text/plain");
				}
			}
		}
		return files;
	}


	public InputStream get1(String key) throws IOException {
		throw new UnsupportedOperationException("Method get1 is not supported by GDrive");
	}


	public InputStream get(String key) throws IOException{
		return service.files().get(key).executeMediaAsInputStream();
	}


	public void indexFiles() {
		index.clear();
		initialDownload = true;
		FileList result = null;
		try {
			result = this.service.files().list()
					.setFields("nextPageToken, files(id,name,modifiedTime,mimeType)")
					.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<File> files = result.getFiles();
		if (files == null || files.size() == 0) {
			System.out.println("No files found.");
		} else {
			for (File file : files) {
				if (!file.getMimeType().contains("folder")) {
					if (!index.containsKey(file.getId())) {
						MyFile myFile = new MyFile(file.getName(), file.getModifiedTime());
						index.put(file.getId(), myFile);
					}
				}
			}
		}
//		System.out.println(index.entrySet().toString());
	}

	public Long get(File file, String key, java.io.File tempFile, String type) throws IOException{
		if (!service.files().get(key).execute().getMimeType().contains("google-apps")) {
			if (initialDownload || !index.containsKey(file.getId()) || (changedFile(file))) {
				try (InputStream input = service.files().get(key).executeMediaAsInputStream()) {
					try (OutputStream outputStream = new FileOutputStream(tempFile)) {
						System.out.println("downloading" + file.getName());
						long ret = IOUtils.copy(input, outputStream);
						MyFile myFile = new MyFile(file.getName(), file.getModifiedTime());
						index.put(file.getId(), myFile);
						return ret;
					}
				} catch (IOException e) {
					// Log something
					throw e;
				}
			}
		} else {
			return getGDocs(file, key, tempFile, type);
		}
		return null;
	}

	public Long getGDocs(File file, String key, java.io.File tempFile, String type) throws IOException {	
		if (file.getMimeType().equals("application/vnd.google-apps.document")) {type = "application/vnd.oasis.opendocument.text";}
		if (file.getMimeType().equals("application/vnd.google-apps.drawing")) {type = "image/jpeg";}
		if (file.getMimeType().equals("application/vnd.google-apps.script")) {type = "application/vnd.google-apps.script+json";}
		if (file.getMimeType().equals("application/vnd.google-apps.presentation")) {type = "application/vnd.oasis.opendocument.presentation";}
		if (file.getMimeType().equals("application/vnd.google-apps.spreadsheet")) {type = "application/x-vnd.oasis.opendocument.spreadsheet";}
		if (initialDownload || !index.containsKey(file.getId()) || (changedFile(file))) {
			try (InputStream input = service.files().export(key, type).executeMediaAsInputStream()) {
				try (OutputStream outputStream = new FileOutputStream(tempFile)) {
					long ret = IOUtils.copy(input, outputStream);
					System.out.println("downloading" + file.getName());
					MyFile myFile = new MyFile(file.getName(), file.getModifiedTime());
					index.put(file.getId(), myFile);
					return ret;
				}
			} catch (IOException e) {
				// Log something
				throw e;
			} 
		} 
		return null;
	}

	public boolean changedFile(File file) {
		MyFile f = index.get(file.getId());
		if (f.getLastModified().getValue() < file.getModifiedTime().getValue()) {
			//				System.out.println("changed" + file.getName());
			return true;
		}
		return false;
	}

	public String put(java.io.File localFile) throws IOException{
		File newFile = new File().setName(localFile.getName());
		FileContent mediaContent = new FileContent(null, localFile);
		String newID = service.files().create(newFile, mediaContent)
				.setFields("id")
				.execute().getId();
		return newID;

	}  

	public String update(String key, java.io.File localFile) {
		String ret = null;
		try {
			delete(key);
			ret = put(localFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public File updateFile(String key, java.io.File localFile) {
		String tempKey = null;
		try {
			delete(key);
			tempKey = put(localFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			// First retrieve the file from the API.
			File file = service.files().get(tempKey).execute();
			file.setId(key);
			// Send the request to the API.
			File updatedFile = service.files().update(key, file).execute();
			return updatedFile;
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
			return null;
		}
	}


	public String delete(String key) throws IOException{
		String s = null;
		try {
			this.service.files().delete(key).execute();
			s = "deleted";
		} catch (GoogleJsonResponseException e) {
			System.out.println("Error: File not found");
		}
		return s;
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