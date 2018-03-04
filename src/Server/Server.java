package Server;

import java.io.*;


import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import Cards.*;
import Utils.MyConstants;

public class Server {

	static Hashtable<String, ConnectionThread> clients = new Hashtable<String, ConnectionThread>();
	static int clientNumber = 0, numOfRounds = 0, bidsEntered = 0, handGiven = 0, cardPlayed = 0, countInitPlayers = 0,
			countPlayers = 0, scoreA = 0, scoreB = 0, allIn = 0, round = 0, allInElse = 0;
	static ArrayList<Integer> playOrder = new ArrayList<Integer>() {
		private static final long serialVersionUID = 1L;
		{
			add(0);
			add(1);
			add(2);
			add(3);
		}
	};

	static String orderString = "", winningCard = "", trickWinner = "";
	private static final Object lock = new Object();
	private static final Object lock2 = new Object();
	private static final Object lock3 = new Object();
	private static final Object lock4 = new Object();

	CountDownLatch clientLatch = new CountDownLatch(1);
	CountDownLatch bidsLatch = new CountDownLatch(1);
	CountDownLatch handLatch = new CountDownLatch(1);
	CountDownLatch playedLatch = new CountDownLatch(1);
	CountDownLatch trickWinnerLatch = new CountDownLatch(1);
	CountDownLatch gameLatch = new CountDownLatch(1);
	CountDownLatch roundLatch = new CountDownLatch(1);
	CountDownLatch elseLatch = new CountDownLatch(1);

	static List<String> playerNames = new ArrayList<String>(); // List of player names
	static List<String> teamNames = new ArrayList<String>(); // List of team names

	// Hash table (user, bid)
	static Hashtable<String, Integer> playerBids = new Hashtable<String, Integer>();

	// Hash table (team, bids), where team = ("Team1, Team2")
	static Hashtable<String, Integer> teamBids = new Hashtable<String, Integer>();

	// Hash table (team, wins)
	static Hashtable<String, Integer> teamWins = new Hashtable<String, Integer>();

	// Hash table (playername,cardPlayed)
	static Hashtable<String, String> playerCard = new Hashtable<String, String>();

	static Deck deck = new Deck();

	static boolean gameOver = false, roundOver = false, trickOver = false, trickWon = false, roundDone = false,
			trickAdd = false, changeOrder = false;
	static int tricksPlayed = 0;

