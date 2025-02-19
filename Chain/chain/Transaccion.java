package chain;

import java.util.Date;

public class Transaccion {
    public int idPersona1;
    public int idPersona2;
    public Date fecha;
    public double valor;
    public String IdentTrans;
    
    @Override
    public String toString() {
        return "Transaccion{" +
                "idPersona1=" + idPersona1 +
                ", idPersona2=" + idPersona2 +
                ", valor=" + valor +
                ", fecha=" + fecha +
                ", IdentTrans='" + IdentTrans + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Transaccion)) {
            return false;
        }
        Transaccion other = (Transaccion) obj;
        return this.IdentTrans.equals(other.IdentTrans);
    }
    
    @Override
    public int hashCode() {
        return IdentTrans.hashCode();
    }
}
