/*
 * Copyright 2017 Nikolas Falco
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.junit4osgi.runner.internal.xml.util;

import java.io.PrintWriter;
import java.io.Writer;

import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Patched version of the original
 * {@link org.codehaus.plexus.util.xml.Xpp3DomWriter} that does not handle
 * correctly the CDATA XML value.
 *
 * @author nikolasfalco
 *
 */
public class Xpp3DomWriter {

	public static void write(Writer writer, Xpp3Dom dom) {
		write(new PrettyPrintXMLWriter(writer), dom);
	}

	public static void write(PrintWriter writer, Xpp3Dom dom) {
		write(new PrettyPrintXMLWriter(writer), dom);
	}

	public static void write(XMLWriter xmlWriter, Xpp3Dom dom) {
		write(xmlWriter, dom, true);
	}

	public static void write(XMLWriter xmlWriter, Xpp3Dom dom, boolean escape) {
		xmlWriter.startElement(dom.getName());
		String[] attributeNames = dom.getAttributeNames();
		for (String attributeName : attributeNames) {
			xmlWriter.addAttribute(attributeName, dom.getAttribute(attributeName));
		}
		Xpp3Dom[] children = dom.getChildren();
		for (Xpp3Dom aChildren : children) {
			write(xmlWriter, aChildren, escape);
		}

		String value = dom.getValue();
		if (value != null) {
			if (escape && !value.startsWith("<![CDATA[") && !value.endsWith("]]>")) {
				xmlWriter.writeText(value);
			} else {
				xmlWriter.writeMarkup(value);
			}
		}

		xmlWriter.endElement();
	}

}
