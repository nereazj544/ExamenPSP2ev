package dam.psp.ex20230315;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Peticion implements Runnable {
    private final Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

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
            String p = in.readUTF();
            Respuesta(p);
            switch (p) {
                case "hash":
                    Hash();
                    break;
                case "cert":
                    Cert();
                    break;
                case "cifrar":
                    Cifrar();
                    break;
                default:
                    Respuesta(String.format("ERROR:'%s' no se reconoce como una petición válida", p));

            }

        } catch (SocketTimeoutException e) {
            Respuesta("ERROR: Read timed out");
        } catch (EOFException e) {
            Respuesta("ERROR: Se esperaba una peticion");
        } catch (IOException e) {
            Respuesta("ERROR:" + e.getLocalizedMessage());
        }
    }

    private void Hash() {
        try {
            String al = in.readUTF();
            MessageDigest md = MessageDigest.getInstance(al);
            byte[] barray = in.readAllBytes();

            if (barray.length == 0) {
                Respuesta("Error: Se esperaban datos");
            } else {
                byte[] hash = md.digest(barray);
                String r = Base64.getEncoder().encodeToString(hash);
                Respuesta("OK:" + r);
            }
        } catch (SocketTimeoutException e) {
            Respuesta("ERROR: Read timed out");
        } catch (EOFException | NoSuchAlgorithmException e) {
            Respuesta("ERROR:Se esperaba un algoritmo");
        } catch (IOException e) {
            Respuesta("ERROR:" + e.getLocalizedMessage());
        }
    }

    private void Cert() {
        try {
            String alias = in.readUTF();
            Servidor.ks = KeyStore.getInstance(KeyStore.getDefaultType());
            Servidor.ks.load(null);

            String cB64 = in.readUTF();
            byte [] certEn = Base64.getDecoder().decode(cB64.getBytes());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(new ByteArrayInputStream(certEn));
            Servidor.ks.setCertificateEntry(alias, cert);

            MessageDigest md;
            md = MessageDigest.getInstance("SHA-256");
            md.update(cB64.getBytes());
            String HashB64 = Base64.getEncoder().encodeToString(md.digest());
            Respuesta("OK: " + HashB64);

        } catch (EOFException | UTFDataFormatException | SocketTimeoutException e) {
                Respuesta("ERROR:Se esperaba un certificado");
                Respuesta("ERROR:Se esperaba un alias");
            } 
            catch (IllegalArgumentException e) {
                Respuesta("ERROR:Se esperaba Base64");
            } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
                Respuesta("ERROR:" + e.getLocalizedMessage());
            } catch (IOException e) {
                Respuesta("ERROR:" + e.getLocalizedMessage());
            }
    }

    private void Cifrar()   {
            try {
                String alias = in.readUTF();
                byte[] l = in.readAllBytes();

                Certificate certificate = Servidor.ks.getCertificate(alias);
                if (certificate != null) {
                    PublicKey publicKey = certificate.getPublicKey();
                    if (publicKey.getAlgorithm().equals("RSA")) {
                        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                        cipher.init(Cipher.ENCRYPT_MODE, Servidor.ks.getCertificate(alias).getPublicKey());

                        int n = 0;
                        byte[] b = new byte[256];
                        boolean aux = false;

                        try (ByteArrayInputStream byin = new ByteArrayInputStream(l)) {
                            while ((n = byin.read(b)) != -1) {
                                aux = true;
                                byte[] by = cipher.doFinal(b, 0, n);
                                String hashb64 = Base64.getEncoder().encodeToString(by);

                                Respuesta("Ok: " + hashb64);

                                if (aux == true) {
                                    Respuesta("FIN");
                                }
                            }
                        } catch (IllegalBlockSizeException | BadPaddingException e) {
                            Respuesta("Error: Se esperaban datos");
                        }
                    }
                }

            } catch(SocketTimeoutException e){
                Respuesta("ERROR: tiempo de respues agotado");
            }
            catch (IOException | KeyStoreException | NoSuchAlgorithmException  | InvalidKeyException | NoSuchPaddingException  e) {
                Respuesta("Error: " + e.getLocalizedMessage());
            }
    }

    private void Respuesta(String respuesta) {
        System.out.println(socket.getInetAddress() + " -> " + respuesta);
        try (socket) {
            out.writeUTF(respuesta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}