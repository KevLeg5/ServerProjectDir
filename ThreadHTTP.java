import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;

public class ThreadHTTP implements Runnable {
	private File serverRootDirectory;
	private String defaultPageName;
	private Socket connectionSocket;

	/**
	 * <h3>ThreadHTTP Constructor</h3>
	 * 
	 * <p>
	 * Constructs a ThreadHTTP runnable object
	 * <p>
	 * 
	 * @param serverRootDirectory the root directory from which the server is being run.
	 * @param defaultPageName     the name of the file to load when the user provides no input (typically index.html)
	 * @param connectionSocket    the socket between the server and the client for this thread to work with
	 */
	public ThreadHTTP(File serverRootDirectory, String defaultPageName, Socket connectionSocket) {
		this.serverRootDirectory = serverRootDirectory;
		this.defaultPageName = defaultPageName;
		this.connectionSocket = connectionSocket;
	}

	/**
	 * <h3>parseClientHttpHeader</h3>
	 * 
	 * <p>
	 * Reads from client stream until cr-lf terminator is read, signifying
	 * the end of the HTTP request header. (cr-lf = carriage return, line feed) 
	 * <br><br>
	 * The combined string is then split by whitespace, and the tokens are returned
	 * as a String array
	 * </p>
	 * 
	 * @param inReader The input stream Reader of the connection socket
	 * @return A string array of each token in the client HTTP request.
	 * @throws IOException - If an I/O error occurs
	 */
	private String[] parseClientHttpRequest(Reader inReader) throws IOException {
		int inputASCII = inReader.read(); // Read the first char, represented as an ASCII int
		StringBuilder userInputBuilder = new StringBuilder();
		while (inputASCII != '\r' && inputASCII != '\n') {
			userInputBuilder.append((char) inputASCII);
			inputASCII = inReader.read();
		}
		// Transform the stringbuilder into a string array. Each token seperated by space.
		String[] requestTokens = userInputBuilder.toString().split("\\s+");
		return requestTokens;
	}

	/**
	 * <h3>parseHttpHeaders</h3>
	 * 
	 * <p>
	 * Call this after parsing HTTP request line. Reads from client stream until two (cr-lf) characters
	 * are read in a row, signifying an empty line (which is the end of the header field)
	 * <br><br>
	 * Headers and values are placed into key-value hash map pairs, with colon removed. EX: "Content-Length: 42"
	 * becomes stored in the hash map as "Content-Length" -> "42"
	 * </p>
	 * 
	 * @param inReader The input stream Reader of the connection socket
	 * @return a hash map of header key-value pairs
	 * @throws IOException - If an I/O error occurs
	 */
	private HashMap<String, String> parseHttpHeaders(Reader inReader) throws IOException {
		HashMap<String, String> map = new HashMap<>();

		StringBuilder currStr = new StringBuilder();

		currStr.append((char) inReader.read());
		// Advance the reader forwards until we reach the body, signified by two carriage returns or newlines
		// Read chars and put into a string until the body is reached.
		while (!currStr.toString().equals("\r\n")) { // if string == \r\n, we've reached end of headers
			if (currStr.toString().endsWith(": ")) {
				String key = currStr.substring(0, currStr.length() - 2); // get rid of the key's colon & space
				currStr = new StringBuilder(); // now get the value
				while (!currStr.toString().endsWith("\r\n")) {
					currStr.append((char) inReader.read());
				}
				String val = currStr.substring(0, currStr.length() - 2); // get rid of key newline and carriage return
				map.put(key, val);
				currStr = new StringBuilder();
			}
			currStr.append((char) inReader.read());
		}
		return map;
	}

	/**
	 * <h1>Gets the body of an HTTP request.</h1>
	 * 
	 * <p>
	 * Call after parsing HTTP headers. Reads the body of the message, given by the length 
	 * of the body specified in the HTTP "Content-Length" header.
	 * </p>
	 * 
	 * @param inReader The input stream Reader of the connection socket
	 * @return The body as a string
	 * @throws IOException - If an I/O error occurs
	 */
	private String getClientHttpBody(Reader inReader, HashMap<String, String> parsedHttpHeaders) throws IOException {
		int bodyLength = Integer.parseInt(parsedHttpHeaders.get("Content-Length"));
		StringBuilder bodyBuilder = new StringBuilder();
		for(int charsRead = 0; charsRead < bodyLength; charsRead++) {
			bodyBuilder.append((char)inReader.read());
		}
		return bodyBuilder.toString();
	}

