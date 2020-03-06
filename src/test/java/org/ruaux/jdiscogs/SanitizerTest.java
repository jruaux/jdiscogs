package org.ruaux.jdiscogs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ruaux.jdiscogs.data.TextSanitizer;

public class SanitizerTest {

	@Test
	public void testSanitize() {
		TextSanitizer sanitizer = new TextSanitizer();
		Assertions.assertEquals("La guepe volume 3", sanitizer.sanitize("La guêpe volume 3"));
	}

}