	public Server() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(MyConstants.PORT);
		} catch (IOException e) {
			System.err.println("Unavailable Port - " + MyConstants.PORT);
			System.exit(-1);
		}
		System.out.println("Starting Server on Port - " + MyConstants.PORT);

		while (true) {
			try {

				// Connect to only 4 clients
				if (clientNumber <= 4) {
					Socket clientSocket = serverSocket.accept();
					deck.shuffle();
					clientNumber += 1;
					// Start one thread per client
					ConnectionThread serverThread = new ConnectionThread(clientSocket);
					serverThread.start();
				} else {
					// Refuse connection if client > 4
					serverSocket.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new Server();
	}

	class ConnectionThread extends Thread {
		private Socket socket = null;

		public ConnectionThread(Socket socket) {
			super("ConnectionThread");
			this.socket = socket;
		}

		public void run() {
			try {

				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();
				// To get input from client
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputStream));

				// To give output to the client. AutoFlush = On
				PrintWriter outputWriter = new PrintWriter(outputStream, true);

				// Get Player Name
				String player = inputReader.readLine();
				while (true) {
					if (!clients.containsKey(player)) { // New User
						System.out.println(player + " is connected!");
						clients.put(player, this);
						outputWriter.println(MyConstants.GOOD);
						playerNames.add(player);
						break;
					} else {
						while (clients.containsKey(player)) { // duplicate username
							outputWriter.println(MyConstants.BAD);
							player = inputReader.readLine();
						}
					}
				}

				// Wait for all clients to connect
				while (clientNumber != 4)
					try {
						clientLatch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				clientLatch.countDown();

				// Display the names of connected clients and the teams
				printNames(outputWriter);
				// Make Teams. (Ex: teamNames[0] = "Team1, Team2")
				teamNames.add(playerNames.get(0) + "," + playerNames.get(2));
				teamNames.add(playerNames.get(1) + "," + playerNames.get(3));

				// Initialize playing order as (0,1,2,3).
				synchronized (this) {
					orderString = orderString + playerNames.get(playOrder.get(countInitPlayers)) + " ";
					countInitPlayers++;
				}

				// GAME BEGINS
				while (!gameOver) {
					if (scoreA >= 250 || scoreB >= 250) {
						// Game is over
						// Display winning team
						allIn += 1;
						while (allIn != 4)
							try {
								gameLatch.await();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						gameLatch.countDown();

						outputWriter.println(MyConstants.WINNER);
						if (scoreA >= 250) {
							outputWriter.println("THE WINNING TEAM IS: " + teamNames.get(0));
						}
						if (scoreB >= 250) {
							outputWriter.println("THE WINNING TEAM IS: " + teamNames.get(1));
						}
						gameOver = true;
					} else {
						roundOver = false;
						outputWriter.println(MyConstants.GAME_NOT_OVER);
						while (!roundOver) {

							// ROUND BEGINS

							// Distribute cards to clients one after another
							Card[] cards = new Card[13];
							synchronized (lock4) {
								for (int i = 0; i < 13; i++) {
									cards[i] = deck.draw();
								}
							}
							// Wait for all clients to receive hand
							handGiven += 1;
							while (handGiven != 4)
								try {
									handLatch.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							handLatch.countDown();

							// Give hand to the players
							String hand = sortHand(cards);
							outputWriter.println(MyConstants.HAND);
							outputWriter.println(hand);

							// Get bids from the players
							String bidString = inputReader.readLine();
							int bidInteger = Integer.parseInt(bidString);
							playerBids.put(player, bidInteger);
							bidsEntered += 1;
							round = 0;
							allInElse = 0;

							// Wait for all players to enter bids
							while (bidsEntered != 4)
								try {
									bidsLatch.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							bidsLatch.countDown();
							TimeUnit.SECONDS.sleep(5);
							synchronized (this) {
								// Assign Team bids. Ex: teamBids[0] = {"Team1, Team2",20}
								String s1 = teamNames.get(0);
								String s2 = playerNames.get(0);
								int s3 = playerBids.get(s2);
								String s4 = playerNames.get(2);
								int s5 = playerBids.get(s4);
								teamBids.put(s1,s3 + s5);
								
								String s6 = teamNames.get(1);
								String s7 = playerNames.get(1);
								int s8 = playerBids.get(s7);
								String s9 = playerNames.get(3);
								int s10 = playerBids.get(s9);
								
								teamBids.put(s6,
										s8 + s10);
							}
							teamWins.put(teamNames.get(0), 0);
							teamWins.put(teamNames.get(1), 0);

							// Display Bids
							displayBids(outputWriter);

							// TRICK BEGINS
							while (tricksPlayed < 13) {
								TimeUnit.SECONDS.sleep(1);
								synchronized (lock3) {
									if (!trickAdd)
										tricksPlayed += 1;
									trickAdd = true;
								}

								// Enable entering cards in order
								outputWriter.println(MyConstants.ORDER);
								outputWriter.println("Playing order for this trick is: " + orderString);
								String cardString = inputReader.readLine();
								playerCard.put(player, cardString);
								cardPlayed++;

								roundDone = false;
								changeOrder = false;

								// Wait for all Players to choose cards
								while (cardPlayed != 4)
									try {
										playedLatch.await();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								playedLatch.countDown();
								trickWon = false;

								// Convert chosen cards in to a String array
								String[] playedCardsString = new String[4];
								for (int j = 0; j < 4; j++)
									playedCardsString[j] = playerCard.get(playerNames.get(j));
								Card highCard = getHighestCard(playedCardsString);
								winningCard = highCard.getRank().toString() + highCard.getSuit().toString();
								for (Entry<String, String> entry : playerCard.entrySet()) {
									if (entry.getValue().equals(winningCard)) {
										trickWinner = entry.getKey();
									}
								}
								trickAdd = false;

								// Display the winner of this trick
								displayTrickWinner(outputWriter);

								// Change playOrder to start from Winner
								// Get index of the Trick Winner
								int index = playOrder.indexOf(playerNames.indexOf(trickWinner));

								synchronized (lock) {
									if (!trickWon) {
										countPlayers = 0;
										handGiven = 0;
										bidsEntered = 0;
										cardPlayed = 0;

										Collections.rotate(playOrder, 4 - index);
										orderString = "";
										// Check who won the trick and add 1 to that team
										if (trickWinner.equals(playerNames.get(0))
												|| trickWinner.equals(playerNames.get(2)))
											teamWins.put(teamNames.get(0), teamWins.get(teamNames.get(0)) + 1);
										else
											teamWins.put(teamNames.get(1), teamWins.get(teamNames.get(1)) + 1);
										while (countPlayers < 4) {
											orderString = orderString + playerNames.get(playOrder.get(countPlayers))
													+ " ";
											countPlayers++;
										}
										trickWon = true;
									}
								}
							}
							// ROUND ENDS
							// Calculate round score using formula
							// X = teamBids, Y = teamWins
							round++;
							while (round != 4)
								try {
									roundLatch.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							roundLatch.countDown();
							synchronized (lock2) {
								if (!roundDone) {
									tricksPlayed = 0;
									if (teamWins.get(teamNames.get(0)) >= teamBids.get(teamNames.get(0))) {
										scoreA = scoreA + (teamBids.get(teamNames.get(0)) * 10)
												+ (teamWins.get(teamNames.get(0)) - teamBids.get(teamNames.get(0)));
									} else {
										scoreA = scoreA - (teamBids.get(teamNames.get(0)) * 10);
									}
									if (teamWins.get(teamNames.get(1)) >= teamBids.get(teamNames.get(1))) {
										scoreB = scoreB + teamBids.get(teamNames.get(1)) * 10 + teamWins.get(teamNames.get(1))
												- teamBids.get(teamNames.get(1));
									} else {
										scoreB = scoreB - (teamBids.get(teamNames.get(1)) * 10);
									}
									deck = new Deck();
									deck.shuffle();
									roundDone = true;
								}
							}
							String roundScore = "";
							roundScore = roundScore + "Round Score is: Team 1: " + scoreA + "; Team 2: " + scoreB;
							outputWriter.println(MyConstants.ROUND_SCORE);
							outputWriter.println(roundScore);

							allInElse += 1;
							while (allInElse != 4)
								try {
									elseLatch.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							elseLatch.countDown();
							roundOver = true;

						}
					}
				}

				ConnectionThread connection = clients.remove(player);
				connection.closeSocket(player);
				socket.close();
			} catch (

			IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		private void closeSocket(String player) {
			try {
				System.out.println("Closing " + player);
				socket.close();
				clientNumber -= 1;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	// Display the cards played and the winner of the trick
	private void displayTrickWinner(PrintWriter outputWriter) {
		String playedCards = "";
		for (int i = 0; i < 4; i++)
			playedCards = playedCards + playerNames.get(i) + ":" + playerCard.get(playerNames.get(i)) + "; ";

		outputWriter.println(MyConstants.PLAYED_CARDS);
		outputWriter.println("The Cards played are - " + playedCards);
		outputWriter.println(MyConstants.TRICK_WINNER);
		outputWriter.println("The winner of this trick is : " + trickWinner);
	}

	// Display bids of all players and teams
	private void displayBids(PrintWriter outputWriter) {
		// Get player bids
		String pBids = "Player Bids are - ";
		String pName = "";
		String tBids1 = "";
		String tBids2 = "";

		// Get player Bids
		Iterator<String> playerIterator = playerNames.iterator();
		while (playerIterator.hasNext()) {
			pName = playerIterator.next();
			pBids = pBids + pName + ": ";
			pBids = pBids + Integer.toString(playerBids.get(pName)) + " ;";
		}

		// Remove last semicolon
		pBids = pBids.substring(0, pBids.length() - 1);

		// Get Team Bids from playerNames
		tBids1 = tBids1 + "Total bid of Team 1 (" + playerNames.get(0) + "," + playerNames.get(2) + ") = "
				+ teamBids.get(playerNames.get(0) + "," + playerNames.get(2));

		tBids2 = tBids2 + "Total bid of Team 2 (" + playerNames.get(1) + "," + playerNames.get(3) + ") = "
				+ teamBids.get(playerNames.get(1) + "," + playerNames.get(3));

		// Send to clients
		outputWriter.println(MyConstants.PLAYER_BIDS);
		outputWriter.println(pBids);
		outputWriter.println(MyConstants.TEAM1_BIDS);
		outputWriter.println(tBids1);
		outputWriter.println(MyConstants.TEAM2_BIDS);
		outputWriter.println(tBids2);
	}

	// Prints the Names of connected Users
	private void printNames(PrintWriter outputWriter) {
		String usernames = "";
		String team1 = "";
		String team2 = "";
		Iterator<String> playerIterator = playerNames.iterator();

		// Put individual user names into a String
		while (playerIterator.hasNext()) {
			usernames = usernames + playerIterator.next() + ",";
		}

		// Get team names
		team1 = team1 + playerNames.get(0) + "," + playerNames.get(2);
		team2 = team2 + playerNames.get(1) + "," + playerNames.get(3);

		usernames = usernames.substring(0, usernames.length() - 1); // Remove last comma

		outputWriter.println("PrintPlayers");
		outputWriter.println("Players: " + usernames + "; Team 1: " + team1 + "; Team 2: " + team2);
	}

	// Gets the highest card (Same Suit) from an array of Strings (Ex: {"3C","5C})
	public Card getHighestCard(String[] playedCards) {
		List<Card> cards = new ArrayList<Card>();
		for (int i = 0; i < playedCards.length; i++) {
			cards.add(stringToCard(playedCards[i]));
		}
		// Sort Cards
		Collections.sort(cards);
		// Return highest card
		return cards.get(cards.size() - 1);
	}

	// Convert String (Ex:"4C") to Card object
	public Card stringToCard(String s) {
		String[] characters = null;
		characters = s.split("");
		Rank rank = null;
		Suit suit = null;

		if ((characters.length) == 3) {
			rank = Rank.valueOf(getString2Rank("10"));
			suit = Suit.valueOf(characters[2]);
		} else {
			rank = Rank.valueOf(getString2Rank(characters[0]));
			suit = Suit.valueOf(characters[1]);
		}
		return new Card(rank, suit);
	}

	// Gets String version of Enum. (Ex: String "2" to ENUM "TWO")
	public String getString2Rank(String s) {
		switch (s) {
		case "2":
			return "TWO";
		case "3":
			return "THREE";
		case "4":
			return "FOUR";
		case "5":
			return "FIVE";
		case "6":
			return "SIX";
		case "7":
			return "SEVEN";
		case "8":
			return "EIGHT";
		case "9":
			return "NINE";
		case "10":
			return "TEN";
		case "J":
			return "JACK";
		case "Q":
			return "QUEEN";
		case "K":
			return "KING";
		case "A":
			return "ACE";
		}
		return "";
	}

	// To sort 13 distributed cards and convert them to string
	public static String sortHand(Card c[]) {
		int[][] suit = new int[4][13];
		int d = 0, e = 0, f = 0, g = 0;
		for (int i = 0; i < 13; i++) {
			switch (c[i].getSuit().toString()) {
			case "D":
				suit[0][d++] = c[i].getRank().toInteger();
				break;
			case "H":
				suit[1][e++] = c[i].getRank().toInteger();
				break;
			case "S":
				suit[2][f++] = c[i].getRank().toInteger();
				break;
			case "C":
				suit[3][g++] = c[i].getRank().toInteger();
				break;
			}
		}
		for (int i = 0; i < 4; i++)
			Arrays.sort(suit[i]);

		String[] st = { "D", "H", "S", "C" };
		String hand = "";
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 13; j++) {
				// To remove zeroes
				if (suit[i][j] > 0) {
					switch (suit[i][j]) {
					case 11:
						hand = hand + "J" + st[i];
						hand = hand + ",";
						break;
					case 12:
						hand = hand + "Q" + st[i];
						hand = hand + ",";
						break;
					case 13:
						hand = hand + "K" + st[i];
						hand = hand + ",";
						break;
					case 14:
						hand = hand + "A" + st[i];
						hand = hand + ",";
						break;
					default:
						hand = hand + Integer.toString(suit[i][j]) + st[i];
						hand = hand + ",";
					}
				}
			}
			if (!hand.isEmpty())
				hand = hand.substring(0, hand.length() - 1) + ";";
		}
		return hand.substring(0, hand.length());
	}
}