	/**
	 * <h3>sendHTTPHeader</h3>
	 * 
	 * <p>
	 * Sends out an HTTP response header that precedes the message body content
	 * </p>
	 * 
	 * @param requestedFileType the MIME type of the requested file
	 * @param bodyLength the byte length of the requested file
	 * @param httpResponse the HTTP response line for the transmitted message
	 * @param outWriter a Write object tied to the connection socket for sending the header.
	 * @throws IOException - If an I/O error occurs
	 */
	private void sendResponseHEAD(String requestedFileType, int bodyLength, String httpResponse, Writer outWriter)
			throws IOException {
		Date currentDate = new Date();
		outWriter.write(httpResponse + "\r\n");
		outWriter.write("Date: " + currentDate + "\r\n");
		outWriter.write("Server: Ryan's humble thread from Joey's kingdom\r\n");
		outWriter.write("Content-length: " + bodyLength + "\r\n");
		outWriter.write("Content-type: " + requestedFileType + "\r\n\r\n");
		outWriter.flush();
	}

	/**
	 * <h3>sendResponseHEADBODY</h3>
	 * 
	 * <p>
	 * Sends both the header and body of the requested file.
	 * </p>
	 * 
	 * @param requestedFileType MIME type of file
	 * @param requestedFileByteData the raw byte data of the file
	 * @param outBufStream a byte output stream connected to the socket. We must use bytes because content may not be a string.
	 * @param outWriter a Write object tied to the same byte output stream above, used for sending the header.
	 * @throws IOException - If an I/O error occurs
	 */
	private void sendResponseHEADBODY(String requestedFileType, byte[] requestedFileByteData, 
									  OutputStream outBufStream, Writer outWriter) throws IOException {
		
		sendResponseHEAD(requestedFileType, requestedFileByteData.length, "HTTP/1.0 200 OK", outWriter);
		// We need to use the out-stream instead of the Writer object for body
		// transmission because the requested file might not be a text document
		outBufStream.write(requestedFileByteData);
		outBufStream.flush();
	}

	/**
	 * <h3>sendResponseFileNotFound</h3>
	 * 
	 * <p>
	 * Sends a 404 error response due to not finding a requested file.
	 * </p>
	 * 
	 * @param outWriter the writer object connected to the socket for which to send the error response through.
	 * @throws IOException - If an I/O error occurs
	 */
	private void sendResponseFileNotFound(Writer outWriter) throws IOException {
		// Send an error header
		String body = new StringBuilder("<HTML>\r\n")
				.append("<HEAD><TITLE>File Not Found</TITLE>\r\n</HEAD>\r\n")
				.append("<BODY>")
				.append("<H1>HTTP Error 404: File Not Found :P</H1>\r\n")
				.append("</BODY></HTML>\r\n").toString();

		sendResponseHEAD("text/html; charset=utf-8", body.toString().getBytes().length, "HTTP/1.0 404 File Not Found", outWriter);
		outWriter.write(body);
		outWriter.flush();
	}

