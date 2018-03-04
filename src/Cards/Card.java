package Cards;

public class Card implements Comparable<Card> {

	private Rank rank;
	private Suit suit;

	public Card(Rank rank,Suit suit) {
		this.rank = rank;
		this.suit = suit;
	}

	public Suit getSuit() {
		return suit;
	}

	public Rank getRank() {
		return rank;
	}

	// To Sort the cards
	public int compareTo(Card card) {

		if (this.suit.compareTo(card.suit) > 0) {
			return 1;
		} else if (this.suit.compareTo(card.suit) < 0) {
			return -1;
		} else {

			if (this.rank.compareTo(rank) > 0) {
				return 1;
			} else if (this.rank.compareTo(card.rank) < 0) {
				return -1;
			} else {
				return 0;
			}
		}
	}
	
	// To check if a card is same as another
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Card other = (Card) obj;
		return rank == other.rank && suit == other.suit;
	}
}