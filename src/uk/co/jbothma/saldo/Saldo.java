package uk.co.jbothma.saldo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Saldo {
	private Process saldoProcess;
	private Thread errorThread;
	private JsonParser jsonParser;
	private MappingJsonFactory jsonFactory;
	private BufferedWriter outputStream;
	private BufferedReader inputStream;
	
	public Saldo(String binPath, String dictPath) throws IOException,
			InterruptedException, SaldoException {
		ProcessBuilder pb = new ProcessBuilder(binPath, dictPath);
		saldoProcess = pb.start();

		errorThread = new Thread(new ErrorReader(saldoProcess.getErrorStream()));
		errorThread.start();

		inputStream = new BufferedReader(new InputStreamReader(
				saldoProcess.getInputStream()));
		outputStream = new BufferedWriter(
				new OutputStreamWriter(saldoProcess.getOutputStream()), 100);

		jsonFactory = new MappingJsonFactory();
		jsonParser = jsonFactory.createJsonParser(inputStream);

		// test lookup
		this.getAnalysis("alltmer");
		this.getCompoundAnalysis("alltmer");
	}

	/**
	 * Print a few demos to stdout
	 * 
	 * @param args
	 * @throws SaldoException 
	 */
	public static void main(String[] args) throws SaldoException {
		String binPath = "/home/jdb/uni/uppsala/2011-2012/thesis/sw_source/FM-SBLEX_svn/sblex/bin/saldo";
		String dictPath = "/home/jdb/uni/uppsala/2011-2012/thesis/sw_source/FM-SBLEX_svn/dicts/saldo.dict";
		try {
			(new Saldo(binPath, dictPath)).close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void getAnalysis(String word) throws JsonParseException,
			JsonProcessingException, IOException, SaldoException {
		JsonNode saldoResult = plainLookup(word);
		if (saldoResult != null) {
			Iterator<String> resultKeys = saldoResult.fieldNames();
			String resultKey;
			while (resultKeys.hasNext()) {
				// TODO: what does the s_ prefix mean?
				// It looks like s_ is a whole-word match, while c_ is compound
				// analysis results
				// e.g. for "kommunalskatt"
				// s_1: word:kommunalskatt id:kommunalskatt..nn.1
				// c_1: [word:kommunal, word:skatt]
				// c_3: [word:kommun, word:al, word:skatt]
				resultKey = resultKeys.next();
				if (resultKey.startsWith("s_")) {
					System.out.println(resultKey + "  "
							+ saldoResult.get(resultKey).toString());
				} else {
					continue;
				}
			}
		} else { // word not found
		}
	}

	public void getCompoundAnalysis(String word) throws JsonParseException,
			JsonProcessingException, IOException, SaldoException {
		JsonNode saldoResult = plainLookup(word);
		Iterator<String> resultKeys = saldoResult.fieldNames();
		String resultKey;
		while (resultKeys.hasNext()) {
			resultKey = resultKeys.next();
			if (resultKey.startsWith("c_")) {
				System.out.println(resultKey + "  "
						+ saldoResult.get(resultKey).toString());
			} else {
				continue;
			}
		}
	}

	/**
	 * Does a plain lookup as if at the saldo console. Don't know what this is
	 * called in saldo terminology, so I call it a plain lookup.
	 * 
	 * @param word
	 * @return The JsonNode representing the result object ie the value of
	 *         "word : {...}"
	 * @throws SaldoException
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws JsonParseException
	 */
	private JsonNode plainLookup(String word)
			throws JsonParseException, JsonProcessingException, IOException, SaldoException {

		outputStream.write(word + System.getProperty("line.separator"));
		outputStream.flush();
		if (jsonParser.nextToken() == JsonToken.START_OBJECT) {
			jsonParser.nextToken();
			if (jsonParser.getCurrentName().equals(word)) {
				if (jsonParser.nextToken() == JsonToken.START_OBJECT) {
					JsonNode result = jsonParser.readValueAsTree();
					jsonParser.nextToken(); // end object
					return result;
				} else if (jsonParser.getText().equals("-Unknown-")) {
					System.out.println("word " + word + " unknown");
					return null; //returning null is apparently bad, but it's a start.
				} else {
					throw new SaldoException("result value should be object: "
							+ jsonParser.getCurrentToken());
				}
			} else {
				throw new SaldoException("first field name should equal word: "
							+ jsonParser.getCurrentToken());
			}
		} else {
			throw new SaldoException("root should be object.");
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

	class SaldoException extends Exception {
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
			return this.error + System.getProperty("line.separator") + super.toString(); 
		}
	}
}
