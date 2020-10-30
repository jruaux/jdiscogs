package org.ruaux.jdiscogs;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ruaux.jdiscogs.model.Master;
import org.ruaux.jdiscogs.model.Release;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiscogsClient {

	private static final String URL_TEMPLATE = "https://api.discogs.com/{entity}/{id}";

	private final JDiscogsProperties props;
	private final RestTemplate restTemplate;
	private long rateLimitLastTime;
	private int rateLimitRemaining;

	public DiscogsClient(JDiscogsProperties props, RestTemplate restTemplate) {
		this.props = props;
		this.restTemplate = restTemplate;
	}

	public DiscogsClient(JDiscogsProperties props) {
		this(props, new RestTemplateBuilder().build());
	}

	public Master getMaster(long id) {
		return getEntity("masters", Master.class, id);
	}

	public Release getRelease(long id) {
		return getEntity("releases", Release.class, id);
	}

	/**
	 * synchronized to avoid running into this bug:
	 * https://bugs.openjdk.java.net/browse/JDK-8213202
	 */
	private synchronized <T> T getEntity(String entity, Class<T> entityClass, long id) {
		boolean after1Min = (System.currentTimeMillis() - rateLimitLastTime) > 60000;
		if (rateLimitRemaining > 1 || after1Min) {
			Map<String, String> uriParams = new HashMap<>();
			uriParams.put("entity", entity);
			uriParams.put("id", String.valueOf(id));
			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(URL_TEMPLATE).queryParam("token",
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
