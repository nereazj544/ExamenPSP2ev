package dam.psp.ex20230315;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Peticion implements Runnable{
    private final Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public Peticion(Socket socket) throws SocketException, IOException {
        this.socket = socket;
        socket.setSoTimeout(50000);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }



    @Override
    public void run() {
        System.out.println("Conectado con "+ socket.getInetAddress());
        try {
            String p = in.readUTF();

            switch (p) {
                case "hash":
                    Hash();
                    break;
                case "Cert":
                    Cert();
                    break;
                case "cifrar":
                    Cifrar();
                    break;
            
                default:
                    break;
            }
            
        } catch (SocketTimeoutException e) {
            Respuesta("ERROR: Read timed out");
        }catch(EOFException e){
            Respuesta("ERROR: Se esperaba una peticion");
        }catch(IOException e){
            Respuesta("ERROR:" + e.getLocalizedMessage());
        }
    }
    



    private void Hash() {
        try {
            String al = in.readUTF();
            MessageDigest md = MessageDigest.getInstance(al);
            byte [] barray = in.readAllBytes();

            if (barray.length == 0 ) {
                Respuesta("Error: Se esperaban datos");
            } else {
                byte [] hash = md.digest(barray);
                String r = Base64.getEncoder().encodeToString(hash);
                Respuesta("OK:" + r);
            }
        } catch (SocketTimeoutException e) {
            Respuesta("ERROR: Read timed out");
        }catch (EOFException | NoSuchAlgorithmException e) {
			Respuesta("ERROR:Se esperaba un algoritmo");
		} catch (IOException e) {
			Respuesta("ERROR:" + e.getLocalizedMessage());
		} 
    }



    private void Cert() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Cert'");
    }



    private void Cifrar() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'Cifrar'");
    }



    private void Respuesta(String respuesta){
        System.out.println(socket.getInetAddress() + " -> " + respuesta);
        try (socket) {
            out.writeUTF(respuesta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
