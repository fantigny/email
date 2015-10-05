package test.bg;

/**
 * @author fantigny
 *
 */
public class Algo {


	/**
	 * find the starting position of an array in another one
	 * i.e. [2,3,4,5] and [4,5] returns 2
	 *
	 * @param source first array
	 * @param search second array
	 * @return starting position of the second array in the first array, (-1) if not found
	 */
	public int getStartPos(final int[] source, final int[] search) {
		if (source == null || source.length == 0) {
			return -1;
		}
		if (search == null || search.length == 0) {
			return -1;
		}

		for(int pos=0, n=source.length; pos<n; pos++) {
			if (source[pos] == search[0]) {
				return pos;
			}
		}

		return -1;
	}
}
