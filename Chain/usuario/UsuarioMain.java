package usuario;

public class UsuarioMain {
    public static void main(String[] args) {
        // Configuración del nodo maestro.
        // Se asume que el nodo maestro está en la IP "10.10.14.153" y puerto 5000.
        String masterIp = "10.10.14.153";
        int masterPort = 5000;
        
        Usuario masterNodo = new Usuario(masterIp, masterPort, true, masterIp);
        masterNodo.iniciarEscucha();
        System.out.println("Nodo maestro iniciado en " + masterIp + ":" + masterPort);
        
        // Espera para asegurarse de que el maestro esté activo
        try { 
            Thread.sleep(1000); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Configuración de otros nodos.
        // Se crean 4 nodos en la misma IP pero con puertos distintos.
        String[] otherNodesIps = {"10.10.14.153", "10.10.14.153", "10.10.14.153", "10.10.14.153"};
        int[] otherNodesPorts = {5001, 5002, 5003, 5004};
        
        for (int i = 0; i < otherNodesIps.length; i++) {
            String nodoIp = otherNodesIps[i];
            int nodoPort = otherNodesPorts[i];
            
            Usuario nodo = new Usuario(nodoIp, nodoPort, false, masterIp);
            nodo.iniciarEscucha();
            System.out.println("Nodo iniciado en " + nodoIp + ":" + nodoPort);
            
            try { 
                Thread.sleep(500); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            nodo.conectarAlMaestro();
            nodo.enviarListaActualizada();
            nodo.conectarAConocidos();
        }
    }
}
