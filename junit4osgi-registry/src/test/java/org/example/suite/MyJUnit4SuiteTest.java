package org.example.suite;

import org.example.SimpleTestCase;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses(value = SimpleTestCase.class)
@RunWith(Suite.class)
public class MyJUnit4SuiteTest {

}
