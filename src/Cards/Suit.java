package Cards;

public enum Suit {
	S("S"), H("H"), D("D"), C("C");

	private String suitString;

	// Constructor
	private Suit(String s) {
		this.suitString = s;
	}

	// Get String
	public String toString() {
		return suitString;
	}
}
