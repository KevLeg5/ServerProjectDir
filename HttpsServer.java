import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.concurrent.*;
import javax.net.ssl.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.nio.file.Paths;
import java.nio.file.Files;

public class HttpsServer implements Runnable {
	private static final String DEFAULT_PAGE = "index.html";
	private static final String ROOT_DIR = "RootDir";
	private static final int MAX_LINES_FOR_FILE = 200;
	public static Logger excLogger = Logger.getLogger("Exception");

	public void run() {
		
		// Initial parameters
		int port = 443;
		String keystorePath = "RootDir/mykey.keystore";
		String keystorePassword = "mypassword";

		// Setting property for SSL keystore and keystore password
		System.setProperty("javax.net.ssl.keyStore", keystorePath);
		System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);

		// creating loggers and file handlers
		Logger userLogger = Logger.getLogger("User Interaction");
		Logger closeLogger = Logger.getLogger("Closed Connection");
		FileHandler fhExc;
		FileHandler fhUser;
		FileHandler fhClose;
		SimpleFormatter formatter = new SimpleFormatter();
		SimpleFormatter formatter2 = new SimpleFormatter();
		SimpleFormatter formatter3 = new SimpleFormatter();

		try {

			// SSLContext for handling SSL/TLS communication
			SSLContext sslContext = SSLContext.getInstance("SSL");
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");

			// KeyStore for managing keys / certificates,
			KeyStore keyStore = KeyStore.getInstance("JKS");

			// Loading keystore file
			FileInputStream keystoreFile = new FileInputStream(keystorePath);

			keyStore.load(keystoreFile, keystorePassword.toCharArray());
			keystoreFile.close();

			// Make KeyManagerFactory
			keyManagerFactory.init(keyStore, keystorePassword.toCharArray());
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
			trustManagerFactory.init(keyStore);

			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			// Creating SSLServerSocketFactory and SSLServerSocket
			SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();
			SSLServerSocket serverSocket = (SSLServerSocket) socketFactory.createServerSocket(port);
			// Cast the ExecutorService to a ThreadPoolExecutor to reduce the keep-alive time and limit thread executions since
			// we do not have super-computers---and RAM resources become very limited if the large demo downloads are attempted. :P
			ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool(); 
			threadPool.setKeepAliveTime(2L, TimeUnit.SECONDS); // After finishing executions, threads may idle for max 2 seconds.
			threadPool.setCorePoolSize(0); // At no requests, close all threads.
			threadPool.setMaximumPoolSize(20); // Maximum 20 threads at any time
			// Setting the need for client authentication
			serverSocket.setNeedClientAuth(false);
			serverSocket.setSoTimeout(2000);

			//assign files to loggers
			fhExc = new FileHandler("RootDir/Logs/exceptions.txt", true);
			fhUser = new FileHandler("RootDir/Logs/interaction.txt", true);
			fhClose = new FileHandler("RootDir/Logs/close_socket.txt", true);
			fhExc.setFormatter(formatter);
    		fhUser.setFormatter(formatter2);
			fhClose.setFormatter(formatter3);
			excLogger.addHandler(fhExc);
			userLogger.addHandler(fhUser);
			closeLogger.addHandler(fhClose);
			excLogger.setUseParentHandlers(false);
			userLogger.setUseParentHandlers(false);
			closeLogger.setUseParentHandlers(false);

			// *** Main server loop ***
			System.out.println("Server is running... Go here: https://localhost:443");
			while (LoginGUI.runServer) {
				Socket socket = null;

				// checking the size of file every iteration
				String fileDir = "RootDir/Logs/interaction.txt";
				checkLines(fileDir);
				String filePath = "RootDir/Logs/exceptions.txt";
				checkLines(filePath);
				String filePath2 = "RootDir/Logs/close_socket.txt";
				checkLines(filePath2);

				try{
					socket = serverSocket.accept();	
					// Thread HTTP Integration
					Runnable runnableThread = new ThreadHTTP(new File(ROOT_DIR), DEFAULT_PAGE, socket);
					threadPool.submit(runnableThread);
					userLogger.log(Level.INFO, "User Interaction");
				} catch(Exception e) {
					System.gc();
					String exception = e.toString();
					excLogger.log(Level.WARNING, exception);
				} 
			}
			threadPool.shutdownNow();
			serverSocket.close();
			System.out.println("Server is stopped");
			closeLogger.log(Level.INFO, "Client Connection has been closed");

		} catch (Exception e) {
			//e.printStackTrace();

			String exception = e.toString();
			excLogger.log(Level.WARNING, exception);
		}
	}

	public static void checkLines(String filename) throws IOException {
	    long lines = 0;
	    String line;

	    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
	        while ((line = reader.readLine()) != null) {
	            lines++;
	            if (lines > MAX_LINES_FOR_FILE) {
	                BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename));
					writer.write("");
					writer.flush();
	                break; // No need to continue reading lines after clearing
	            }
	        }
	    }
	    catch (Exception E) {
	    	E.printStackTrace();
	    }
	}
}
