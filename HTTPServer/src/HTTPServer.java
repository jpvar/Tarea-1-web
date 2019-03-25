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
			System.err.println("Error de conexion en el servidor" + e.getMessage());
		}
	}

	@Override
	public void run() 
	{
		BufferedReader input = null;
		PrintWriter output = null;
		BufferedOutputStream outputData = null;
		String requestedFile = null;
		
		try
		{
			input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			output = new PrintWriter(connection.getOutputStream());
			outputData = new BufferedOutputStream(connection.getOutputStream());
			
			String inputData = input.readLine();
			StringTokenizer parser = new StringTokenizer(inputData);
			String metodo = parser.nextToken().toUpperCase();
			
			requestedFile = parser.nextToken().toLowerCase();
			
			if(!metodo.equals("HEAD") && !metodo.equals("GET") && !metodo.equals("POST"))
			{
				System.out.println("501 Not implemented: " + metodo + " method");
				
				File archivo = new File(WEB_ROOT, NOT_IMPLEMENTED);
				int tamano_archivo = (int)archivo.length();
				String contentMimeType = "text/html";
				byte[] datos_archivo = leer_datos_archivo(archivo, tamano_archivo);
				
				System.out.println("HTTP/1.1 501 Not Implemented");
				System.out.println("Servidor: servidor http");
				System.out.println("Date: " + new Date());
				System.out.println("Content-type: " + contentMimeType);
				System.out.println("Content-length: " + tamano_archivo);
				System.out.println();//linea en blanco entre el header y el documento
				System.out.flush();
				
				//archivo
				outputData.write(datos_archivo, 0, tamano_archivo);
				outputData.flush();
			}
			else
			{
				//GET, HEAD o POST
				if(requestedFile.endsWith("/"))
				{
					requestedFile += DEFAULT_FILE;
				}
				
				File archivo = new File(WEB_ROOT, requestedFile);
				int tamano_archivo = (int) archivo.length();
				String contenido = getContentType(requestedFile);
				
				if(metodo.equals("GET"))
				{
					byte[] datos_archivo = leer_datos_archivo(archivo, tamano_archivo);
					
					
					output.println();
					output.println("HTTP/1.1 200 OK");
					output.println("Servidor: servidor http");
					output.println("Date: " + new Date());
					output.println("Content-type: " + contenido);
					output.println("Content-length: " + tamano_archivo);
					//salida.println("GET");
					output.println();//linea en blanco entre el header y el documento
					output.flush();
					
					outputData.write(datos_archivo, 0, tamano_archivo);
					
					outputData.flush();
					
				}
			}
		
		}
		catch(FileNotFoundException fnfe)
		{
			try
			{
				archivo_no_encontrado(output, outputData, requestedFile);
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
	
	private byte[] leer_datos_archivo(File archivo, int tamano) throws IOException
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
		if(archivo_solicitado.endsWith(".htm") || archivo_solicitado.endsWith(".html"))
		{
			return "html";
		}
		else
		{
			return "text/plain";
		}
	}
	
	private void archivo_no_encontrado(PrintWriter salida, OutputStream datos_salida, String archivo_solicitado) throws IOException
	{
		File archivo = new File(WEB_ROOT, NOT_FOUND);
		int tamano_archivo = (int)archivo.length();
		String contenido = "text/html";
		byte[] datos_archivo = leer_datos_archivo(archivo, tamano_archivo);
		
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
