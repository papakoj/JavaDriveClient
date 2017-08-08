import com.google.api.client.util.DateTime;

public class MyFile {
	
	private String name;
	private DateTime lastModified;
	
	public MyFile(String name, DateTime lastModified) {
		this.name = name;
		this.lastModified = lastModified;
	}
	
	public String getName() {
		return name;
	}
	
	public DateTime getLastModified() {
		return lastModified;
	}
	
	public String toString() {
		return name + ": " + lastModified.toString();
	}
}
