package com.sailthru.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sailthru.client.handler.JSONHandler;
import com.sailthru.client.handler.SailthruResponseHandler;
import com.sailthru.client.http.SailthruHandler;
import com.sailthru.client.http.SailthruHttpClient;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpVersion;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

/**
 *
 * @author Prajwal Tuladhar
 */
public abstract class AbstractSailthruClient {
    public static final String DEFAULT_API_URL = "https://api.sailthru.com";
    public static final int DEFAULT_HTTP_PORT = 80;
    public static final int DEFAULT_HTTPS_PORT = 443;
    public static final String DEFAULT_USER_AGENT = "Sailthru Java Client";
    public static final String VERSION = "1.0";
    public static final String DEFAULT_ENCODING = "UTF-8";

    protected static enum HandlerType { JSON };    //we can also add XML but who cares about it!
    public static enum HttpRequestMethod {GET, POST, DELETE}; //HTTP methods supported by Sailthru API

    protected String apiKey;
    protected String apiSecret;
    protected String apiUrl;

    protected SailthruHttpClient httpClient;

    private SailthruHandler handler;

    public AbstractSailthruClient(String apiKey, String apiSecret, String apiUrl) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.apiUrl = apiUrl;
        this.handler = new SailthruHandler(new JSONHandler());
        this.httpClient = create();
    }

    protected SailthruHttpClient create() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, DEFAULT_ENCODING);
        HttpProtocolParams.setUserAgent(params, DEFAULT_USER_AGENT);
        HttpProtocolParams.setUseExpectContinue(params, true);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(getScheme());

        ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager(schemeRegistry);
        return new SailthruHttpClient(connManager, params);
    }

    protected Scheme getScheme() {
        String scheme = null;
        try {
            URI uri = new URI(this.apiUrl);
            scheme = uri.getScheme();
        }
        catch (URISyntaxException e) {
            scheme = "http";
        }
        if (scheme.equals("https")) {
            return new Scheme(scheme, DEFAULT_HTTPS_PORT, SSLSocketFactory.getSocketFactory());
        }
        else {
            return new Scheme(scheme, DEFAULT_HTTP_PORT, PlainSocketFactory.getSocketFactory());
        }
    }

    protected Object httpRequest(String action, HttpRequestMethod method, Map<String, Object> data) throws IOException {
        String url = this.apiUrl + "/" + action;
        Map<String, String> params = new HashMap<String, String>();
        params.put("api_key", this.apiKey);
        params.put("format", "json");

        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, Object>>() {}.getType();
        
        params.put("json", gson.toJson(data, type));
        params.put("sig", getSignatureHash(params));

        System.out.println(params);

        return this.httpClient.executeHttpRequest(url, method, params, handler);
    }

    protected String getSignatureHash(Map<String, String> parameters) {
        ArrayList<String> values = new ArrayList<String>();

        StringBuilder data = new StringBuilder();
        data.append(this.apiSecret);

        for (Entry<String, String> entry : parameters.entrySet()) {
           values.add(entry.getValue());
        }

        Collections.sort(values);

        for( String value:values ) {
            data.append(value);
        }
        //System.out.println(data.toString());
        return SailthruUtil.md5(data.toString());
    }

    protected Object apiGet(String action, Map<String, Object> data) throws IOException {
        return httpRequest(action, HttpRequestMethod.GET, data);
    }

    protected Object apiPost(String action, Map<String, Object> data) throws IOException {
        return httpRequest(action, HttpRequestMethod.POST, data);
    }

    protected Object apiDelete(String action, Map<String, Object> data) throws IOException {
        return httpRequest(action, HttpRequestMethod.DELETE, data);
    }

    public void setResponseHandler(SailthruResponseHandler responseHandler) {
        this.handler.setSailthruResponseHandler(responseHandler);
    }
}