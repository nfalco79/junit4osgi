package org.example;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AfterBeforeTest {

	@Before
	public void init() {
		System.out.println("log to system out");
		System.err.println("log to system err");
		throw new RuntimeException("init method exception");
	}

	@After
	public void dispose() {
		System.out.println("log to system out");
		System.err.println("log to system err");
		throw new RuntimeException("dispose method exception");
	}

	@Test
	public void test() {
	}

}