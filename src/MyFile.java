import com.google.api.client.util.DateTime;

public class MyFile {
	
	private String name;
	private DateTime lastModified;
	private String filePath;
	
	public MyFile(String name, DateTime lastModified) {
		this.name = name;
		this.lastModified = lastModified;
		this.filePath = null;
	}
	
	public MyFile(String name, DateTime lastModified, String filePath) {
		this.name = name;
		this.lastModified = lastModified;
		this.filePath = filePath;
	}
	
	public String getName() {
		return name;
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public DateTime getLastModified() {
		return lastModified;
	}
	
	public String toString() {
		return name + ": " + lastModified.toString() + "\nFilepath: " + filePath;
	}
}
