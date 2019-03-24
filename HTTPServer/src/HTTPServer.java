import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.ReadOnlyFileSystemException;
import java.util.StringTokenizer;
import java.util.Date;

public class HTTPServer implements Runnable{
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String NOT_IMPLEMENTED = "not_supported.html";
	
	//puerto de conexión
	static final int PUERTO = 8080;
	
	//verbose mode
	static final boolean verbose = true;
	
	//conexión del cliente a través de socket
	private Socket connect;
	
	public HTTPServer(Socket s)
	{
		connect = s;
	}
	
	public static void main(String[] args)
	{
		try
		{
			ServerSocket serverConnect = new ServerSocket(PUERTO);
			System.out.println("Servidor arrancado. \nEscuchando conexiones en el puerto " + PUERTO + "...\n");
			
			//escucha hasta que el usuario detenga la ejecución
			while(true)
			{
				HTTPServer servidor = new HTTPServer(serverConnect.accept());
				
				//crea un hilo para cada conexión entrante
				Thread hilo = new Thread(servidor);
				hilo.start();
			}
		}
		catch(IOException e)
		{
			System.err.println("Error de conexión en el servidor" + e.getMessage());
		}
	}

	@Override
	public void run() 
	{
		//maneja la conexión de un cliente particular
		BufferedReader entrada = null;
		PrintWriter salida = null;
		BufferedOutputStream datos_salida = null;
		String archivo_solicitado = null;
		
		try
		{
			//lee caracteres del cliente
			entrada = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			//salida de caracteres para el cliente (headers)
			salida = new PrintWriter(connect.getOutputStream());
			//información binaria para los datos solicitados
			datos_salida = new BufferedOutputStream(connect.getOutputStream());
			
			//obtiene la primera linea de la solicitud http
			String entr = entrada.readLine();
			System.out.println("entr = " + entr);
			//parsea el request con un tokenizador
			StringTokenizer parser = new StringTokenizer(entr);
			String metodo = parser.nextToken().toUpperCase();//saca el metodo http de la solicitud
			
			//jala el archivo solicitado
			archivo_solicitado = parser.nextToken().toLowerCase();
			
			if(!metodo.equals("HEAD") && !metodo.equals("GET") && !metodo.equals("POST"))
			{
				System.out.println("501 Not implemented: " + metodo + " method");
				
				//prepara el archivo solicitado para devolverlo al cliente
				File archivo = new File(WEB_ROOT, NOT_IMPLEMENTED);
				int tamano_archivo = (int)archivo.length();
				String contentMimeType = "text/html";
				//lee contenido para retornar al cliente
				byte[] datos_archivo = leer_datos_archivo(archivo, tamano_archivo);
				
				//se envian los encabezados http al cliente con los datos
				System.out.println("HTTP/1.1 501 Not Implemented");
				System.out.println("Servidor: servidor http");
				System.out.println("Date: " + new Date());
				System.out.println("Content-type: " + contentMimeType);
				System.out.println("Content-length: " + tamano_archivo);
				System.out.println();//linea en blanco entre el header y el documento
				System.out.flush();
				
				//archivo
				datos_salida.write(datos_archivo, 0, tamano_archivo);
				datos_salida.flush();
			}
			else
			{
				//GET, HEAD o POST
				if(archivo_solicitado.endsWith("/"))
				{
					archivo_solicitado += DEFAULT_FILE;
				}
				
				File archivo = new File(WEB_ROOT, archivo_solicitado);
				int tamano_archivo = (int) archivo.length();
				String contenido = getContentType(archivo_solicitado);
				
				if(metodo.equals("GET"))
				{
					byte[] datos_archivo = leer_datos_archivo(archivo, tamano_archivo);
					
					
					salida.println();
					salida.println("HTTP/1.1 200 OK");
					salida.println("Servidor: servidor http");
					salida.println("Date: " + new Date());
					salida.println("Content-type: " + contenido);
					salida.println("Content-length: " + tamano_archivo);
					//salida.println("GET");
					salida.println();//linea en blanco entre el header y el documento
					salida.flush();
					
					datos_salida.write(datos_archivo, 0, tamano_archivo);
					
					datos_salida.flush();
					
				}
			}
		
		}
		catch(FileNotFoundException fnfe)
		{
			try
			{
				archivo_no_encontrado(salida, datos_salida, archivo_solicitado);
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
				entrada.close();
				salida.close();
				datos_salida.close();
				connect.close();
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
		File archivo = new File(WEB_ROOT, FILE_NOT_FOUND);
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
