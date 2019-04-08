import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;


public class HTTPServer implements Runnable{
	private static final File WEB_ROOT = new File(".");
	private static final String HEAD = "HEAD";
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String DEFAULT_FILE = "index.html";
	private static final String NOT_FOUND = "404.html";
	private static final String NOT_ACCEPTABLE = "406.html";
	private static final String NOT_IMPLEMENTED = "501.html";
	private static final int PORT = 8082;
	private Socket connection;
	private static Map<String, String> mimeTypes;
	private static ServerLog serverLog = new ServerLog();

	public HTTPServer(Socket connection)
	{
		this.connection = connection;
	}

	public static void Init() {
		try
		{
			mimeTypes = new HashMap<>();
			loadMimeTypes();
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started. \nListen for connections on port: " + PORT + "...\n");

			//Server listen until user ends connection.
			while(true)
			{
				HTTPServer server = new HTTPServer(serverConnect.accept());
				//Creates one thread per connection.
				Thread connection = new Thread(server);
				connection.start();
			}
		}
		catch(IOException e)
		{
			System.err.println("Connection error" + e.getMessage());
		}
	}

	@Override
	public void run()
	{
		BufferedReader input = null;
		PrintWriter output = null;
		BufferedOutputStream outputData = null;
		String requestedFileName = null;
		File requestedFile = null;
		try
		{
			input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			output = new PrintWriter(connection.getOutputStream());
			outputData = new BufferedOutputStream(connection.getOutputStream());
			String inputData = input.readLine();
			StringTokenizer parser = new StringTokenizer(inputData);
			String httpMethod = parser.nextToken().toUpperCase();
			requestedFileName = parser.nextToken().toLowerCase();

			if(requestedFileName.endsWith("/"))
			{
				requestedFileName += DEFAULT_FILE;
			}
			String contentMimeType = getContentType(requestedFileName.substring(requestedFileName.indexOf('.')).substring(1));

			System.out.println("mime type: " + contentMimeType);
			System.out.printf("requested file [%s]\n", requestedFileName);
			int fileSize = 0;
			switch(httpMethod)
			{
				case HEAD:
					requestedFile = new File(WEB_ROOT, requestedFileName);
					fileSize = (int) requestedFile.length();
					String accept = "";//accept field in header
					boolean acceptType = false;
					acceptType = isAcceptType(input, output, outputData, requestedFileName, contentMimeType, acceptType);
					if(acceptType) {
						returnResponse(output, "HTTP/1.1 200 OK", fileSize, contentMimeType);
						serverLog.log(HEAD, "localhost", "localhost", WEB_ROOT + requestedFileName, "");
					}
					else {
						notAcceptable(output, outputData);
					}
					break;
					
				case GET:
					requestedFile = new File(WEB_ROOT, requestedFileName);
					fileSize = (int) requestedFile.length();
					byte[] dataFile = readDataFile(requestedFile, fileSize);
					accept = "";//accept field in header
					acceptType = false;
					acceptType = isAcceptType(input, output, outputData, requestedFileName, contentMimeType, acceptType);

					if(acceptType)
					{
						returnResponse(output, "HTTP/1.1 200 OK", fileSize, contentMimeType);
						outputData.write(dataFile, 0, fileSize);
						outputData.flush();
						serverLog.log(GET, "localhost", "localhost", requestedFile.getPath(), "");
					} else {
						notAcceptable(output, outputData);
					}
					break;
				case POST:
					requestedFile = new File(WEB_ROOT, requestedFileName);
					fileSize = (int) requestedFile.length();
					dataFile = readDataFile(requestedFile, fileSize);

                    StringBuffer queryString = new StringBuffer();
                    int contentLength = 0;
                    accept = "";//accept field in header
					acceptType = false;
                    while((inputData = input.readLine()) != null)
                    {
                        if(inputData.contains("Accept: "))//
						{
							String mimeTypes = inputData.substring(8);
							accept = mimeTypes;
							String[] types = mimeTypes.split(",");

							for(String t : types) {
								if (t.contains(";")) {
									if (contentMimeType.equals(t.substring(0, t.indexOf(";")))) {
										acceptType = true;
									}
								} else if (contentMimeType.equals(t)) {
									acceptType = true;
								} else if (t.equals("*/*")) {
									acceptType = true;
								}
							}

							if(acceptType == false)//si el tipo no es soportado retorna not acceptable
							{
								notAcceptable(output, outputData);
								break;
							}

						}
                    	if(inputData.contains("Content-Length"))
                        {
                            contentLength = Integer.parseInt(inputData.substring(16)) ;
                        }
                        if(inputData.equals(""))
                        {
                            for(int i = 0; i < contentLength; ++i)
                            {
                                queryString.append(((char) input.read()));
                            }
                            System.out.println(queryString.toString());
                            break;
                        }
                            System.out.println("queryString: " + queryString.toString());
                    }
                    System.out.println("queryString: " + queryString.toString());
					if(acceptType) {
						returnResponse(output, "HTTP/1.1 200 OK", fileSize, contentMimeType);
						outputData.write(dataFile, 0, fileSize);
						outputData.flush();
						serverLog.log(POST, "localhost", "localhost", requestedFile.getPath(), queryString.toString());
					} else {
						notAcceptable(output, outputData);
					}

					break;
					
				default:
					System.out.println("501 Not implemented: " + httpMethod + " method");
					requestedFile = new File(WEB_ROOT, NOT_IMPLEMENTED);
					fileSize = (int)requestedFile.length();
					contentMimeType = "text/html";
					byte[] fileData = readDataFile(requestedFile, fileSize);
					returnResponse(output, "HTTP/1.1 501 Not Implemented", fileSize, contentMimeType);
					outputData.write(fileData, 0, fileSize);
					outputData.flush();

					break;
			}
		}
		catch(FileNotFoundException fnfe)
		{
			try
			{
				fileNotFound(output, outputData);
			}
			catch (IOException ioe)
			{
				System.err.println("File not found: " + ioe.getMessage());
			}
		}
		catch(IOException ioe)
		{
			System.err.println("Server error: " + ioe);
		}
		finally
		{
			try
			{
				System.out.println("Server:end");
				input.close();
				output.close();
				outputData.close();
				connection.close();
			}
			catch(Exception e)
			{
				System.err.println("Error closing file: " + e.getMessage());
			}
		}
	}

