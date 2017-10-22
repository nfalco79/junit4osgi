package com.github.nfalco79.junit4osgi.registry.internal.asm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public final class ASMUtils {

	private ASMUtils() {
		// default constructor
	}

	public static void analyseByteCode(URL entry, ClassVisitor visitor) {
		InputStream is = null;
		try {
			is = entry.openStream();
			if (is != null) {
				ClassReader reader = new ClassReader(is);
				reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			}
		} catch (IOException e) {
			// skip class
		} finally {
			closeSilently(is);
		}
	}

	private static void closeSilently(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}

}