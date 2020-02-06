package org.ruaux.jdiscogs.api;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.ruaux.jdiscogs.api.model.Master;
import org.ruaux.jdiscogs.api.model.Release;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiscogsClient {

	private @Setter JDiscogsApiProperties props;
	private @Setter RestTemplate restTemplate;
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
	 * 
	 * @param entity
	 * @param entityClass
	 * @param id
	 * @return
	 */
	private synchronized <T> T getEntity(String entity, Class<T> entityClass, String id) {
		boolean after1Min = (System.currentTimeMillis() - rateLimitLastTime) > 60000;
		if (rateLimitRemaining > 1 || after1Min) {
			Map<String, String> uriParams = new HashMap<String, String>();
			uriParams.put("entity", entity);
			uriParams.put("id", id);
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(props.getUrl()).queryParam("token",
					props.getToken());
			URI uri = builder.buildAndExpand(uriParams).toUri();
			HttpHeaders headers = new HttpHeaders();
			headers.set("User-Agent", props.getUserAgent());
			RequestEntity<Object> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, uri);
			ResponseEntity<T> response = restTemplate.exchange(requestEntity, entityClass);
			HttpHeaders responseHeaders = response.getHeaders();
			int newRemaining = Integer.parseInt(responseHeaders.get("X-Discogs-Ratelimit-Remaining").get(0));
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
