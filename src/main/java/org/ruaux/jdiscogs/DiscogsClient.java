package org.ruaux.jdiscogs;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ruaux.jdiscogs.model.Master;
import org.ruaux.jdiscogs.model.Release;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DiscogsClient {

    private @Setter
    JDiscogsProperties props;
    private @Setter
    RestTemplate restTemplate;
    private long rateLimitLastTime;
    private int rateLimitRemaining;

    public Master getMaster(String masterId) {
        return getEntity("masters", Master.class, masterId);
    }

    public Release getRelease(String releaseId) {
        return getEntity("releases", Release.class, releaseId);
    }

    /**
     * synchronized to avoid running into this bug:
     * https://bugs.openjdk.java.net/browse/JDK-8213202
     */
    private synchronized <T> T getEntity(String entity, Class<T> entityClass, String id) {
        boolean after1Min = (System.currentTimeMillis() - rateLimitLastTime) > 60000;
        if (rateLimitRemaining > 1 || after1Min) {
            Map<String, String> uriParams = new HashMap<>();
            uriParams.put("entity", entity);
            uriParams.put("id", id);
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(props.getApiUrl()).queryParam("token",
                    props.getToken());
            URI uri = builder.buildAndExpand(uriParams).toUri();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", props.getUserAgent());
            RequestEntity<Object> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, uri);
            ResponseEntity<T> response = restTemplate.exchange(requestEntity, entityClass);
            HttpHeaders responseHeaders = response.getHeaders();
            List<String> remaining = responseHeaders.get("X-Discogs-Ratelimit-Remaining");
            int newRemaining = remaining == null ? Integer.MAX_VALUE : Integer.parseInt(remaining.get(0));
            this.rateLimitLastTime = System.currentTimeMillis();
            if (rateLimitRemaining == 0 || newRemaining < rateLimitRemaining || after1Min) {
                log.info("RateLimitRemaining -> {}", newRemaining);
                this.rateLimitRemaining = newRemaining;
            }
            return response.getBody();
        }
        return null;
    }

}
