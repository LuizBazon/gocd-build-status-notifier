package br.com.easynvest.plugin.provider.bitbucket;

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.tw.go.plugin.BuildStatusNotifierPlugin;
import com.tw.go.plugin.provider.DefaultProvider;
import com.tw.go.plugin.setting.DefaultPluginConfigurationView;
import com.tw.go.plugin.setting.PluginSettings;
import com.tw.go.plugin.util.AuthenticationType;
import com.tw.go.plugin.util.HTTPClient;
import com.tw.go.plugin.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class BitbucketProvider extends DefaultProvider {
    public static final String PLUGIN_ID = "bitbucket.pr.status";
    public static final String BITBUCKET_PR_POLLER_PLUGIN_ID = "bitbucket.pr";

    public static final String IN_PROGRESS_STATE = "INPROGRESS";
    public static final String SUCCESSFUL_STATE = "SUCCESSFUL";
    public static final String FAILED_STATE = "FAILED";
    public static final String STOPPED_STATE = "STOPPED";
    
    private static final String URL_ROOT = "https://bitbucket.org/api/2.0/repositories/";
    
    private static Logger LOGGER = Logger.getLoggerFor(BitbucketProvider.class);

    private HTTPClient httpClient;

    public BitbucketProvider() {
        super(new DefaultPluginConfigurationView());
        httpClient = new HTTPClient();
    }

    public BitbucketProvider(HTTPClient httpClient) {
        super(new DefaultPluginConfigurationView());
        this.httpClient = httpClient;
    }

    @Override
    public String pluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String pollerPluginId() {
        return BITBUCKET_PR_POLLER_PLUGIN_ID;
    }

    @Override
    public void updateStatus(String pathUrl, PluginSettings pluginSettings, String prId, String revision, String pipelineStage,
                             String result, String trackbackURL) throws Exception {
    	String endPointToUse = pluginSettings.getEndPoint();
        String usernameToUse = pluginSettings.getUsername();
        String passwordToUse = pluginSettings.getPassword();
        
        String updateURL = String.format("%s/%s/commit/%s/statuses/build", endPointToUse, pathUrl, revision);
        LOGGER.info("Update URL status: " + updateURL);

        Map<String, String> params = new HashMap<String, String>();
        params.put("url", "http://www.easynvest.com.br");
        //params.put("url", trackbackURL);
        String state = getState(result);
        params.put("state", state);
        params.put("key", pipelineStage);
        params.put("name", pipelineStage);
        String requestBody = new GsonBuilder().create().toJson(params);
        LOGGER.info("requestBody: " + requestBody);

        httpClient.postRequest(updateURL, AuthenticationType.BASIC, usernameToUse, passwordToUse, requestBody);
        
        setStatusPR(pluginSettings, pathUrl, prId, state);
    }
    
    private void setStatusPR(PluginSettings pluginSettings, String pathUrl, String prId, String state) throws Exception {
    	String updateURL = String.format("%s/%s/pullrequests/%s/decline", pluginSettings.getEndPoint(), pathUrl, prId);
    	
    	if(state == FAILED_STATE){
    		LOGGER.info("Declining PR " + prId + "\n Repository: " + pathUrl);
    		String requestBody = new GsonBuilder().create().toJson(new Object());
    		httpClient.postRequest(updateURL, AuthenticationType.BASIC, pluginSettings.getUsername(), pluginSettings.getPassword(), requestBody);
    	}
    }
    
    /*private HttpHost getHttpHost(String url) throws Exception {
        URI uri = new URI(url);
        return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    }
    
    private AuthCache getAuthCache(AuthenticationType authenticationType, HttpHost target) {
        AuthCache authCache = new BasicAuthCache();
        if (authenticationType == AuthenticationType.BASIC) {
            authCache.put(target, new BasicScheme());
        } else {
            authCache.put(target, new DigestScheme());
        }
        return authCache;
    }
    
    public void postRequest(String updateURL, AuthenticationType authenticationType, String username, String password, String requestBody) throws Exception {
        CloseableHttpClient httpClient = null;
        try {
            HttpPost request = new HttpPost(updateURL);
            request.addHeader("content-type", "application/json");
            request.setEntity(new StringEntity(requestBody));

            HttpHost target = getHttpHost(updateURL);
            AuthCache authCache = getAuthCache(authenticationType, target);
            HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);

            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(target), new UsernamePasswordCredentials(username, password));
            httpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build();

            HttpResponse response = httpClient.execute(request, localContext);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode > 204) {
            	LOGGER.info(EntityUtils.toString(response.getEntity(), "UTF-8"));
                throw new RuntimeException("Error occurred. Status Code: " + statusCode);
            }
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }*/

    @Override
    public List<Map<String, Object>> validateConfig(Map<String, Object> fields) {
        return new ArrayList<Map<String, Object>>();
    }

    String getState(String result) {
        result = result == null ? "" : result;
        String state = IN_PROGRESS_STATE;
        if (result.equalsIgnoreCase("Passed")) {
            state = SUCCESSFUL_STATE;
        } else if (result.equalsIgnoreCase("Failed")) {
            state = FAILED_STATE;
        } else if (result.equalsIgnoreCase("Cancelled")) {
            state = STOPPED_STATE;
        }
        return state;
    }
}
