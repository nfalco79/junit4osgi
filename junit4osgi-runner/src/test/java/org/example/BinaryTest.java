package org.example;

import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;
import org.junit.Test;

public class BinaryTest {

	@Test
	public void illegal_chars_on_error_message() throws Exception {
		InputStream image = null;
		try {
			image = getClass().getResourceAsStream("sample.png");
			String invalidMessage = IOUtil.toString(image);
			Assert.fail(invalidMessage);
		} finally {
			IOUtil.close(image);
		}
	}
}
