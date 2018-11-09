package io.github.oliviercailloux.expldiv;

import static com.google.common.base.Preconditions.checkArgument;

import org.cprover.CProver;

public class App {
	/**
	 * Number of objects
	 */
	private final int m;
	/**
	 * Number of users
	 */
	private final int n;
	/**
	 * [i]: the utility function of user i
	 */
	private int[][] utilityFunctions;
	/**
	 * [k]: to which user the k^th object is assigned
	 */
	private int[] assignment;

	public App() {
		this(3, 3);
	}

	public App(int m, int n) {
		this.m = m;
		this.n = n;
		utilityFunctions = null;
		assignment = null;
	}

	void setUtilityFunctions(int[][] ufs) {
		checkArgument(ufs.length == n);
		checkArgument(n == 0 || ufs[0].length == m);
		utilityFunctions = ufs;
	}

	void setAssignment(int[] anAssignment) {
		assignment = anAssignment;
	}

	public static void main(String[] args) throws Exception {
		final App app = new App();
		app.proceed();
	}

	public void falsifyEnvy() {
		generateUtilities();
		generateAssignment();
		assert envies(0, 1);
	}

	public void proceed() {
		// 3 users, 8 objects: dunno if mms satisfiable.
		generateUtilities();
		assert existsEnvyFreeAssignment();

		// find ufs such that for all assignment: no envy, thus: ut_i of share j >
		// ut_i of share i.

	}

	public boolean existsEnvyFreeAssignment() {
		generateStartAssignment();
		do {
			if (!envy()) {
				return true;
			}
		} while (incrementAssignment());
		return false;
	}

	public boolean envy() {
		for (int i = 0; i < n; ++i) {
			for (int j = 0; j < n; ++j) {
				if (envies(i, j)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param i in [0, N)
	 * @param j in [0, N)
	 * @return <code>true</code> iff user i has a strictly smaller utility for her
	 *         share than for j's share
	 */
	public boolean envies(int i, int j) {
		assert i < n;
		assert j < n;
		final int[] shareI = getShare(assignment, i);
		final int[] ufI = utilityFunctions[i];
		/** User i envies user j iff ut_i(share j) > ut_i(share i). */
		final int utilityI = getUtility(ufI, shareI);
		final int utilityIShareJ = getUtility(ufI, getShare(assignment, j));
		return utilityI < utilityIShareJ;
	}

	public void generateUtilities() {
		utilityFunctions = new int[n][m];
		for (int i = 0; i < n; ++i) {
			for (int k = 0; k < m; ++k) {
				final int utility = CProver.nondetInt();
				CProver.assume(utility >= 0);
				CProver.assume(utility <= 5);
				utilityFunctions[i][k] = utility;
			}
		}
	}

	public int[] newAssignment() {
		final int[] anAssignment = new int[m];
		for (int k = 0; k < m; ++k) {
			final int owner = CProver.nondetInt();
			CProver.assume(owner >= 0);
			CProver.assume(owner < n);
			anAssignment[k] = owner;
		}
		return anAssignment;
	}

	public void generateStartAssignment() {
		final int[] anAssignment = new int[m];
		for (int k = 0; k < m; ++k) {
			anAssignment[k] = 0;
		}
		assignment = anAssignment;
	}

	public boolean incrementAssignment() {
		int k = m - 1;
		while (k >= 0 && assignment[k] == n - 1) {
			assignment[k] = 0;
			--k;
		}
		if (k == -1) {
			return false;
		}
		assignment[k] += 1;
		return true;
	}

	/**
	 * @param assignment an array of length n
	 * @param user       in [0, N]
	 * @return an array of length n with 1 if the k^th object is assigned to the
	 *         given user
	 */
	public int[] getShare(int[] anAssignment, int user) {
		assert anAssignment.length == m;
		assert user < n;
		final int[] share = new int[m];
		for (int k = 0; k < m; ++k) {
			if (anAssignment[k] == user) {
				share[k] = 1;
			} else {
				share[k] = 0;
			}
		}
		return share;
	}

	public void computeMms(int user) {
		final int[] tentativeAssignment = newAssignment();
		// TODO
		int tentativeUtility = getUtility(user, tentativeAssignment);
		getUtility(user, assignment);
	}

	public int getUtility(int user, int[] tentativeAssignment) {
		assert user < n;
		assert tentativeAssignment.length == m;
		int sum = 0;
		for (int k = 0; k < m; ++k) {
			if (tentativeAssignment[k] == user) {
				sum += utilityFunctions[user][k];
			}
		}
		return sum;
	}

	public int getUtility(int[] utilityFunction, int[] share) {
		assert utilityFunction.length == m;
		assert share.length == m;
		int prod = 0;
		for (int k = 0; k < m; ++k) {
			prod += utilityFunction[k] * share[k];
		}
		return prod;
	}

	public int worstShareUtility(int user, int[] tentativeAssignment) {
		throw new UnsupportedOperationException();
	}

	public void generateAssignment() {
		final int[] anAssignment = newAssignment();
		assignment = anAssignment;
	}

	public void check() {
		assert (!envies(0, 1));
	}

	public int[][] getUtilityFunctions() {
		return utilityFunctions;
	}

	public int[] getAssignment() {
		return assignment;
	}
}
