package uk.co.jbothma.saldo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * 
 * @author jdb
 * 
 */
@JsonIgnoreProperties({ "head", "param", "inhs", "p", "attr" })
public class Word {
	private String word;
//	private String head;
	private String pos;
//	private String param;
//	private String[] inhs;
	private String saldoId;
	
	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

//	public String getHead() {
//		return head;
//	}
//
//	public void setHead(String head) {
//		this.head = head;
//	}

	public String getPos() {
		return pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

//	public String getParam() {
//		return param;
//	}

//	public void setParam(String param) {
//		this.param = param;
//	}
//
//	public String[] getInhs() {
//		return inhs;
//	}
//
//	public void setInhs(String[] inhs) {
//		this.inhs = inhs;
//	}

	@JsonProperty("id")
	public String getSaldoId() {
		return saldoId;
	}

	@JsonProperty("id")
	public void setSaldoId(String saldoId) {
		this.saldoId = saldoId;
	}
	
	public String toString() {
		return this.getClass().getName() +
				" word:" + this.getWord() + 
				" saldoId:" + this.getSaldoId();
	}
}
