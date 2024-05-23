package dam.psp.ex20230315;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;

public class Peticion implements Runnable{
    private Socket socket;
	private DataOutputStream out;
    private DataInputStream in;
    // private KeyStore ks;
	
	public Peticion(Socket socket) throws IOException {
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
                    peticionHash(socket, in, out);
                    break;
            
                case "cert":
                    peticionCert(socket, in, out);
                    break;
            
                case "cifrar":
                    
                    break;
            
                default:
                enviarRespuesta(String.format("Error: '%s' no se reconcoe como una peticion valida", peticion));
                    break;
            }
		} catch (SocketTimeoutException e) {
			enviarRespuesta("ERROR:Read timed out");
		}catch(EOFException e){
            enviarRespuesta("ERROR: Se esperaba una peticion");
        }
        catch (IOException e) {
			e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
		}
	}
	
	

    
    private void peticionHash(Socket socket, DataInputStream in, DataOutputStream out) {
        // String algorimo = null;
        //! CLASE
        // try {
        //     String a = in.readUTF();
        //     MessageDigest md = MessageDigest.getInstance(a);
        //     byte [] by = in.readAllBytes();
        //     if (by.length == 0) {
        //         enviarRespuesta("ERROR: Se esperaban datos");
        //     }else{
        //         byte [] hash = md.digest(by);
        //         enviarRespuesta("OK" + Base64.getEncoder().encodeToString(by));
        //     }

            
        // }catch(SocketTimeoutException s){
        //     enviarRespuesta("ERROR: Read timed out");
        // } catch(EOFException | NoSuchAlgorithmException x){
        //     enviarRespuesta("ERROR: Se esperaba un algoritmo.");
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
            //! VERSION JULIO
            try {
                String a = in.readUTF();
                MessageDigest md = MessageDigest.getInstance(a);
                int n;
                int contador = 0;
                byte [] by = new byte[1024];
                while ((n = in.read(by)) != -1) {
                    contador+= n;
                    md.update(by, 0, n);
                    //inice inical del array es cero
                }
                if (contador == 0) {
                    enviarRespuesta("ERROR: Se esperaban datos");
                }else{
                    byte [] hash = md.digest();
                    enviarRespuesta("OK" + Base64.getEncoder().encodeToString(by));
                }
    
                
            }catch(SocketTimeoutException s){
                enviarRespuesta("ERROR: Read timed out");
            } catch(EOFException | NoSuchAlgorithmException x){
                enviarRespuesta("ERROR: Se esperaba un algoritmo.");
            } catch (IOException e) {
                e.printStackTrace();
            }
                
    


        // try {
        //     algorimo = in.readUTF();
        //     byte [] m = in.readAllBytes();
        //     if (m.length > 0) {
        //         MessageDigest md = MessageDigest.getInstance(algorimo);
        //         String r = Base64.getEncoder().encodeToString(md.digest(m));
        //         out.writeUTF("OK:" + r);
        //         out.flush();
        //     }else{
        //         System.out.println("ERROR:Se esperaba un algoritmo");
        //         out.flush();
        //     }
        // }catch(NoSuchAlgorithmException | EOFException e){
        //     try {
        //         System.out.println("ERROR: Se espera un algoritmo");
        //     } catch (Exception x) {
                
        //     }
        // }
    }
    
    private void peticionCert(Socket socket, DataInputStream in, DataOutputStream out) {
        try {
            String alias = in.readUTF();
            try {
                String certB64 = in.readUTF();

                //! Decificar
                byte [] b = Base64.getDecoder().decode(certB64.getBytes());

                //! Conversion
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                //! Generar el certificado
                Certificate c = cf.generateCertificate(new ByteArrayInputStream(b));
                Servidor.ks.setCertificateEntry(alias, c);
            } catch ( EOFException | UTFDataFormatException | SocketTimeoutException e) {
                enviarRespuesta("ERROR: Se esperaba un certificado.");
            }catch(IllegalArgumentException e){
                enviarRespuesta("Error: Se esperaba base64");
            }catch(KeyStoreException | CertificateException e){
                enviarRespuesta("ERROR: " + e.getLocalizedMessage());
            }
        } catch ( EOFException | UTFDataFormatException | SocketTimeoutException e){
            enviarRespuesta("ERROR: Se esperaba un alias.");
        }catch(IOException e){
            enviarRespuesta("ERROR: " + e.getLocalizedMessage());
        }
        
    }

    private void enviarRespuesta(String respuesta) {
		System.out.println(socket.getInetAddress() + " -> " + respuesta);
		try { 
			out.writeUTF(respuesta);
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}
    
}
