import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.api.client.util.DateTime;

public class UploadTest {

	private HashMap<String, MyFile> index = new HashMap<>();
	private GoogleConnector googleConnector;

	public UploadTest(GoogleConnector googleConnector) {
		this.googleConnector = googleConnector;
		index = googleConnector.index;
		//		listFiles(Constants.testFolder);
		makeMap(Constants.testFolder);
		System.out.println(fileMap.entrySet().toString());
	}

	private static ArrayList<File> retList = new ArrayList<>();
	private static HashMap<String, File> fileMap = new HashMap<>();
	private static HashMap<String, MyFile> myFileMap = new HashMap<>();


	public void listFiles(File directory) {

		File[] listOfFiles = directory.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String fileName = listOfFiles[i].getName();
				System.out.println("File " + fileName);
				retList.add(listOfFiles[i]);
				for (MyFile s : index.values()) {
					if (s.getName().equals(fileName)) {
					}
				}
			} else if (listOfFiles[i].isDirectory()) {
				System.out.println("Directory " + listOfFiles[i].getName());
				retList.add(listOfFiles[i]);
				listFiles(listOfFiles[i]);
			}
		}
	}

	public void makeMap(File directory) {

		File[] listOfFiles = directory.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String fileName = listOfFiles[i].getName();
				System.out.println("File " + fileName);
				retList.add(listOfFiles[i]);
				for (Map.Entry<String, MyFile> s : index.entrySet()) {
					if (s.getValue().getName().equals(fileName)) {
						fileMap.put(s.getKey(), listOfFiles[i]);
						MyFile myFile = new MyFile(listOfFiles[i].getAbsolutePath(), new DateTime(listOfFiles[i].lastModified()));
						myFileMap.put(s.getKey(), myFile);						
					}
				}
				if (!index.values().toString().contains(fileName)) {
					fileMap.put("NotInDrive" + fileName, listOfFiles[i]);
					MyFile myFile = new MyFile(listOfFiles[i].getAbsolutePath(), new DateTime(listOfFiles[i].lastModified()));
					myFileMap.put("NotInDrive" + fileName, myFile);						

				}
			} else if (listOfFiles[i].isDirectory()) {
				String directoryName = listOfFiles[i].getName();
				System.out.println("Directory " + directoryName);
				for (Map.Entry<String, MyFile> s : index.entrySet()) {
					if (s.getValue().getName().equals(directoryName)) {
						fileMap.put(s.getKey(), listOfFiles[i]);
						MyFile myFile = new MyFile(listOfFiles[i].getAbsolutePath(), new DateTime(listOfFiles[i].lastModified()));
						myFileMap.put(s.getKey(), myFile);						
						makeMap(listOfFiles[i]);
					}
				}
				if (!index.values().toString().contains(directoryName)) {
					fileMap.put("NotInDrive" + directoryName, listOfFiles[i]);
					MyFile myFile = new MyFile(listOfFiles[i].getAbsolutePath(), new DateTime(listOfFiles[i].lastModified()));
					myFileMap.put("NotInDrive" + directoryName, myFile);						
				}
			}
		}
	}

	public void checkForChanges() {
		Iterator<Entry<String, MyFile>> it = myFileMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, MyFile> m = it.next();
			if (!m.getKey().startsWith("NotInDrive")) {
				MyFile mf = m.getValue();
				File f = new File(mf.getName());
				if (mf.getLastModified().getValue() < f.lastModified()) {
					System.out.println("changed file " + f.getName());
					MyFile newFile = new MyFile(mf.getName(), new DateTime(f.lastModified()));	
					myFileMap.put(m.getKey(), newFile);
				}
			} else {
				MyFile mf = m.getValue();
				File f = new File(mf.getName());
				if (!f.isDirectory()) {
					String s = null;
					try {
						s = googleConnector.put(f);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (s != null) {
						myFileMap.remove(m.getKey());
						MyFile newFile = new MyFile(mf.getName(), new DateTime(f.lastModified()));	
						myFileMap.put(s, newFile);
						System.out.println("uploaded file" + f.getName());
					}
				}
			}
		}
	}


	public static void main(String[] args) {
		//		listFiles(Constants.testFolder);
		System.out.println(retList.toString());
	}
}
