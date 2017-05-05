import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Diego Urrutia Astorga <durrutia@ucn.cl>
 * @version 20170330131600
 */
public class WebServerThreads extends Thread {

    /**
     * Logger de la clase
     */
    private static final Logger log = LoggerFactory.getLogger(WebServer.class);

    /**
     * Puerto de escucha
     */
    private static final Integer PORT = 9090;


    /**
     * Lista de clientes conectados
     */
    private static final List<String> ipClientes = Lists.newArrayList();
    private static final List<Socket> listaClientes = Lists.newArrayList();

    /**
     * Inicio del programa.
     *
     * @param args
     */

    public static void main(String[] args) throws IOException {

        log.debug("Starting ..");

        final ExecutorService executor = Executors.newFixedThreadPool(8);

        // Servidor en el puerto PORT
        final ServerSocket serverSocket = new ServerSocket(PORT);

        // Ciclo para atender a los clientes
        while (true) {

            log.debug("Waiting for connection in port {} ..", PORT);

            // 1 socket por peticion
            final Socket socket = serverSocket.accept();
            String ipCliente = socket.getInetAddress().getHostAddress();

            //el cliente no debe existir
            if(!ipClientes.contains(ipCliente)) {


                // Al executor ..
                final Runnable runnable = new ProcessRequestRunnable(socket, listaClientes.size() + 1);
                executor.execute(runnable);

                //Agregamos la ip y el cliente a sus listas correspondientes
                PrintStream ps = new PrintStream(socket.getOutputStream());
                ipClientes.add(ipCliente);
                listaClientes.add(socket);

                log.debug("Connection from {} in port {}.", socket.getInetAddress(), socket.getPort());
            }else
            {
                //el cliente ya existe
                int i = 0;
                //buscamos y capturamos al cliente
                while(!ipClientes.get(i).equals(ipCliente)){
                    i ++;
                }

                // Al executor ..
                //listaClientes.get(i).connect(serverSocket.getLocalSocketAddress());

                final Runnable runnable = new ProcessRequestRunnable(socket, i + 1);
                executor.execute(runnable);


                //log.debug("El usuario de IP {} volvió", ipCliente);
                //log.debug("Connection from {} in port {}.", socket.getInetAddress(), socket.getPort());
            }

            for (int i = 0; i < listaClientes.size(); i ++){

            }
        }
    }

}
