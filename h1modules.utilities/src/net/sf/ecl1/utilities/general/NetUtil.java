package net.sf.ecl1.utilities.general;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Verify a text control by trying to open a socket
 *
 * @author keunecke
 *
 */
public class NetUtil {

    private static final Map<String, Integer> defaultPorts;
    static {
        defaultPorts = Maps.newHashMap();
        defaultPorts.put("ssh", Integer.valueOf(22));
        defaultPorts.put("http", Integer.valueOf(80));
        defaultPorts.put("https", Integer.valueOf(443));
    }

    /**
     * determine if socket can be opened
     *
     * @param serverUrl
     * @return
     */
    public static boolean canOpenSocket(String serverUrl) {
        final Socket socket;

        try {
            //exchange protocol from ssh to http as ssh is not a supported protocol
            String urlToCheck = serverUrl.replace("ssh://", "http://");
            int positionOfProtocollDelimiter = serverUrl.indexOf("://");
            String protocoll = serverUrl.substring(0, positionOfProtocollDelimiter);

            URL url = new URL(urlToCheck);
            int port = url.getPort();
            if (port <= 0) {
                port = defaultPorts.get(protocoll).intValue();
            }
            socket = new Socket(url.getHost(), port);
        } catch (IOException e) {
            return false;
        }

        try {
            socket.close();
        } catch (IOException e) {
            // will never happen, it's thread-safe
        }

        return true;
    }

}
