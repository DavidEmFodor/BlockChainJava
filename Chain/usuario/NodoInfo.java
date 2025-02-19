package usuario;

public class NodoInfo {
    public String ip;
    public int port;
    
    public NodoInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
    
    @Override
    public String toString() {
        return ip + ":" + port;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NodoInfo))
            return false;
        NodoInfo other = (NodoInfo) obj;
        return this.ip.equals(other.ip) && this.port == other.port;
    }
    
    @Override
    public int hashCode() {
        return (ip + port).hashCode();
    }
}
