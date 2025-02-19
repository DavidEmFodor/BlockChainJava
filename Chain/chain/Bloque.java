package chain;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;

public class Bloque {
    public String hashActual;
    public ArrayList<Transaccion> listaTransacciones = new ArrayList<>();
    public String hashAnterior;
    public Date fechaCreaccion;
    public int nonce;
    
    public static String computeHash(Bloque bloque) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(bloque.hashAnterior == null ? "null" : bloque.hashAnterior);
            sb.append(bloque.fechaCreaccion.getTime());
            sb.append(bloque.nonce);
            for (Transaccion tx : bloque.listaTransacciones) {
                sb.append(tx.idPersona1)
                  .append(tx.idPersona2)
                  .append(tx.valor)
                  .append(tx.fecha.getTime())
                  .append(tx.IdentTrans);
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(sb.toString().getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hashBytes.length; i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Bloque)) {
            return false;
        }
        Bloque other = (Bloque) obj;
        return computeHash(this).equals(computeHash(other));
    }
    
    @Override
    public int hashCode() {
        return computeHash(this).hashCode();
    }
}
