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
import java.nio.file.ReadOnlyFileSystemException;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class HTTPServer implements Runnable{
	private static final File WEB_ROOT = new File(".");
	private static final String HEAD = "HEAD";
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String DEFAULT_FILE = "index.html";
	private static final String NOT_FOUND = "404.html";
	private static final String NOT_ACCEPTABLE = "406.html";
	private static final String NOT_IMPLEMENTED = "501.html";
	private static final int PORT = 8080;
	private Socket connection;
	private Map<String, String> mimeTypes;

	public HTTPServer(Socket connection)
	{
		this.connection = connection;
		mimeTypes = new HashMap<String, String>();
		this.cargar_mimeTypes();
	}

	public static void Init() {
		try
		{
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

			requestedFile = new File(WEB_ROOT, requestedFileName);
			int fileSize = (int) requestedFile.length();
			String contentMimeType = getContentType(requestedFileName);

			Switch(httpMethod)
			{
				case HEAD:
					
				case GET:
					byte[] dataFile = readDataFile(requestedFile, fileSize);
					output.println();
					output.println("HTTP/1.1 200 OK");
					output.println("Servidor: servidor http");
					output.println("Date: " + new Date());
					output.println("Content-type: " + contentMimeType);
					output.println("Content-length: " + fileSize);
					output.println();
					output.flush();
					outputData.write(dataFile, 0, fileSize);
					outputData.flush();
				case POST:
					
				default:
					System.out.println("501 Not implemented: " + httpMethod + " method");
					requestedFile = new File(WEB_ROOT, NOT_IMPLEMENTED);
					int fileSize = (int)requestedFile.length();
					String contentMimeType = "text/html";
					byte[] fileData = readDataFile(requestedFile, fileSize);
					System.out.println("HTTP/1.1 501 Not Implemented");
					System.out.println("Server: HTTP Server");
					System.out.println("Date: " + new Date());
					System.out.println("Content-type: " + contentMimeType);
					System.out.println("Content-length: " + fileSize);
					System.out.println();
					System.out.flush();
					outputData.write(fileData, 0, fileSize);
					outputData.flush();
			}
		}
		catch(FileNotFoundException fnfe)
		{
			try
			{
				fileNotFound(output, outputData, requestedFileName);
			}
			catch (IOException ioe)
			{
				System.err.println("Error con archivo no encontrado: " + ioe.getMessage());
			}
		}
		catch(IOException ioe)
		{
			System.err.println("Error del servidor: " + ioe);
		}
		finally
		{
			try
			{
				input.close();
				output.close();
				outputData.close();
				connection.close();
			}
			catch(Exception e)
			{
				System.err.println("Error cerrando el archivo: " + e.getMessage());
			}
		}
	}

	private byte[] readDataFile(File archivo, int tamano) throws IOException
	{
		FileInputStream archivo_entrada = null;
		byte[] datos_archivo = new byte[tamano];

		try
		{
			archivo_entrada = new FileInputStream(archivo);
			archivo_entrada.read(datos_archivo);
		}
		finally
		{
			if(archivo_entrada != null)
			{
				archivo_entrada.close();
			}
		}

		return datos_archivo;
	}

	private String getContentType(String archivo_solicitado)
	{
		for (Map.Entry<String, String> pair : mimeTypes.entrySet())//busca en el mapa de tipos el tipo solicitado
		{
			if(archivo_solicitado == pair.getKey())
			{
				return pair.getValue().toString();//si encuentra el tipo, lo devuelve
			}
		}
		return "no";//si no lo encuentra retorna no (406. No aceptable)


	}

	public void cargar_mimeTypes()
	{
		String filename = "./mimetype.txt";
		BufferedReader br = null;
		FileReader fr = null;


		try
		{
			fr = new FileReader(filename);
			br = new BufferedReader(fr);
			String current_line = "";
			while((current_line = br.readLine()) != null)
			{
				StringTokenizer tokenizer = new StringTokenizer(current_line);
				if(tokenizer.hasMoreTokens())
				{
					String key = tokenizer.nextToken();
					if(tokenizer.hasMoreTokens())
					{
						String value = tokenizer.nextToken();
						mimeTypes.put(key, value);
					}

				}

			}

			for (Map.Entry<String, String> pair : mimeTypes.entrySet())
			{
				System.out.print("Llave: " + pair.getKey() + " valor: " + pair.getValue());
				System.out.println();
			}
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
		}
	}


	private void fileNotFound(PrintWriter salida, OutputStream datos_salida, String archivo_solicitado) throws IOException
	{
		File archivo = new File(WEB_ROOT, NOT_FOUND);
		int tamano_archivo = (int)archivo.length();
		String contenido = "text/html";
		byte[] datos_archivo = readDataFile(archivo, tamano_archivo);

		salida.println("HTTP/1.1 404 Not found");
		salida.println("Servidor: servidor http");
		salida.println("Date: " + new Date());
		salida.println("Content-type: " + contenido);
		salida.println("Content-length: " + tamano_archivo);
		salida.println();//linea en blanco entre el header y el documento
		salida.flush();

		datos_salida.write(datos_archivo, 0, tamano_archivo);
		datos_salida.flush();
	}
}