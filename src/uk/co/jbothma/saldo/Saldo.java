package uk.co.jbothma.saldo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Saldo {
	private Process saldoProcess;
	private Thread errorThread;

	public Saldo(String binPath, String dictPath) throws IOException,
			InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(binPath, dictPath);
		saldoProcess = pb.start();

		errorThread = new Thread(new ErrorReader(saldoProcess.getErrorStream()));
		errorThread.start();

		BufferedReader inputStream = new BufferedReader(new InputStreamReader(
				saldoProcess.getInputStream()));
		BufferedWriter outputStream = new BufferedWriter(
				new OutputStreamWriter(saldoProcess.getOutputStream()), 100);

		// test lookup
		outputStream.write("ord\n\n");
		outputStream.flush();
		System.out.println(inputStream.readLine());
	}

	/**
	 * Print a few demos to stdout
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String binPath = "/home/jdb/uni/uppsala/2011-2012/thesis/sw_source/FM-SBLEX_svn/sblex/bin/saldo";
		String dictPath = "/home/jdb/uni/uppsala/2011-2012/thesis/sw_source/FM-SBLEX_svn/dicts/saldo100.dict";
		try {
			(new Saldo(binPath, dictPath)).close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		saldoProcess.destroy();
	}

	class ErrorReader implements Runnable {
		BufferedReader inputStream;

		ErrorReader(InputStream inputStream) {
			this.inputStream = new BufferedReader(new InputStreamReader(
					inputStream));
		}

		public void run() {
			String line;
			try {
				while ((line = inputStream.readLine()) != null) {
					System.err.println("reader thread " + line);
				}
			} catch (IOException e) {
				// closing the file handle when the process exits throws an
				// exception here. don't care enough about stderr to handle this
				// better yet.
			}
		}
	}
}
