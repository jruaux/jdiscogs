package org.ruaux.jdiscogs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ruaux.jdiscogs.data.Helper;

public class SanitizerTest {

	@Test
	public void testSanitize() {
		Assertions.assertEquals("La guepe volume 3", Helper.sanitize("La guêpe volume 3"));
	}

}
