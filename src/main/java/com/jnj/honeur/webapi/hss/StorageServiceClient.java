package com.jnj.honeur.webapi.hss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnj.honeur.webapi.cohortdefinition.CohortGenerationResults;
import com.jnj.honeur.webapi.hssserviceuser.HSSServiceUserEntity;
import com.jnj.honeur.webapi.hssserviceuser.HSSServiceUserRepository;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.ohdsi.webapi.service.CohortDefinitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

@Component
public class StorageServiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceClient.class);

    private RestTemplate restTemplate;

    @Autowired
    private HSSServiceUserRepository hssServiceUserRepository;

    @Value("${datasource.hss.url}")
    private String storageServiceApi;

    @Value("${webapi.central}")
    private boolean webapiCentral;

    @Value("${security.token.expiration}")
    private int EXPIRATION_TIME;


    public StorageServiceClient() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(csf)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);
        restTemplate = new RestTemplate(requestFactory);
    }

    public void setStorageServiceApi(String storageServiceApi) {
        this.storageServiceApi = storageServiceApi;
    }
    public void setWebapiCentral(boolean webapiCentral) {
        this.webapiCentral = webapiCentral;
    }
    public void setHssServiceUserRepository(HSSServiceUserRepository hssServiceUserRepository) {
        this.hssServiceUserRepository = hssServiceUserRepository;
    }

    public String saveResults(String token, File results, String uuid) {
        if (!webapiCentral) {
            token = getStorageServiceToken();
        }
        String endpoint = "/cohort-results/" + uuid;
        try {
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = createHttpEntity(token, results);

            return restTemplate.exchange(storageServiceApi + endpoint,
                    HttpMethod.POST, requestEntity, String.class).getHeaders().getLocation().getPath();
        } catch (RestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    public String saveCohort(String token, File results, final UUID groupKey) {
        if (!webapiCentral) {
            token = getStorageServiceToken();
        }
        String endpoint = "/cohort-definitions/" + groupKey;
        try {
            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = createHttpEntity(token, results);

            return restTemplate.exchange(storageServiceApi + endpoint,
                    HttpMethod.POST, requestEntity, StorageInformationItem.class).getHeaders()
                    .getLocation().getPath().replace("/cohort-definitions/", "");
        } catch (RestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    private HttpEntity<LinkedMultiValueMap<String, Object>> createHttpEntity(String token, File file) {
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", new FileSystemResource(file.getAbsolutePath()));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("token", token.replace("Bearer ", ""));

        return new HttpEntity<>(map, headers);
    }

    public List<CohortDefinitionStorageInformationItem> getCohortDefinitionImportList(String token) {
        if (!webapiCentral) {
            token = getStorageServiceToken();
        }
        String endpoint = "/cohort-definitions/list";

        return Arrays.asList(restTemplate.exchange(storageServiceApi + endpoint, HttpMethod.GET,
                getTokenHeader(token), CohortDefinitionStorageInformationItem[].class).getBody());
    }

    public CohortDefinitionService.CohortDefinitionDTO getCohortDefinition(String token, String uuid) {
        if (!webapiCentral) {
            token = getStorageServiceToken();
        }
        String endpoint = "/cohort-definitions/" + uuid;
        return restTemplate
                .exchange(storageServiceApi + endpoint, HttpMethod.GET, getTokenHeader(token), CohortDefinitionService.CohortDefinitionDTO.class)
                .getBody();
    }

    public List<StorageInformationItem> getCohortDefinitionResultsImportList(String token, UUID uuid) {
        if (!webapiCentral) {
            token = getStorageServiceToken();
        }
        try {
            String endpoint = "/cohort-results/list/" + uuid + "?reverseOrder=true";
            return Arrays.asList(restTemplate
                    .exchange(storageServiceApi + endpoint, HttpMethod.GET, getTokenHeader(token),
                            StorageInformationItem[].class).getBody());
        } catch (RestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public CohortGenerationResults getCohortGenerationResults(String token, String definitionUuid, String resultsUuid) throws IOException {
        if (!webapiCentral) {
            token = getStorageServiceToken();
        }
        String endpoint = "/cohort-results/" + definitionUuid + "/" + resultsUuid;
        String response = restTemplate
                .exchange(storageServiceApi + endpoint, HttpMethod.GET, getTokenHeader(token), String.class)
                .getBody();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response, CohortGenerationResults.class);
    }

    public String getStorageServiceToken() {
        JsonNode tokenResponse = restTemplate
                .exchange(storageServiceApi + "/login", HttpMethod.GET, getBasicAuthenticationHeader(),
                        JsonNode.class).getBody();
        return tokenResponse.path("token").asText();
    }

    boolean deleteStorageFile(String token, String uuid) {
        try {
            String serviceUrl = storageServiceApi + "/" + uuid;
            restTemplate.exchange(serviceUrl, HttpMethod.DELETE, getTokenHeader(token), String.class);
            return true;
        } catch (RestClientException e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
    }

    private HttpEntity getTokenHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token.replace("Bearer ", ""));

        return new HttpEntity(headers);
    }

    private HttpEntity getBasicAuthenticationHeader() {
        Iterator<HSSServiceUserEntity> hssServiceUserEntities = hssServiceUserRepository.findAll().iterator();
        if(!hssServiceUserEntities.hasNext()){
            throw new IllegalStateException("No HSS service user defined.");
        }
        final HSSServiceUserEntity hssServiceUser = hssServiceUserEntities.next();
        return new HttpEntity(new HttpHeaders() {{
            String auth = hssServiceUser.getUsername() + ":" + hssServiceUser.getPlainTextPassword();
            byte[] encodedAuth = Base64.encodeBase64(
                    auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String(encodedAuth);
            set("Authorization", authHeader);
        }});
    }

}
