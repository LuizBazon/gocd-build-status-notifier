package com.tw.go.plugin;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.tw.go.plugin.provider.Provider;
import com.tw.go.plugin.provider.github.GitHubProvider;
import com.tw.go.plugin.provider.stash.StashProvider;
import com.tw.go.plugin.setting.PluginConfigurationView;
import com.tw.go.plugin.setting.PluginSettings;
import com.tw.go.plugin.util.JSONUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tw.go.plugin.BuildStatusNotifierPlugin.PLUGIN_SETTINGS_GET_CONFIGURATION;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class BuildStatusNotifierPluginTest {
    @Mock
    private GoApplicationAccessor goApplicationAccessor;
    @Mock
    private Provider provider;

    private BuildStatusNotifierPlugin plugin;

    @Before
    public void setUp() {
        initMocks(this);

        plugin = new BuildStatusNotifierPlugin();

        DefaultGoApiResponse pluginSettingsResponse = new DefaultGoApiResponse(200);
        pluginSettingsResponse.setResponseBody(JSONUtils.toJSON(new HashMap<String, String>()));
        when(goApplicationAccessor.submit(any(GoApiRequest.class))).thenReturn(pluginSettingsResponse);
        when(provider.pluginId()).thenReturn(GitHubProvider.PLUGIN_ID);
        when(provider.pollerPluginId()).thenReturn(GitHubProvider.GITHUB_PR_POLLER_PLUGIN_ID);

        plugin.initializeGoApplicationAccessor(goApplicationAccessor);
        plugin.setProvider(provider);
    }


    private GoPluginApiRequest createRequest(final String name) {
        return new GoPluginApiRequest() {
            @Override
            public String extension() {
                return null;
            }

            @Override
            public String extensionVersion() {
                return null;
            }

            @Override
            public String requestName() {
                return name;
            }

            @Override
            public Map<String, String> requestParameters() {
                return null;
            }

            @Override
            public Map<String, String> requestHeaders() {
                return null;
            }

            @Override
            public String requestBody() {
                return null;
            }
        };
    }

    private Map createRequestBodyMap(String url, String username, String revision, String prId, String pipelineName, String pipelineCounter, String stageName, String stageCounter, String stageResult) {
        Map materialRevisionMap = new HashMap();
        Map materialMap = new HashMap();
        materialMap.put("type", "scm");
        materialMap.put("plugin-id", "github.pr");
        Map configurationMap = new HashMap();
        configurationMap.put("url", url);
        configurationMap.put("username", username);
        materialMap.put("scm-configuration", configurationMap);
        materialRevisionMap.put("material", materialMap);

        List modifications = new ArrayList();
        Map modificationMap = new HashMap();
        modificationMap.put("revision", revision);
        Map modificationDataMap = new HashMap();
        modificationDataMap.put("PR_ID", prId);
        modificationMap.put("data", modificationDataMap);
        modifications.add(modificationMap);
        materialRevisionMap.put("modifications", modifications);

        Map pipelineMap = new HashMap();
        List buildCause = new ArrayList();
        buildCause.add(materialRevisionMap);
        pipelineMap.put("build-cause", buildCause);

        Map stageMap = new HashMap();
        stageMap.put("name", stageName);
        stageMap.put("counter", stageCounter);
        stageMap.put("result", stageResult);
        pipelineMap.put("stage", stageMap);

        pipelineMap.put("name", pipelineName);
        pipelineMap.put("counter", pipelineCounter);

        Map requestBody = new HashMap();
        requestBody.put("pipeline", pipelineMap);
        return requestBody;
    }

    private DefaultGoPluginApiRequest createGoPluginAPIRequest(Map requestBody) {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(BuildStatusNotifierPlugin.EXTENSION_NAME, "1.0", BuildStatusNotifierPlugin.REQUEST_STAGE_STATUS);
        request.setRequestBody(JSONUtils.toJSON(requestBody));
        return request;
    }
}
