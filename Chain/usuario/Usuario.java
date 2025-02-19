package usuario;

import chain.Bloque;
import chain.Conexion;
import chain.Transaccion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Clase que representa un nodo en la red P2P.
 * Cada nodo actúa tanto como cliente como servidor para mantener la red.
 */
public class Usuario {
    public int puerto;
    public final int nodoMaestro = 5000;
    public String ip;         
    public String ipMaestro;
    public boolean master = false;
    
    /**
     * Listas que contienen los nodos conocidos, la blockchain y las transacciones pendientes.
     */
    public ArrayList<NodoInfo> nodos = new ArrayList<>();
    public ArrayList<Bloque> listaTBloques = new ArrayList<>();
    public ArrayList<Transaccion> pendingTransactions = new ArrayList<>();

    /**
     * Crea un nodo, pasándole la ip, el puerto, si es o no maestro y la ip del maestro.
     * @param ip dirección IP del nodo.
     * @param puerto puerto en el que se establecerá la comunicación.
     * @param isMaster indica si el nodo es maestro.
     * @param ipMaestro dirección IP del nodo maestro.
     */
    public Usuario(String ip, int puerto, boolean isMaster, String ipMaestro) {
        this.ip = ip;
        this.puerto = puerto;
        this.master = isMaster;
        this.ipMaestro = ipMaestro;
        nodos.add(new NodoInfo(ip, puerto));
    }

