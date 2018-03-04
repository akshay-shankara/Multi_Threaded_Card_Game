package Cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

	private List<Card> deck = new ArrayList<>();

	public Deck() {
		for (Suit suit : Suit.values()) {
			for (Rank rank : Rank.values()) {
				deck.add(new Card(rank, suit));
			}
		}
	}

	public Card draw() {
		return deck.remove(deck.size() - 1);
	}

	public int size() {
		return deck.size();
	}

	// Sort the deck
	public void sort() {
		Collections.sort(deck);
	}

	// Less number of permutations
	public void shuffle() {
		Collections.shuffle(deck);
	}
}
