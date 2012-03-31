package uk.co.jbothma.saldo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class Saldo {
	private Process saldoProcess;

	public Saldo(String binPath, String dictPath) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(binPath, dictPath);
		
		pb.redirectErrorStream(true);
		saldoProcess = pb.start();

		new Thread(new Reader(saldoProcess.getInputStream())).start();
		new Thread(new Writer(saldoProcess.getOutputStream())).start();
		
		saldoProcess.waitFor();
	}

	/**
	 * Print a few demos to stdout
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String binPath = "/home/jdb/uni/uppsala/2011-2012/thesis/sw_source/FM-SBLEX_svn/sblex/bin/saldo";
		String dictPath = "/home/jdb/uni/uppsala/2011-2012/thesis/sw_source/FM-SBLEX_svn/dicts/saldo.dict";
		try {
			(new Saldo(binPath, dictPath)).close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void close() {
		saldoProcess.destroy();
	}
	
	class Reader implements Runnable {
		BufferedReader inputStream;
		
		Reader(InputStream inputStream) {
			this.inputStream = new BufferedReader(new InputStreamReader(inputStream));
		}
		
		public void run() {
			String line;
			try {
				while ((line = inputStream.readLine()) != null) {
					System.out.println("reader thread " + line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	class Writer implements Runnable {
		BufferedWriter outputStream;
		Writer(OutputStream outputStream) {
			this.outputStream = new BufferedWriter( new OutputStreamWriter( outputStream ), 50 /* keep small for tests */ );
		}
		@Override
		public void run() {
			try {
				outputStream.write("ord" + System.getProperty("line.separator"));
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
