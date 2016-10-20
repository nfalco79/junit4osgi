/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
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
package com.github.nfalco79.junit4osgi.runner.internal;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Print stream dispatching on a given one and storing written data in a output
 * stream.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ReportPrintStream extends PrintStream {

	private PrintStream stream;

	private boolean duplicate;

	public ReportPrintStream(OutputStream out, PrintStream def,
			boolean hideOutput) {
		super(out);
		stream = def;
		duplicate = !hideOutput;
	}

	public void println() {
		if (duplicate) {
			stream.println();
		}
		super.println();
	}

	public void println(boolean x) {
		if (duplicate) {
			stream.println(x);
		}
		super.println(x);
	}

	public void println(char x) {
		if (duplicate) {
			stream.println(x);
		}
		super.println(x);
	}

	public void println(char[] x) {
		if (duplicate) {
			stream.println(x);
		}
		super.println(x);
	}

	public void println(double x) {
		if (duplicate) {
			stream.println(x);
		}
		super.println(x);
	}

	public void println(float x) {
		if (duplicate) {
			stream.println(x);
		}
		super.println(x);
	}

	public void println(int x) {
		if (duplicate) {
			stream.println(x);
		}
		super.println(x);
	}

	public void println(long x) {
		if (duplicate) {
			stream.println(x);
		}
		super.println(x);
	}

	public void println(Object x) {
		if (duplicate) {
			stream.println(x);
		}
		super.println(x);
	}

	public void println(String s) {
		if (duplicate) {
			stream.println(s);
		}
		super.println(s);
	}

}
