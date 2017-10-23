package com.github.nfalco79.junit4osgi.registry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class TestURLStreamHandlerFactory implements URLStreamHandlerFactory {

	private class TestURLStreamHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			return new TestURLConnection(url);
		}
	}

	private class TestURLConnection extends URLConnection {

		public TestURLConnection(URL url) {
			super(url);
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			String query = super.getURL().getQuery();
			String realURL = query.substring(query.indexOf('=') + 1);
			return new URL(realURL).openStream();
		}
	}

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if ("testentry".equals(protocol)) {
            return new TestURLStreamHandler();
        }

        return null;
	}

}
