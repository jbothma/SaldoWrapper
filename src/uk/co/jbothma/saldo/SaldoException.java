package uk.co.jbothma.saldo;

/**
 * Assume after one of these that the Saldo instance is unusuable and should
 * be closed. This is because something unexpected happened and we might not
 * be able to reliably process more JSON responses.
 * 
 * If we're a bit more clever about handling unexpected JSON output, we can
 * relax this exception.
 */
public class SaldoException extends Exception {
	String error;

	public SaldoException() {
		this.error = "unknown";
	}

	public SaldoException(String error) {
		this.error = error;
	}

	public String getError() {
		return this.error;
	}

	public String toString() {
		return this.error + System.getProperty("line.separator")
				+ super.toString();
	}
}