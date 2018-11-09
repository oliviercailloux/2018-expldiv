package io.github.oliviercailloux.expldiv;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class TestApp {

	@Test
	void testEnvy() {
		final App app = new App(1, 2);
		final int[] u = new int[] { 1 };
		app.setUtilityFunctions(new int[][] { u, u });
		final int[] assignment = new int[] { 0 };
		app.setAssignment(assignment);
		assertArrayEquals(new int[] { 1 }, app.getShare(assignment, 0));
		assertArrayEquals(new int[] { 0 }, app.getShare(assignment, 1));
		assertTrue(app.envies(1, 0));
		assertTrue(app.envy());
	}

	@Test
	void testNoEnvyFree1Item() {
		final App app = new App(1, 2);
		final int[] u = new int[] { 1 };
		app.setUtilityFunctions(new int[][] { u, u });
		final int[] assignment = new int[] { 0 };
		app.setAssignment(assignment);
		assertTrue(app.envy());
		assertFalse("Found: " + Arrays.toString(app.getAssignment()) + ".", app.existsEnvyFreeAssignment());
	}

	@Test
	void testNoEnvyFree3Items() {
		final App app = new App(3, 2);
		final int[] u = new int[] { 1, 1, 1 };
		app.setUtilityFunctions(new int[][] { u, u });
		assertFalse("Found: " + Arrays.toString(app.getAssignment()) + ".", app.existsEnvyFreeAssignment());
	}

}