    /**
     * Inicia la escucha de conexiones entrantes en el nodo.
     * Este método actúa como un "servidor" interno para recibir mensajes de otros nodos.
     */
    public void iniciarEscucha() {
        Thread escuchaThread = new Thread(() -> {
            try {
                Conexion conexion = new Conexion("servidor", puerto, ip);
                System.out.println("Nodo en " + ip + ":" + puerto + " iniciado, esperando conexiones...");
                while (true) {
                    Socket socket = conexion.ss.accept();
                    new Thread(() -> {
                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            String mensaje = in.readLine();
                            if (mensaje != null) {
                                System.out.println("Nodo " + ip + ":" + puerto + " recibió: " + mensaje);
                                if (mensaje.startsWith("NEW_NODE:")) {
                                    // Procesa la incorporación de un nuevo nodo a la red
                                    String nodeData = mensaje.substring("NEW_NODE:".length()).trim();
                                    String[] parts = nodeData.split(":");
                                    if (parts.length == 2) {
                                        String newIp = parts[0].trim();
                                        int newPort = Integer.parseInt(parts[1].trim());
                                        NodoInfo nuevoNodo = new NodoInfo(newIp, newPort);
                                        if (!nodos.contains(nuevoNodo)) {
                                            nodos.add(nuevoNodo);
                                            System.out.println("Nodo " + ip + ":" + puerto + " agregó nuevo nodo: " + nuevoNodo);
                                        }
                                    }
                                    // Si es nodo maestro, sincroniza la red enviando la lista actualizada, blockchain y transacciones pendientes
                                    if (master) {
                                        String listStr = nodos.toString();
                                        String blockchainSerialized = serializeBlockchain();
                                        String pendingSerialized = serializePendingTransactions();
                                        out.println("SYNC:" + listStr + "||" + blockchainSerialized + "||" + pendingSerialized);
                                        enviarListaActualizada();
                                    }
                                } else if (mensaje.startsWith("UPDATE_LIST:")) {
                                    // Actualiza la lista de nodos conocidos
                                    String listContent = mensaje.substring("UPDATE_LIST:".length()).trim();
                                    listContent = listContent.replace("[", "").replace("]", "");
                                    String[] nodeParts = listContent.split(",");
                                    for (String nodeStr : nodeParts) {
                                        nodeStr = nodeStr.trim();
                                        if (!nodeStr.isEmpty()) {
                                            String[] ipPort = nodeStr.split(":");
                                            if (ipPort.length == 2) {
                                                String nodeIp = ipPort[0].trim();
                                                int nodePort = Integer.parseInt(ipPort[1].trim());
                                                NodoInfo newNode = new NodoInfo(nodeIp, nodePort);
                                                if (!nodos.contains(newNode)) {
                                                    nodos.add(newNode);
                                                    System.out.println("Nodo " + ip + ":" + puerto + " actualizado con nodo: " + newNode);
                                                }
                                            }
                                        }
                                    }
                                } else if (mensaje.startsWith("NEW_TX:")) {
                                    // Procesa la recepción de una nueva transacción
                                    String txData = mensaje.substring("NEW_TX:".length());
                                    String[] txParts = txData.split(",");
                                    if (txParts.length >= 5) {
                                        try {
                                            int id1 = Integer.parseInt(txParts[0].trim());
                                            int id2 = Integer.parseInt(txParts[1].trim());
                                            double valor = Double.parseDouble(txParts[2].trim());
                                            long timestamp = Long.parseLong(txParts[3].trim());
                                            String ident = txParts[4].trim();
                                            Transaccion tx = new Transaccion();
                                            tx.idPersona1 = id1;
                                            tx.idPersona2 = id2;
                                            tx.valor = valor;
                                            tx.fecha = new Date(timestamp);
                                            tx.IdentTrans = ident;
                                            if (!pendingTransactions.contains(tx) && pendingTransactions.size() < 4) {
                                                pendingTransactions.add(tx);
                                                System.out.println("Nodo " + ip + ":" + puerto + " agregó transacción: " + tx.toString());
                                            } else {
                                                System.out.println("Nodo " + ip + ":" + puerto + " ya tiene 4 transacciones pendientes o transacción duplicada.");
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else if (mensaje.startsWith("NEW_BLOCK:")) {
                                    // Procesa la recepción de un nuevo bloque minado
                                    String content = mensaje.substring("NEW_BLOCK:".length());
                                    String[] parts = content.split("\\|\\|");
                                    if (parts.length == 2) {
                                        String serializedBlock = parts[0];
                                        String receivedHash = parts[1];
                                        Bloque bloqueRecibido = deserializeBlock(serializedBlock);
                                        if (bloqueRecibido != null) {
                                            String computedHash = Bloque.computeHash(bloqueRecibido);
                                            if (computedHash.equals(receivedHash)) {
                                                if (!bloqueExiste(bloqueRecibido)) {
                                                    listaTBloques.add(bloqueRecibido);
                                                    pendingTransactions.removeAll(bloqueRecibido.listaTransacciones);
                                                    System.out.println("Nodo " + ip + ":" + puerto + " agregó bloque minado con hash: " + receivedHash);
                                                }
                                            } else {
                                                System.out.println("Nodo " + ip + ":" + puerto + " recibió bloque con hash inválido.");
                                            }
                                        }
                                    }
                                }
                            }
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            } catch (IOException e) {
                System.err.println("Error iniciando la escucha en nodo " + ip + ":" + puerto);
                e.printStackTrace();
            }
        });
        escuchaThread.start();
    }

    /**
     * Conecta el nodo al nodo maestro para sincronizar la red, blockchain y transacciones pendientes.
     */
    public void conectarAlMaestro() {
        if (puerto == nodoMaestro) return;
        try {
            Socket socket = new Socket(ipMaestro, nodoMaestro);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println("NEW_NODE:" + ip + ":" + puerto);
            String response = in.readLine();
            if (response != null && response.startsWith("SYNC:")) {
                String syncData = response.substring("SYNC:".length());
                String[] syncParts = syncData.split("\\|\\|");
                if (syncParts.length >= 3) {
                    String listContent = syncParts[0].replace("[", "").replace("]", "");
                    String[] parts = listContent.split(",");
                    nodos.clear();
                    for (String nodeStr : parts) {
                        nodeStr = nodeStr.trim();
                        if (!nodeStr.isEmpty()) {
                            String[] ipPort = nodeStr.split(":");
                            if (ipPort.length == 2) {
                                String nodeIp = ipPort[0].trim();
                                int nodePort = Integer.parseInt(ipPort[1].trim());
                                NodoInfo node = new NodoInfo(nodeIp, nodePort);
                                if (!nodos.contains(node)) {
                                    nodos.add(node);
                                }
                            }
                        }
                    }
                    System.out.println("Nodo " + ip + ":" + puerto + " sincronizó lista de nodos: " + nodos);
                    
                    String blockchainSerialized = syncParts[1];
                    deserializeBlockchain(blockchainSerialized);
                    System.out.println("Nodo " + ip + ":" + puerto + " sincronizó blockchain con " + listaTBloques.size() + " bloques.");
                    
                    String pendingSerialized = syncParts[2];
                    deserializePendingTransactions(pendingSerialized);
                    System.out.println("Nodo " + ip + ":" + puerto + " sincronizó " + pendingTransactions.size() + " transacciones pendientes.");
                }
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("Error conectando al nodo maestro desde nodo " + ip + ":" + puerto);
            e.printStackTrace();
        }
    }

    /**
     * Envía la lista actualizada de nodos a todos los nodos conocidos.
     */
    public void enviarListaActualizada() {
        for (NodoInfo nodo : nodos) {
            if (nodo.port == puerto && nodo.ip.equals(ip))
                continue;
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(nodo.ip, nodo.port), 2000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("UPDATE_LIST:" + nodos.toString());
                socket.close();
            } catch (IOException e) {
                System.err.println("Error enviando lista desde nodo " + ip + ":" + puerto + " a nodo " + nodo);
            }
        }
    }

    /**
     * Intenta conectar el nodo a cada uno de los nodos conocidos para actualizar la lista.
     */
    public void conectarAConocidos() {
        for (NodoInfo nodo : nodos) {
            if (nodo.port == puerto && nodo.ip.equals(ip))
                continue;
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(nodo.ip, nodo.port), 2000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("UPDATE_LIST:" + nodos.toString());
                socket.close();
            } catch (IOException e) {
                System.err.println("Error conectando desde nodo " + ip + ":" + puerto + " a nodo " + nodo);
            }
        }
    }

    /**
     * Crea una nueva transacción y la difunde a los demás nodos.
     * @param id1 ID de la primera persona.
     * @param id2 ID de la segunda persona.
     * @param valor monto de la transacción.
     */
    public void crearTransaccion(int id1, int id2, double valor) {
        if (pendingTransactions.size() >= 4) {
            System.out.println("Ya hay 4 transacciones pendientes. Minar bloque para continuar.");
            return;
        }
        Transaccion tx = new Transaccion();
        tx.idPersona1 = id1;
        tx.idPersona2 = id2;
        tx.valor = valor;
        tx.fecha = new Date();
        tx.IdentTrans = UUID.randomUUID().toString();
        pendingTransactions.add(tx);
        System.out.println("Transacción creada: " + tx.toString());
        broadcastTransaccion(tx);
    }

    /**
     * Difunde la transacción a todos los nodos conocidos.
     * @param tx transacción a difundir.
     */
    public void broadcastTransaccion(Transaccion tx) {
        String txStr = tx.idPersona1 + "," + tx.idPersona2 + "," + tx.valor + "," + tx.fecha.getTime() + "," + tx.IdentTrans;
        for (NodoInfo nodo : nodos) {
            if (nodo.port == puerto && nodo.ip.equals(ip))
                continue;
            try {
                Socket socket = new Socket(nodo.ip, nodo.port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("NEW_TX:" + txStr);
                socket.close();
            } catch (IOException e) {
                System.err.println("Error enviando transacción desde nodo " + ip + ":" + puerto + " a nodo " + nodo);
            }
        }
    }

    /**
     * Minera un nuevo bloque con las transacciones pendientes y lo difunde a la red.
     */
    public void minarBloque() {
        if (pendingTransactions.isEmpty()) {
            System.out.println("No hay transacciones pendientes para minar.");
            return;
        }
        Bloque nuevoBloque = new Bloque();
        nuevoBloque.listaTransacciones = new ArrayList<>(pendingTransactions);
        if (listaTBloques.isEmpty()) {
            nuevoBloque.hashAnterior = null;
        } else {
            Bloque ultimo = listaTBloques.get(listaTBloques.size() - 1);
            nuevoBloque.hashAnterior = Bloque.computeHash(ultimo);
        }
        nuevoBloque.fechaCreaccion = new Date();
        int nonceInicial = (int) (Math.random() * 100000);
        nuevoBloque.nonce = nonceInicial;
        String hash;
        while (true) {
            hash = Bloque.computeHash(nuevoBloque);
            System.out.println("Minando: nonce = " + nuevoBloque.nonce + ", hash = " + hash);
            if (hash.startsWith("000"))
                break;
            nuevoBloque.nonce++;
        }
        listaTBloques.add(nuevoBloque);
        pendingTransactions.clear();
        System.out.println("Bloque minado con hash: " + hash);
        broadcastBloque(nuevoBloque, hash);
    }

    /**
     * Difunde el bloque minado a todos los nodos conocidos.
     * @param bloque bloque minado.
     * @param hash hash calculado del bloque.
     */
    public void broadcastBloque(Bloque bloque, String hash) {
        String serializedBlock = serializeBlock(bloque);
        String message = "NEW_BLOCK:" + serializedBlock + "||" + hash;
        for (NodoInfo nodo : nodos) {
            if (nodo.port == puerto && nodo.ip.equals(ip))
                continue;
            try {
                Socket socket = new Socket(nodo.ip, nodo.port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(message);
                socket.close();
            } catch (IOException e) {
                System.err.println("Error enviando bloque desde nodo " + ip + ":" + puerto + " a nodo " + nodo);
            }
        }
    }

    /**
     * Serializa un bloque para poder enviarlo por la red.
     * @param block bloque a serializar.
     * @return representación en String del bloque.
     */
    private String serializeBlock(Bloque block) {
        String serialized = (block.hashAnterior == null ? "null" : block.hashAnterior) + "|"
                + block.fechaCreaccion.getTime() + "|"
                + block.nonce;
        if(!block.listaTransacciones.isEmpty()){
            StringBuilder txsSerialized = new StringBuilder();
            for (int i = 0; i < block.listaTransacciones.size(); i++) {
                Transaccion tx = block.listaTransacciones.get(i);
                txsSerialized.append(tx.idPersona1).append(",")
                             .append(tx.idPersona2).append(",")
                             .append(tx.valor).append(",")
                             .append(tx.fecha.getTime()).append(",")
                             .append(tx.IdentTrans);
                if (i < block.listaTransacciones.size() - 1) {
                    txsSerialized.append("~");
                }
            }
            serialized += "|" + txsSerialized.toString();
        }
        return serialized;
    }
    
    /**
     * Serializa la blockchain completa para sincronizar con otros nodos.
     * @return cadena que representa la blockchain.
     */
    private String serializeBlockchain() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < listaTBloques.size(); i++) {
            sb.append(serializeBlock(listaTBloques.get(i)));
            if (i < listaTBloques.size() - 1) {
                sb.append(";;");
            }
        }
        return sb.toString();
    }

    /**
     * Serializa una transacción.
     * @param tx transacción a serializar.
     * @return representación en String de la transacción.
     */
    private String serializeTransaction(Transaccion tx) {
        return tx.idPersona1 + "," + tx.idPersona2 + "," + tx.valor + "," + tx.fecha.getTime() + "," + tx.IdentTrans;
    }

    /**
     * Serializa las transacciones pendientes.
     * @return cadena que representa las transacciones pendientes.
     */
    private String serializePendingTransactions() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pendingTransactions.size(); i++) {
            sb.append(serializeTransaction(pendingTransactions.get(i)));
            if (i < pendingTransactions.size() - 1) {
                sb.append("~~");
            }
        }
        return sb.toString();
    }

    /**
     * Deserializa un bloque recibido en formato String.
     * @param s cadena serializada.
     * @return objeto Bloque o null si falla la deserialización.
     */
    private Bloque deserializeBlock(String s) {
        try {
            String[] parts = s.split("\\|", 4);
            if (parts.length < 3)
                return null;
            Bloque block = new Bloque();
            block.hashAnterior = parts[0].equals("null") ? null : parts[0];
            block.fechaCreaccion = new Date(Long.parseLong(parts[1]));
            block.nonce = Integer.parseInt(parts[2]);
            block.listaTransacciones = new ArrayList<>();
            if (parts.length == 4 && !parts[3].isEmpty()) {
                String txsSerialized = parts[3];
                String[] txs = txsSerialized.split("~");
                for (String txStr : txs) {
                    String[] fields = txStr.split(",");
                    if (fields.length < 5)
                        continue;
                    Transaccion tx = new Transaccion();
                    tx.idPersona1 = Integer.parseInt(fields[0]);
                    tx.idPersona2 = Integer.parseInt(fields[1]);
                    tx.valor = Double.parseDouble(fields[2]);
                    tx.fecha = new Date(Long.parseLong(fields[3]));
                    tx.IdentTrans = fields[4];
                    block.listaTransacciones.add(tx);
                }
            }
            return block;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserializa la blockchain recibida.
     * @param s cadena que representa la blockchain.
     */
    private void deserializeBlockchain(String s) {
        listaTBloques.clear();
        if (s.isEmpty())
            return;
        String[] blocks = s.split(";;");
        for (String blockStr : blocks) {
            Bloque block = deserializeBlock(blockStr);
            if (block != null) {
                listaTBloques.add(block);
            }
        }
    }
    
    /**
     * Deserializa las transacciones pendientes recibidas.
     * @param s cadena que representa las transacciones pendientes.
     */
    private void deserializePendingTransactions(String s) {
        pendingTransactions.clear();
        if (s.isEmpty())
            return;
        String[] txs = s.split("~~");
        for (String txStr : txs) {
            String[] fields = txStr.split(",");
            if (fields.length < 5)
                continue;
            Transaccion tx = new Transaccion();
            tx.idPersona1 = Integer.parseInt(fields[0]);
            tx.idPersona2 = Integer.parseInt(fields[1]);
            tx.valor = Double.parseDouble(fields[2]);
            tx.fecha = new Date(Long.parseLong(fields[3]));
            tx.IdentTrans = fields[4];
            pendingTransactions.add(tx);
        }
    }

    /**
     * Verifica si un bloque ya existe en la blockchain comparando los hashes.
     * @param bloque bloque a verificar.
     * @return true si el bloque ya existe; false en caso contrario.
     */
    private boolean bloqueExiste(Bloque bloque) {
        String hashNuevo = Bloque.computeHash(bloque);
        for (Bloque b : listaTBloques) {
            if (Bloque.computeHash(b).equals(hashNuevo)) {
                return true;
            }
        }
        return false;
    }
}
