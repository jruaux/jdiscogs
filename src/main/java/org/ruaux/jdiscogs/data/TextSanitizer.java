package org.ruaux.jdiscogs.data;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
public class TextSanitizer {

	public String sanitize(String string) {
		return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}

}
