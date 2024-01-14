package com.nononsenseapps.notepad.test;

import androidx.test.filters.SmallTest;

import com.nononsenseapps.notepad.database.Task;

import junit.framework.TestCase;

/**
 * Tests related to the Date and Time formats
 */
public class DateTimeTest extends TestCase {

	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@SmallTest
	public void test_setAsCompleted() {
		Task t = new Task();
		t.setAsCompletedForLegacy(); // <-- see its comments
		assertTrue(t.completed > 0);
	}

}
