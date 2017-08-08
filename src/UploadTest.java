import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadTest {

	private HashMap<String, MyFile> index = new HashMap<>();

	public UploadTest(GoogleConnector googleConnector) {
		index = googleConnector.index;
//		listFiles(Constants.testFolder);
//		System.out.println("this is index to start " + index.values().toString());
		makeMap(Constants.testFolder);
		System.out.println(fileMap.entrySet().toString());
	}

	private static ArrayList<File> retList = new ArrayList<>();
	private static HashMap<String, File> fileMap = new HashMap<>();
	

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
					}
				}
				if (!index.values().toString().contains(fileName)) {
					fileMap.put("NotInDrive" + fileName, listOfFiles[i]);
				}
			} else if (listOfFiles[i].isDirectory()) {
				String directoryName = listOfFiles[i].getName();
				System.out.println("Directory " + directoryName);
				for (Map.Entry<String, MyFile> s : index.entrySet()) {
					if (s.getValue().getName().equals(directoryName)) {
						fileMap.put(s.getKey(), listOfFiles[i]);
						makeMap(listOfFiles[i]);
					}
				}
				if (!index.values().toString().contains(directoryName)) {
					fileMap.put("NotInDrive" + directoryName, listOfFiles[i]);
				}
			}
		}
	}
	
	public void checkForChanges() {
		
	}

	
	public static void main(String[] args) {
		//		listFiles(Constants.testFolder);
		System.out.println(retList.toString());
	}
}
