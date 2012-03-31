package uk.co.jbothma.saldo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		outputStream.write("alltmer\n");
		outputStream.flush();

		JsonFactory f = new MappingJsonFactory();
		JsonParser jp = f.createJsonParser(inputStream);
		//ObjectMapper mapper = new ObjectMapper();
		JsonToken current;

		current = jp.nextToken();
		if (current != JsonToken.START_OBJECT) {
			System.out.println("Error: root should be object: quiting.");
		}
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = jp.getCurrentName();
			System.out.println(fieldName);

			current = jp.nextToken();
			if (current == JsonToken.START_OBJECT) {
				// read the record into a tree model,
				// this moves the parsing position to the end of it
				JsonNode node = jp.readValueAsTree();
				Iterator<String> resultKeys = node.fieldNames();
				String resultKey;
				while (resultKeys.hasNext()) {
					// TODO: what does the s_ prefix mean?
					resultKey = resultKeys.next();
					System.out.println(resultKey);
				}
				
				System.out.println(node.toString());
			} else {
				System.err.println("Error: records should be an array: skipping.");
				jp.skipChildren();
			}
		}
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
