import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import com.sun.tools.doclets.internal.toolkit.util.DocFinder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * @author Diego Urrutia Astorga <durrutia@ucn.cl>
 * @version 20170330130700
 */
public class ProcessRequestRunnable implements Runnable {

    /**
     * Logger de la clase
     */
    private static final Logger log = LoggerFactory.getLogger(ProcessRequestRunnable.class);

    /**
     * Contenedor de Chat
     */
    private static final List<String> chats = Lists.newArrayList();

    /**
     * Socket asociado al cliente.
     */
    private Socket socket;

    /**
     * Atributos del cliente
     */
    private int colorFont;
    private String avatar;
    private String apodo;




    /**
     * Constructor
     *
     * @param socket
     */
    public ProcessRequestRunnable(final Socket socket, final int indice) {
        this.socket = socket;
        if(indice <10) {
            this.apodo = "ChatUsser00" + indice;
        }else
        {
            this.apodo = "ChatUsser0" + indice;
        }
    }


    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {

        // Cronometro ..
        final Stopwatch stopWatch = Stopwatch.createStarted();

        log.debug("Connection from {} in port {}.", socket.getInetAddress(), socket.getPort());


            // A dormir!
            try {
                Thread.sleep(RandomUtils.nextInt(100, 500));
            } catch (InterruptedException e) {
                log.error("Error in sleeping", e);
            }

            try {
                processRequest(this.socket, this.apodo);
            } catch (Exception ex) {
                log.error("Error processing request", ex);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Nothing here
                }
            }

            log.debug("Request timex: {}.", stopWatch);

        //Remover el cliente de la lista

    }

    /**
     * Procesar peticion
     *
     * @param socket
     */



    private static void processRequest(final Socket socket, final String apodo) throws IOException {

        // Iterador de la peticion
        final LineIterator lineIterator = IOUtils.lineIterator(socket.getInputStream(), Charset.defaultCharset());

        // Peticion
        final String request = getRequest(lineIterator);
        log.debug("Request detected: {}", request);

        // Output
        final OutputStream outputStream = IOUtils.buffer(socket.getOutputStream());

        log.debug("Writing data for: {}", request);

        // HTTP header
        writeHeader(outputStream);

        // Dividir el request
        final String[] request3 = StringUtils.split(request);

        // String from http protocol: "GET /chat HTTP/1.1"

        // Cada componente
        final String verbo = request3[0];
        final String uri = request3[1];
        final String version = request3[2];

        log.warn(" verbo: {}", verbo);


        // Deteccion de version
        if (!StringUtils.equals("HTTP/1.1", version)) {
            log.warn("Wrong version: {}", version);
        }

        // Si la peticion incluye /chat
        if (StringUtils.startsWith(uri, "/chat")) {
            writeChat(outputStream, verbo, uri, apodo);
        } else {
            // HTTP Body
            writeBody(outputStream, request);
        }

        // Cierro el stream
        IOUtils.closeQuietly(outputStream);

    }


    /**
     * @param outputStream
     * @param verbo
     * @param uri
     */
    private static void writeChat(final OutputStream outputStream, final String verbo, final String uri, final String apodo) throws IOException {

        if (StringUtils.contains(uri, "/chat")) {

            Date date = new Date();
            String strDateFormat = "hh:mm:ss";
            DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
            String formattedDate= dateFormat.format(date);

            if( !StringUtils.substringAfter(uri, "chat?msgText=").equals("")) {
                String msg = formattedDate + " " + apodo + ":" + StringUtils.substringAfter(uri, "chat?msgText=");


                log.debug("Msg to include: {}", msg);

                // Sincronizacion
                synchronized (chats) {
                    chats.add(msg);
                }
            }
        }

        // Listado completo de chat
        final StringBuffer sb = new StringBuffer();

        // Linea de chat
        final String chatline = readFile("chatline.html");

        // Siempre el mismo comportamiento
        synchronized (chats) {
            for (String line : chats) {
                sb.append(StringUtils.replace(chatline, "CONTENT", line));
                sb.append("\r\n");
            }
        }



        // Contenido completo
        final String content = readFile("index.html");

        final String contentChat = StringUtils.replace(content, "<!-- CHAT_CONTENT-->", sb.toString());

        // Envio el contenido
        IOUtils.write(contentChat + "\r\n", outputStream, Charset.defaultCharset());

    }

    public static void recibeMsg(final Socket socket) throws  IOException{


        final  OutputStream outputStream = IOUtils.buffer(socket.getOutputStream());

        // Listado completo de chat
        final StringBuffer sb = new StringBuffer();

        // Linea de chat
        final String chatline = readFile("chatline.html");

        // Siempre el mismo comportamiento
        synchronized (chats) {
            for (String line : chats) {
                sb.append(StringUtils.replace(chatline, "CONTENT", line));
                sb.append("\r\n");
            }
        }



        // Contenido completo
        final String content = readFile("index.html");

        final String contentChat = StringUtils.replace(content, "<!-- CHAT_CONTENT-->", sb.toString());

        // Envio el contenido
        IOUtils.write(contentChat + "\r\n", outputStream, Charset.defaultCharset());
    }


    /**
     * @param filename
     * @return the contenido del archivo.
     */
    private static String readFile(final String filename) {

        // URL del index
        URL url;
        try {
            url = Resources.getResource(filename);
        } catch (IllegalArgumentException ex) {
            log.error("Can't find file", ex);
            return null;
        }

        // Contenido
        try {
            return IOUtils.toString(url, Charset.defaultCharset());
        } catch (IOException ex) {
            log.error("Error in read", ex);
            return null;
        }

    }

    /**
     * Obtengo la linea de peticion de request.
     *
     * @param lineIterator
     * @return the request.
     */
    private static String getRequest(LineIterator lineIterator) {

        String request = null;
        int n = 0;

        log.debug("entramos al get Request");


        while (lineIterator.hasNext()) {

            // Leo linea a linea
            final String line = lineIterator.nextLine();
            log.debug("Line {}: {}", ++n, line);

            // Guardo la peticion de la primera linea
            if (n == 1) {
                request = line;
            }

            // Termine la peticion si llegue al final de la peticion
            if (StringUtils.isEmpty(line)) {
                break;
            }

        }

        return request;
    }

    /**
     * Escribe el encabezado del protocolo HTTP.
     *
     * @param outputStream
     * @throws IOException
     */
    private static void writeHeader(OutputStream outputStream) throws IOException {

        // Header
        IOUtils.write("HTTP/1.0 200 OK\r\n", outputStream, Charset.defaultCharset());
        IOUtils.write("Content-type: text/html\r\n", outputStream, Charset.defaultCharset());

        // end-header
        IOUtils.write("\r\n", outputStream, Charset.defaultCharset());

    }

    /**
     * Escribe el body del encabezado.
     *
     * @param outputStream
     * @param request
     */
    private static void writeBody(OutputStream outputStream, String request) throws IOException {

        // Detect if chat is what we want


        // Body
        final String body = "<html><head><title>WebServer v1.0</title></head><body><h3>Result:</h3><pre>CONTENT</pre></body></html>";

        final String random = RandomStringUtils.randomAlphabetic(100);

        final String result = StringUtils.replace(body, "CONTENT", random);

        IOUtils.write(result + "\r\n", outputStream, Charset.defaultCharset());

    }


}
