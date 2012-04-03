package uk.co.jbothma.saldo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * 
 * @author jdb
 * 
 */
@JsonIgnoreProperties({ "head", "p", "attr" })
public class Word {
	private String word;
//	private String head;
	private String pos;
	private String param;
	private String[] inhs;
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

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

	public String[] getInhs() {
		return inhs;
	}

	public void setInhs(String[] inhs) {
		this.inhs = inhs;
	}

	@JsonProperty("id")
	public String getSaldoId() {
		return saldoId;
	}

	@JsonProperty("id")
	public void setSaldoId(String saldoId) {
		this.saldoId = saldoId;
	}
	
	/**
	 * Returns the morphological analysis of the word 
	 * in a form similar to the SUC tagset.
	 * 
	 * nn u sg indef nom <- pos inhs[0] param
	 */
	public String getMorph() {
		String morph = "";
		morph += this.getPos() + " ";
		if (this.getInhs() != null && this.getInhs().length > 0) {
			morph += this.getInhs()[0] + " ";
		} else {
			morph += " ";
		}
		morph += this.getParam();
		return morph;
	}
	
	/**
	 * Returns lemma part of saldoId
	 * e.g. kunna for kunna..vb.1
	 */
	public String getLemma() {
		return this.getSaldoId().substring(0, this.getSaldoId().indexOf('.'));
	}
	
	public String toString() {
		return this.getClass().getSimpleName() +
				" word:" + this.getWord() + 
				" saldoId:" + this.getSaldoId() + 
				" morph:" + this.getMorph() ;
	}
}
