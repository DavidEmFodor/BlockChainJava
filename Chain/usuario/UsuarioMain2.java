package usuario;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class UsuarioMain2 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Obtener automáticamente la IP local (IPv4)
        String localIp;
        try {
            localIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            localIp = "127.0.0.1";
        }
        System.out.println("Tu IP es: " + localIp);

        // Solicitar solo el puerto para el nuevo nodo
        System.out.print("Ingrese el puerto para el nuevo nodo: ");
        int puerto = scanner.nextInt();
        scanner.nextLine(); // Consumir salto de línea

        // Si el puerto es 5000, se asume que es el nodo maestro;
        // de lo contrario, se conecta al nodo maestro con IP fija "10.10.14.153"
        String masterIp = (puerto == 5000) ? localIp : "10.10.14.153";

        // Crear e iniciar el nodo
        Usuario nodo = new Usuario(localIp, puerto, puerto == 5000, masterIp);
        nodo.iniciarEscucha();

        if (puerto != 5000) {
            try { 
                Thread.sleep(1000); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nodo.conectarAlMaestro();
            nodo.enviarListaActualizada();
            nodo.conectarAConocidos();
        }

        System.out.println("Nodo iniciado en " + localIp + ":" + puerto);

        // Menú interactivo
        while (true) {
            System.out.println("\nSeleccione una opción:");
            System.out.println("1. Crear transacción");
            System.out.println("2. Minar bloque");
            System.out.println("3. Mostrar blockchain");
            System.out.println("4. Salir");
            int opcion = scanner.nextInt();
            scanner.nextLine();
            switch (opcion) {
                case 1:
                    System.out.print("Ingrese idPersona1: ");
                    int id1 = scanner.nextInt();
                    System.out.print("Ingrese idPersona2: ");
                    int id2 = scanner.nextInt();
                    System.out.print("Ingrese valor: ");
                    double valor = scanner.nextDouble();
                    scanner.nextLine();
                    nodo.crearTransaccion(id1, id2, valor);
                    break;
                case 2:
                    nodo.minarBloque();
                    break;
                case 3:
                    System.out.println("Blockchain:");
                    for (chain.Bloque b : nodo.listaTBloques) {
                        String hash = chain.Bloque.computeHash(b);
                        System.out.println("Bloque: hashAnterior=" + b.hashAnterior + ", nonce=" + b.nonce + ", hash=" + hash);
                        System.out.println("Transacciones:");
                        for (chain.Transaccion t : b.listaTransacciones) {
                            System.out.println("  " + t.toString());
                        }
                    }
                    break;
                case 4:
                    System.out.println("Saliendo...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Opción inválida.");
            }
        }
    }
}
