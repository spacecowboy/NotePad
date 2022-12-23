package com.nononsenseapps.notepad.test;

import static org.junit.Assert.assertNotEquals;

import androidx.test.filters.MediumTest;

import com.nononsenseapps.notepad.database.Task;

import junit.framework.TestCase;

public class DaoTaskTest extends TestCase {

	private void copyValsFromTo(final Task task1, final Task task2) {
		task2.title = task1.title;
		task2.note = task1.note;
		task2.completed = task1.completed;
		task2.due = task1.due;
	}

	@MediumTest
	public void testTask() {
		final Task task1 = new Task();
		task1.title = "title1";
		task1.note = "note1";

		// Equals method should be true for these
		assertEquals("Task should equal itself!", task1, task1);

		task1.due = 924592L;
		assertEquals("Task should equal itself!", task1, task1);

		task1.completed = 230456L;
		assertEquals("Task should equal itself!", task1, task1);

		// Create copy
		final Task task2 = new Task();
		copyValsFromTo(task1, task2);

		assertTrue((task1.title != null && task1.title.equals(task2.title)));
		assertTrue((task1.note != null && task1.note.equals(task2.note)));
		assertEquals(task1.due, task2.due);
		assertTrue(((task1.completed != null) == (task2.completed != null)));

		assertEquals("Task1 should equal task2!", task1, task2);

		// Completed should only care about null status
		task2.completed = 9272958113551L;
		assertEquals("Completed should only care about null values", task1, task2);

		// Should all fail
		copyValsFromTo(task1, task2);
		task2.title = "badfa";
		assertNotEquals("Task1 should not equal task2!", task1, task2);

		copyValsFromTo(task1, task2);
		task2.note = "badfa";
		assertNotEquals("Task1 should not equal task2!", task1, task2);

		copyValsFromTo(task1, task2);
		task2.due = 29037572395L;
		assertNotEquals("Task1 should not equal task2!", task1, task2);

		copyValsFromTo(task1, task2);
		task2.completed = null;
		assertNotEquals("Task1 should not equal task2!", task1, task2);

		copyValsFromTo(task1, task2);
		task2.due = null;
		assertNotEquals("Task1 should not equal task2!", task1, task2);

		copyValsFromTo(task1, task2);
		task2.title = "badfa";
		task2.note = "asdfal";
		assertNotEquals("Task1 should not equal task2!", task1, task2);

		copyValsFromTo(task1, task2);
		task2.title = "badfa";
		task2.due = null;
		assertNotEquals("Task1 should not equal task2!", task1, task2);

		copyValsFromTo(task1, task2);
		task2.note = "badfa";
		task2.due = 292374522222L;
		assertNotEquals("Task1 should not equal task2!", task1, task2);

		copyValsFromTo(task1, task2);
		task2.title = "badfa";
		task2.note = "asdf";
		task2.due = null;
		assertNotEquals("Task1 should not equal task2!", task1, task2);

		copyValsFromTo(task1, task2);
		task2.title = "badfa";
		task2.note = "asdf";
		task2.due = null;
		task2.completed = null;
		assertNotEquals("Task1 should not equal task2!", task1, task2);
	}
}
