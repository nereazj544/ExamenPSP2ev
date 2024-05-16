package dam.psp.ex20230315;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Peticion implements Runnable{
    private Socket socket;
    private DataOutputStream out;

    public Peticion(Socket socket) throws IOException {
        this.socket = socket;
        socket.setSoTimeout(3000);
        out = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        System.out.println("Conectado con " + socket.getInetAddress());
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            String peticion = in.readUTF();
        }catch(SocketTimeoutException e){
            //! Al ser una subclase tiene que ir primero
            enRe("ERROR:Read timed out");
            // System.out.println("====================\n");
            // System.out.println("EXPECIFICACION DEL ERROR: ");
            // e.printStackTrace();
        }catch(IOException e){
        //     System.out.println("ERROR EN LA CONEXION O EN LA LECTURA.");
        //     System.out.println("======================================\n");
        //     System.out.println("EXPECIFICACION DEL ERROR: ");
        //     e.printStackTrace();
        }
        
    }

    private void enRe(String respuesta){
        System.out.println(socket.getInetAddress() + "-> " + respuesta);
        try {
            out.writeUTF(respuesta);
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
    
}
