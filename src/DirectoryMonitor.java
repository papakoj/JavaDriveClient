
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class DirectoryMonitor implements AutoCloseable, Runnable {
	private volatile boolean stopped = false;
	private final CountDownLatch latch = new CountDownLatch(1);
	private MonitorService service;

	public DirectoryMonitor(Path dir) {
		this.service = new MonitorService(this, dir);
	}

	@Override
	public void close() throws Exception {
		this.stopped = true;
		this.latch.countDown();
	}
	public void start(){
		this.service.start();
	}

	public void await() throws InterruptedException{
		this.latch.await();
	}
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException{
		return this.latch.await(timeout, unit);
	}

	public static void main(String[] args) {
		Path dir = Paths.get(Constants.testFolder.getAbsolutePath());
		try (DirectoryMonitor directoryMonitor = new DirectoryMonitor(dir)){
			directoryMonitor.start();
			directoryMonitor.await();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class MonitorService extends Thread {
//		private DirectoryMonitor service;
		private WatchService watcher;
		private Map<WatchKey, Path> keys;

		public MonitorService(DirectoryMonitor service, Path dir) {
			try {
				this.watcher = FileSystems.getDefault().newWatchService();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.keys = new HashMap<WatchKey, Path>();
			try {
				walkAndRegisterDirectories(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		/**
		 * Register the given directory with the WatchService; This function will be called by FileVisitor
		 */
		private void registerDirectory(Path dir) throws IOException {
			WatchKey key = dir.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
			this.keys.put(key, dir);
		}

		/**
		 * Register the given directory, and all its sub-directories, with the WatchService.
		 */
		private void walkAndRegisterDirectories(final Path start) throws IOException {
			// register directory and sub-directories
			Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					registerDirectory(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}

		private boolean isCompletelyWritten(File file) {
			RandomAccessFile stream = null;
			try {
				stream = new RandomAccessFile(file, "rw");
				return true;
			} catch (IOException e) {
				System.out.println("Skipping file " + file.getName() + " for this iteration due it's not completely written");
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						System.out.println("Exception during closing file " + file.getName());
					}
				}
			}
			return false;
		}


		/**
		 * Process all events for keys queued to the watcher
		 * @throws IOException 
		 * @throws MachinicServerException 
		 * @throws MachinicClientException 
		 */
//		@SuppressWarnings("deprecation")
		void processEvents() throws IOException {
			// wait for key to be signaled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}
			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				return;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				@SuppressWarnings("unchecked")
				Path name = ((WatchEvent<Path>)event).context();
				Path child = dir.resolve(name);
				File f = child.toFile();
				@SuppressWarnings("rawtypes")
				WatchEvent.Kind kind = event.kind();
				if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
					// if directory is created, and watching recursively, then register it and its sub-directories
//					File f = child.toFile();
					if (isCompletelyWritten(f)) {
						//						if
					}

					try {
						if (Files.isDirectory(child)) {
							walkAndRegisterDirectories(child);
						}
					} catch (IOException x) {
						// do something useful
					}
				} else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {


				} else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {

				}
				//				 print out event
				if (isCompletelyWritten(f)) {
					System.out.format("%s: %s\n", event.kind().name(), child);
				}
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);
				// all directories are inaccessible
				if (keys.isEmpty()) {
					return;
				}
			}
		}

		public void run() {
			while(!stopped) {
				try {
					processEvents();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Path dir = Paths.get(Constants.testFolder.getAbsolutePath());
		try (DirectoryMonitor directoryMonitor = new DirectoryMonitor(dir)){
			directoryMonitor.start();
			directoryMonitor.await();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}