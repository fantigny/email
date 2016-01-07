package test.geos.matrix;

@SuppressWarnings("serial")
public class MatrixParamBuilderException extends Exception {
	private static final String CR_LF = System.getProperty("line.separator");
	private static final String GENERAL_MSG = "%s" + CR_LF + CR_LF
			+ "three parameters expected:" + CR_LF
			+ " 1/ Width of GeoBlock (in number of Geos)" + CR_LF
			+ " 2/ Height of GeoBlock (in number of Geos)" + CR_LF
			+ " 3/ Name of the CSV file that defines the occupied Geos for that GeoBlock" + CR_LF + CR_LF
			+ "for example: java test.geos.cluster.entrypoint.GeoClusterAnalyser 4 7 GeoBlockExample.csv" + CR_LF;

	public MatrixParamBuilderException(String msg) {
		super(String.format(GENERAL_MSG, msg));
	}

	public MatrixParamBuilderException(String msg, Throwable t) {
		super(String.format(GENERAL_MSG, msg), t);
	}
}
