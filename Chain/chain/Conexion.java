package chain;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Conexion {
    public ServerSocket ss;
    protected Socket cs;
    
    public Conexion(String tipo, int puerto, String direccionIP) throws IOException {
        if (tipo.equalsIgnoreCase("servidor")) {
            ss = new ServerSocket(puerto, 50, InetAddress.getByName(direccionIP));
        } else {
            cs = new Socket(direccionIP, puerto);
        }
    }
}
