package charactermanaj.model;

import java.io.Serializable;

/**
 * パーツの作者情報
 * @author seraphy
 */
public class PartsAuthorInfo implements Serializable {

	private static final long serialVersionUID = -5308326806956816225L;

	private String author;
	
	private String homePage;

	public String getAuthor() {
		return author;
	}
	
	public String getHomePage() {
		return homePage;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
	
	public void setHomePage(String homePage) {
		this.homePage = homePage;
	}
	
}
