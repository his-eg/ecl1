package net.sf.ecl1.utilities.preferences;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;

/**
 * Verify a text control by trying to open a socket
 *
 * @author keunecke
 *
 */
public class NetUtil {

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
            URL url = new URL(urlToCheck);
            socket = new Socket(url.getHost(), url.getPort());
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
