package org.ruaux.jdiscogs.data;

import java.text.Normalizer;

public class TextSanitizer {

	public String sanitize(String string) {
		return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}

}
