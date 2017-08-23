import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.api.client.util.DateTime;

public class UploadTest {

	private HashMap<String, MyFile> index = new HashMap<>();
	private GoogleConnector googleConnector;

	public UploadTest(GoogleConnector googleConnector) {
		this.googleConnector = googleConnector;
		index = googleConnector.index;
		//		listFiles(Constants.testFolder);
		makeMap(Constants.testFolder);
	}

	private static ArrayList<File> retList = new ArrayList<>();
	//	private HashMap<String, MyFile> myFileMap = new HashMap<>();

	private ConcurrentHashMap<String, MyFile> myFileMap = new ConcurrentHashMap<>();

	public void listFiles(File directory) {

		File[] listOfFiles = directory.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String fileName = listOfFiles[i].getName();
				//				System.out.println("File " + fileName);
				retList.add(listOfFiles[i]);
			} else if (listOfFiles[i].isDirectory()) {
				//				System.out.println("Directory " + listOfFiles[i].getName());
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
				//				System.out.println("File " + fileName);
				retList.add(listOfFiles[i]);
				boolean notFound = true;
				for (Map.Entry<String, MyFile> s : index.entrySet()) {
					if (s.getValue().getName().equals(fileName)) {
						//						MyFile myFile = new MyFile(listOfFiles[i].getName(), new DateTime(listOfFiles[i].lastModified()), listOfFiles[i].getAbsolutePath());
						MyFile myFile = new MyFile(listOfFiles[i].getName(), s.getValue().getLastModified(), listOfFiles[i].getAbsolutePath());
						myFileMap.put(s.getKey(), myFile);
					}
				}
				if (!index.values().toString().contains(fileName)) {
					MyFile myFile = new MyFile(listOfFiles[i].getName(), new DateTime(listOfFiles[i].lastModified()), listOfFiles[i].getAbsolutePath());			
					myFileMap.put("NotInDrive" + fileName, myFile);						
				}
			} else if (listOfFiles[i].isDirectory()) {
				String directoryName = listOfFiles[i].getName();
				//				System.out.println("Directory " + directoryName);
				for (Map.Entry<String, MyFile> s : index.entrySet()) {
					if (s.getValue().getName().equals(directoryName)) {
						//						MyFile myFile = new MyFile(listOfFiles[i].getName(), new DateTime(listOfFiles[i].lastModified()), listOfFiles[i].getAbsolutePath());	
						MyFile myFile = new MyFile(listOfFiles[i].getName(), s.getValue().getLastModified(), listOfFiles[i].getAbsolutePath());
						myFileMap.put(s.getKey(), myFile);						
						makeMap(listOfFiles[i]);
					}
				}
				if (!index.values().toString().contains(directoryName)) {
					MyFile myFile = new MyFile(listOfFiles[i].getName(), new DateTime(listOfFiles[i].lastModified()), listOfFiles[i].getAbsolutePath());
					myFileMap.put("NotInDrive" + directoryName, myFile);
				}
			}
		}
	}

	public void checkForChanges() {
		makeMap(Constants.testFolder);
		Iterator<Entry<String, MyFile>> it = myFileMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, MyFile> m = it.next();
			//			System.out.println(m.getKey() + " " + m.getValue().toString());
			if (!m.getKey().startsWith("NotInDrive")) { // File IS in drive
				MyFile mf = m.getValue();
				File f = new File(mf.getFilePath());
				if (!f.exists()) { // File name changed or file deleted
					try {
						googleConnector.delete(m.getKey());
						System.out.println("deleted file " + mf.getName());
						it.remove();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (mf.getLastModified().getValue() < f.lastModified()) { // File contents changed
					System.out.println("changed file " + f.getName());
					MyFile newFile = new MyFile(mf.getName(), new DateTime(f.lastModified()), mf.getFilePath());
					it.remove();
					googleConnector.updateFile(m.getKey(), f);
					System.out.println("this is an upload1");
					myFileMap.put(m.getKey(), newFile);	
				} 
			} else {
				it.remove();
				MyFile mf = m.getValue();
				//				System.out.println(mf.toString());
				File f = new File(mf.getFilePath());
				if (!f.isDirectory()) {
					String s = null;
					try {
						s = googleConnector.put(f);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (s != null) {
						MyFile newFile = new MyFile(mf.getName(), new DateTime(f.lastModified()), mf.getFilePath());	
						myFileMap.put(s, newFile);
						System.out.println("uploaded file " + f.getName());
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		//		listFiles(Constants.testFolder);
		//		System.out.println(retList.toString());
	}
}