	private  void returnResponse(PrintWriter output, String response, int fileSize, String contentMimeType){
		output.println();
		output.println(response);
		output.println("Date: " + new Date());
		output.println("Server: localhost");
		output.println("Content-length: " + fileSize);
		output.println("Content-type: " + contentMimeType);
		output.println("Referer: localhost:" + PORT);
		output.println();
		output.flush();
	}
	private boolean isAcceptType(BufferedReader input, PrintWriter output, BufferedOutputStream outputData, String requestedFileName, String contentMimeType, boolean acceptType) throws IOException {
		String inputData;
		String accept;
		while((inputData = input.readLine()) != null)
		{
			if(inputData.contains("Accept: "))//
			{
				String mimeTypes = inputData.substring(8);
				accept = mimeTypes;
				String[] types = mimeTypes.split(",");

				for(String t : types) {
					if (t.contains(";")) {
						if (contentMimeType.equals(t.substring(0, t.indexOf(";"))) || t.substring(0, t.indexOf(";")).equals("*/*")) {
							acceptType = true;
							break;
						}
					} else if (contentMimeType.equals(t)) {
						acceptType = true;
						break;
					} else if (t.equals("*/*")) {
						acceptType = true;
						break;
					}
				}
				break;
			}
		}
		return acceptType;
	}

	private byte[] readDataFile(File file, int fileSize) throws IOException
	{
		FileInputStream inputFile = null;
		byte[] dataFile = new byte[fileSize];

		try
		{
			inputFile = new FileInputStream(file);
			inputFile.read(dataFile);
		}
		finally
		{
			if(inputFile != null)
			{
				inputFile.close();
			}
		}

		return dataFile;
	}

	private String getContentType(String requestedFileType)
	{
		String value = mimeTypes.get(requestedFileType);
		return value.isEmpty() ? "no": value;
	}

	public static void loadMimeTypes()
	{
		try {

			File inputFile = new File("./web.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("mime-mapping");


			for (int temp = 0; temp < nList.getLength(); temp++)
			{
				Node nNode = nList.item(temp);
				//System.out.println("\nCurrent Element :" + nNode.getNodeName());

				if (nNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element eElement = (Element) nNode;
					mimeTypes.put(eElement.getElementsByTagName("extension").item(0).getTextContent(), eElement.getElementsByTagName("mime-type").item(0).getTextContent());
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error parsing xml" + e.getMessage());
		}
	}


	private void fileNotFound(PrintWriter output, OutputStream outputData) throws IOException
	{
		File file = new File(WEB_ROOT, NOT_FOUND);
		int fileSize = (int)file.length();
		String content = "text/html";
		byte[] dataFile = readDataFile(file, fileSize);
		returnResponse(output, "HTTP/1.1 404 Not found", fileSize, content);
		outputData.write(dataFile, 0, fileSize);
		outputData.flush();
	}

	private void notAcceptable(PrintWriter output, OutputStream outputData) throws IOException
	{
		File file = new File(WEB_ROOT, NOT_ACCEPTABLE);
		int fileSize = (int)file.length();
		String content = "text/html";
		byte[] dataFile = readDataFile(file, fileSize);
		returnResponse(output, "HTTP/1.1 406 Not acceptable", fileSize, content);
		outputData.write(dataFile, 0, fileSize);
		outputData.flush();
	}
}