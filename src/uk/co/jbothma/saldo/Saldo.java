package uk.co.jbothma.saldo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO: Maybe the API should be based on WordNet's???
 * TODO: more control over stderr could be nice
 */
public class Saldo {
	private Process saldoProcess;
	private Thread errorThread;
	private JsonParser jsonParser;
	private MappingJsonFactory jsonFactory;
	private BufferedWriter outputStream;
	private BufferedReader inputStream;
	private ObjectMapper mapper;

	public Saldo(String binPath, String dictPath) throws IOException,
			InterruptedException, SaldoException {
		ProcessBuilder pb = new ProcessBuilder(binPath, dictPath);
		saldoProcess = pb.start();

		errorThread = new Thread(new ErrorReader(saldoProcess.getErrorStream()));
		errorThread.start();

		inputStream = new BufferedReader(new InputStreamReader(
				saldoProcess.getInputStream()));
		outputStream = new BufferedWriter(new OutputStreamWriter(
				saldoProcess.getOutputStream()), 100);

		jsonFactory = new MappingJsonFactory();
		jsonParser = jsonFactory.createJsonParser(inputStream);
		mapper = new ObjectMapper(); // can reuse, share globally

		// test lookup
		Word testWord = this.getAnalysis("alltmer").get(0);
		if (!testWord.getSaldoId().equals("alltmer..ab.1"))
			throw new SaldoException(
					"Expected test lookup to find id alltmer..ab.1 but instead got "
							+ testWord.getSaldoId());
	}

	/**
	 * Print a few demos to stdout
	 * 
	 * @param args
	 * @throws SaldoException
	 */
	public static void main(String[] args) throws SaldoException {
		String binPath = "/home/jdb/uni/uppsala/2011-2012/thesis/sw_source/FM-SBLEX_svn/sblex/bin/saldo";
		String dictPath = "/home/jdb/uni/uppsala/2011-2012/thesis/sw_source/FM-SBLEX_svn/dicts/saldo.saker.dict";
		try {
			Saldo saldo = new Saldo(binPath, dictPath);
			System.out.println(saldo.getAnalysis("alltmer"));
			System.out.println(saldo.getCompoundAnalysis("alltmer"));
			// kommuner is cool because it doesn't actually occur in the
			// dictionary file.
			System.out.println(saldo.getAnalysis("kommuner"));
			System.out.println(saldo.getAnalysis("saker"));
			saldo.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the list of saldo results for the given word.
	 * 
	 * @param word
	 *            to look up
	 * @return list of result words. Empty list implies word not found.
	 * @throws JsonParseException
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws SaldoException
	 * 
	 * TODO: what does the s_ prefix mean? It looks like s_ is a
	 * whole-word match, while c_ is compound analysis results e.g.
	 * for "kommunalskatt" s_1: word:kommunalskatt
	 * id:kommunalskatt..nn.1 c_1: [word:kommunal, word:skatt] c_3:
	 * [word:kommun, word:al, word:skatt]
	 */
	public List<Word> getAnalysis(String word) throws JsonParseException,
			JsonProcessingException, IOException, SaldoException {
		JsonNode saldoResponse = plainLookup(word);
		List<Word> resultList = new ArrayList<Word>();
		if (saldoResponse != null) {
			Iterator<String> resultKeys = saldoResponse.fieldNames();
			String resultKey;
			JsonNode resultNode;
			while (resultKeys.hasNext()) {
				resultKey = resultKeys.next();
				if (resultKey.startsWith("s_")) {
					resultNode = saldoResponse.get(resultKey);
					Word saldoWord = mapper.readValue(resultNode.traverse(),
							Word.class);
					resultList.add(saldoWord);
				} else {
					continue;
				}
			}
		}
		return resultList;
	}

	public List<Word> getCompoundAnalysis(String word)
			throws JsonParseException, JsonProcessingException, IOException,
			SaldoException {
		// TODO: This is just duplicating getAnalysis except for c_ prefix.
		// Duplication is baaaaad.
		JsonNode saldoResponse = plainLookup(word);
		List<Word> resultList = new ArrayList<Word>();
		if (saldoResponse != null) {
			Iterator<String> resultKeys = saldoResponse.fieldNames();
			String resultKey;
			JsonNode resultNode;
			while (resultKeys.hasNext()) {
				resultKey = resultKeys.next();
				if (resultKey.startsWith("c_")) {
					resultNode = saldoResponse.get(resultKey);
					Word saldoWord = mapper.readValue(resultNode.traverse(),
							Word.class);
					resultList.add(saldoWord);
				} else {
					continue;
				}
			}
		}
		return resultList;
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
	private JsonNode plainLookup(String word) throws JsonParseException,
			JsonProcessingException, IOException, SaldoException {

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
					jsonParser.nextToken(); // end object
					// returning null is apparently bad, but we're dealing with
					// JsonNodes here, not collections.
					// The functions that return results to the outside should
					// return empty collections.
					return null;
				} else if (jsonParser.getText().equals("-Symb-")) {
					jsonParser.nextToken(); // end object
					return null;
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
}
