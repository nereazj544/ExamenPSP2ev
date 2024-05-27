package dam.psp.ex20230315;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;

public class PeticionVieja implements Runnable {
    private final Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public PeticionVieja(Socket socket) throws IOException {
        this.socket = socket;
        socket.setSoTimeout(5000);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        System.out.println("Conectado con " + socket.getInetAddress());
        try {
            String peticion = in.readUTF();
            switch (peticion) {
                case "hash":
                    peticionHash();
                    break;

                case "cert":
                    peticionCert();
                    break;

                case "cifrar":
                    peticionCifrar();
                    break;

                default:
                    enviarRespuesta(String.format("Error: '%s' no se reconcoe como una peticion valida", peticion));
                    break;
            }
        } catch (SocketTimeoutException e) {
            enviarRespuesta("ERROR:Read timed out");
        } catch (EOFException e) {
            enviarRespuesta("ERROR: Se esperaba una peticion");
        } catch (IOException e) {
            enviarRespuesta("ERROR: " + e.getLocalizedMessage());
        }
    }

    // !PETICION HASH
    private void peticionHash() {
        try {
            String a = in.readUTF();
            byte [] by = in.readAllBytes();

            if (by.length > 0) {
                MessageDigest md = MessageDigest.getInstance(a);
                String r = Base64.getEncoder().encodeToString(md.digest(by));
                enviarRespuesta("OK: " + r);
            }else{
                enviarRespuesta("ERROR: Se esperaban datos.");
            }
        } catch (NoSuchAlgorithmException | EOFException e) {
            enviarRespuesta("ERROR: Se esperaba un algoritmo");
        }catch(SocketTimeoutException e){
            enviarRespuesta("EROR: Read time out");
        }catch(IOException e){
            enviarRespuesta("ERROR: " + e.getLocalizedMessage());
        }


        // TODO: CLASE
        /*
         * 
         * String algorimo = null;
         * try {
         * String a = in.readUTF();
         * MessageDigest md = MessageDigest.getInstance(a);
         * byte [] by = in.readAllBytes();
         * if (by.length == 0) {
         * enviarRespuesta("ERROR: Se esperaban datos");
         * }else{
         * byte [] hash = md.digest(by);
         * enviarRespuesta("OK" + Base64.getEncoder().encodeToString(by));
         * }
         * 
         * 
         * }catch(SocketTimeoutException s){
         * enviarRespuesta("ERROR: Read timed out");
         * } catch(EOFException | NoSuchAlgorithmException x){
         * enviarRespuesta("ERROR: Se esperaba un algoritmo.");
         * } catch (IOException e) {
         * e.printStackTrace();
         * }
         */
        // ! VERSION JULIO
        /*
         * 
         * try {
         * String a = in.readUTF();
         * MessageDigest md = MessageDigest.getInstance(a);
         * int n;
         * int contador = 0;
         * byte [] by = new byte[1024];
         * while ((n = in.read(by)) != -1) {
         * contador+= n;
         * md.update(by, 0, n);
         * //inice inical del array es cero
         * }
         * if (contador == 0) {
         * enviarRespuesta("ERROR: Se esperaban datos");
         * }else{
         * byte [] hash = md.digest();
         * enviarRespuesta("OK" + Base64.getEncoder().encodeToString(by));
         * }
         * 
         * 
         * }catch(SocketTimeoutException s){
         * enviarRespuesta("ERROR: Read timed out");
         * } catch(EOFException | NoSuchAlgorithmException x){
         * enviarRespuesta("ERROR: Se esperaba un algoritmo.");
         * } catch (IOException e) {
         * e.printStackTrace();
         * }
         */

    }

    private void peticionCert() {
        try {
            String alias = in.readUTF();
            try {
                String certB64 = in.readUTF();
                // ! Decificar
                byte[] b = Base64.getDecoder().decode(certB64.getBytes());
                if (b.length > 0) {

                    // ! Conversion
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    // ! Generar el certificado
                    Certificate c = cf.generateCertificate(new ByteArrayInputStream(b));
                    Servidor.ks.setCertificateEntry(alias, c);
                } else {
                    MessageDigest md;
                    String hash = Base64.getEncoder().encodeToString(b);
                    md = MessageDigest.getInstance("SHA-256");
                    md.update(certB64.getBytes());

                    enviarRespuesta("OK" + hash);
                    // Con una sola sentencia:
                    // enviarRespuesta(Base64.getEncoder().encodeToString(md.getInstance("SHA-256").digest(certB64.getBytes())));
                }

            } catch (EOFException | UTFDataFormatException | SocketTimeoutException e) {
                enviarRespuesta("ERROR: Se esperaba un certificado.");
            } catch (IllegalArgumentException e) {
                enviarRespuesta("Error: Se esperaba Base64");
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
                enviarRespuesta("ERROR: " + e.getLocalizedMessage());
            }
        } catch (EOFException | UTFDataFormatException | SocketTimeoutException e) {
            enviarRespuesta("ERROR: Se esperaba un alias.");
        } catch (IOException e) {
            enviarRespuesta("ERROR: " + e.getLocalizedMessage());
        }

    }

    // ! CIFRAR
    private void peticionCifrar() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'peticionCifrar'");
    }

    // ! RESPUESTA
    private void enviarRespuesta(String respuesta) {
        System.out.println(socket.getInetAddress() + " -> " + respuesta);
        try (socket) {
            out.writeUTF(respuesta);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // System.out.println(socket.getInetAddress() + " -> " + respuesta);
        // try {
        // out.writeUTF(respuesta);
        // } catch (IOException e) {
        // e.printStackTrace();
        // }finally{
        // try {
        // if (socket != null) {
        // socket.close();
        // }
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }
    }

}
