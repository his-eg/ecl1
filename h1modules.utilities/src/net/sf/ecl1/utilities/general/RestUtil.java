package net.sf.ecl1.utilities.general;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import h1modules.utilities.utils.Activator;

/**
 * simple util for calling rest services
 *
 * @author keunecke
 *
 */
public class RestUtil {

    private static final ConsoleLogger logger = new ConsoleLogger(Activator.getDefault().getLog(), Activator.PLUGIN_ID);

    public static InputStream getJsonStream(final String defaultTarget) {
        try {
            HttpGet get = new HttpGet(defaultTarget);
            ResponseHandler<InputStream> responseHandler = new ResponseHandler<InputStream>() {
                @Override
                public InputStream handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? new ByteArrayInputStream(EntityUtils.toString(entity).getBytes()) : null;
                    }
                    throw new ClientProtocolException("Unexpected response status '" + status + "' expected status <= 200 and < 300 for URL " + defaultTarget);
                }
            };
            HttpClient c = new DefaultHttpClient();
            return c.execute(get, responseHandler);
        } catch (IOException e) {
    		logger.error2(e.getMessage(), e);
        }
        return null;
    }

}
