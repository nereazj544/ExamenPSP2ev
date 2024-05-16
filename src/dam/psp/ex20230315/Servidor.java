package dam.psp.ex20230315;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {

	public static void main(String[] args) throws IOException{
		try (ServerSocket serverSocket = new ServerSocket(9000)) {
			ExecutorService executor = Executors.newFixedThreadPool(100);
			System.out.println("SERVIDOR ESCUCHADO EN EL PUERTO: 9000");

			while (true) {
				Socket socket = serverSocket.accept();
				executor.submit(new Peticion(socket));
			}
		}
		
//! END MAIN
}

//! END CLASS SERVER
}