	/**
	 * <h3>processClientHTTPRequest</h3>
	 * 
	 * <p>
	 * Processes and responds to the client's HTTP request using pre-parsed HTTP request line and headers.
	 * <br><br>
	 * Supports GET, HEAD, and POST requests for all file types on the server.
	 * </p>
	 * 
	 * @param outBufStream The byte output stream connected to the socket
	 * @param outWriter The Writer object output stream connected to the socket (for easy String writing)
	 * @param inReader The input Reader object connected to the socket for reading HTTP request
	 * @param rootPath The root path of the server
	 * @param parsedClientHttpRequest The parsed array of the HTTP request line
	 * @param parsedHttpHeaders The parsed hash map of each header in key-value pair format.
	 * @throws IOException - If an I/O error occurs
	 */
	public void processClientHTTPRequest(OutputStream outBufStream, Writer outWriter, Reader inReader, String rootPath,
										 String[] parsedClientHttpRequest, HashMap<String, String> parsedHttpHeaders) throws IOException {

		String methodCommand = parsedClientHttpRequest[0]; 		// HTTP request format: Method | URL | Version \cr-lf
		String URL = parsedClientHttpRequest[1];
		if (URL.endsWith("/")) {
			URL += defaultPageName; // If user does not specify a file, load the default page
		} 
		// server root will probably not be system root, so we need to strip the "/"
		// preceding the file name from the URL & make file relative to server root
		File requestedFile = new File(serverRootDirectory, URL.substring(1, URL.length()));
		// Make sure if the user has included /../../.. etc in the path, we don't allow them to get out of the server directory.
		if (requestedFile.canRead() && requestedFile.getCanonicalPath().startsWith(serverRootDirectory.getCanonicalPath())) {
			// *******************************
			// **** GET, HEAD, POST **********
			// *******************************
			if (methodCommand.equals("GET") || methodCommand.equals("HEAD")) {
				// Read the requested file on the server and store in byte array for out stream
				// transmission.
				String requestedFileType = URLConnection.getFileNameMap().getContentTypeFor(requestedFile.getName());
				byte[] requestedFileByteData = Files.readAllBytes(requestedFile.toPath());

				// send head and body (get) or just head depending on user request
				if (methodCommand.equals("GET")) {
					sendResponseHEADBODY(requestedFileType, requestedFileByteData, outBufStream, outWriter);
				} else if (methodCommand.equals("HEAD")) {
					sendResponseHEAD(requestedFileType, requestedFileByteData.length, "HTTP/1.0 200 OK", outWriter);
				}
			} else if (methodCommand.equals("POST")) {
				String clientBody = getClientHttpBody(inReader, parsedHttpHeaders); // Reads user-sent HTML form
				// Builds a php command to be sent to another thread for command-line execution
				ProcessBuilder cgiProcessBuilder = null;
				String operatingSystem = System.getProperty("os.name");
				System.out.println(serverRootDirectory.getCanonicalPath());
				// Execute bundled php.exe if Windows, or php installed on system PATH if other OS (requires manual PATH installation).
				if (operatingSystem.startsWith("Windows")) {
					cgiProcessBuilder = new ProcessBuilder(serverRootDirectory.getCanonicalPath() + "\\phpWin\\php.exe",
							requestedFile.getCanonicalPath(), clientBody);
				} else {
					cgiProcessBuilder = new ProcessBuilder("php", requestedFile.getCanonicalPath(), clientBody);
				}
				// cgiProcessBuilder.redirectErrorStream(true); // for troubleshooting, re-direct errors to this thread
				cgiProcessBuilder.directory(serverRootDirectory);
				Process cgiProcess = cgiProcessBuilder.start();

				// Read in output from running the php command
				BufferedReader stdInput = new BufferedReader(new InputStreamReader(cgiProcess.getInputStream()));
				int phpOutputASCII;
				StringBuilder responseBuilder = new StringBuilder();
				while ((phpOutputASCII = stdInput.read()) != -1) {
					responseBuilder.append((char) phpOutputASCII);
				}
				
				// Send a header & body HTML response with the php script's returned output.
				sendResponseHEADBODY("text/html", responseBuilder.toString().getBytes(), outBufStream, outWriter);
			} else {
				System.out.println("The HTTP method requested is not implemented.");
			}
		} else {
			sendResponseFileNotFound(outWriter); // could not find file, or user requested file outside of directory
		}
	}

	/**
	 * <h3>ThreadHTTP Run (Runnable implementation)</h3>
	 * 
	 * <p>
	 * Reads in data from the socket provided during thread construction, processes
	 * client HTTP request, and then returns an HTTP response. Supported HTTP methods are GET, HEAD, POST
	 * </p>
	 */
	@Override
	public void run() {
		String rootPath = serverRootDirectory.getPath(); // Convert the path to a usable String

		try (
				// Create (byte) streams for incoming reads and outgoing writes
				InputStream inBufStream = new BufferedInputStream(connectionSocket.getInputStream());
				OutputStream outBufStream = new BufferedOutputStream(connectionSocket.getOutputStream());
				// Create (US-ASCII charset) buffered reader & writer objects, using the
				// buffered streams^
				BufferedReader inReader = new BufferedReader(new InputStreamReader(inBufStream, "US-ASCII"));
				BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(outBufStream, "US-ASCII"));) {

			// *********************************
			// **** HTTP REQUEST PROCESSING ****
			// *********************************
			// Parse, then Process and respond to the HTTP request
			String[] parsedClientHttpRequest = parseClientHttpRequest(inReader);
			HashMap<String, String> parsedHttpHeaders = parseHttpHeaders(inReader);
			processClientHTTPRequest(outBufStream, outWriter, inReader, rootPath, parsedClientHttpRequest, parsedHttpHeaders);
			connectionSocket.close();

		} catch (IOException e) {
            //e.printStackTrace();
			String exception = e.toString();
			HttpsServer.excLogger.log(Level.WARNING, exception);
		}
	}
}