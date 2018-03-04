package Client;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import Utils.MyConstants;

public class Client {

	public Client() {
		try {
			// To get input from the keyboard
			Scanner inputKeyboard = new Scanner(System.in);

			Socket socket = new Socket(MyConstants.SERVER, MyConstants.PORT);

			// To give output to the client
			PrintWriter outputWriter = new PrintWriter(socket.getOutputStream(), true);

			// To get input from server
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			// Cards in the hand
			String hand = "";
			int roundNumber = 0;
			String cardPlayed = "";

			// Get User's Name
			System.out.println("Hello Player! Enter your username:");
			String userName = "";
			userName = inputKeyboard.nextLine().toUpperCase();
			outputWriter.println(userName);

			while (true) {
				if (inputReader.readLine().equals(MyConstants.GOOD)) {
					break;
				} else {
					System.out.println("This username already exists. Please enter your username again:");
					userName = inputKeyboard.nextLine().toUpperCase();
					outputWriter.println(userName);
				}
			}
			System.out.println("Connected to Server");
			System.out.println("Waiting for other players");

			if (inputReader.readLine().equals("PrintPlayers"))
				System.out.println(inputReader.readLine());

			System.out.println("\n\nGAME BEGINS\n\n");

			int tricksPlayed = 0;
			boolean gameOver = false;
			boolean roundOver = false;
			String bid = "";
			String s = "";
			while (!gameOver) {

				// GAME BEGINS
				s = inputReader.readLine();
				if (s.equals(MyConstants.WINNER)) {
					// GAME IS OVER
					// Display winning team
					System.out.println(inputReader.readLine());
					gameOver = true;
				} else {
					System.out.println("\nRound " + ++roundNumber + "\n");
					roundOver = false;
					while (!roundOver) {
						// ROUND BEGINS

						// Get Cards
						if (inputReader.readLine().equals(MyConstants.HAND)) {
							hand = inputReader.readLine();
							System.out.println("Getting cards...");
							System.out.println(hand);
						}
						// Get bids
						System.out.print("\nEnter your bid: ");
						bid = inputKeyboard.nextLine();
						System.out.println("Waiting for all players to bid\n");
						outputWriter.println(bid);

						// Display all bids
						displayAllBids(inputReader);

						while (tricksPlayed < 13) {
							// TRICK BEGINS
							tricksPlayed += 1;

							System.out.println("\nTRICK " + tricksPlayed);
							
							// WINNER SHOULD START THE GAME
							if (inputReader.readLine().equals(MyConstants.ORDER))
								System.out.println(inputReader.readLine());
							System.out.print("Choose a card to play: ");
							while (true) {
								cardPlayed = inputKeyboard.nextLine().toUpperCase();
								if (hand.contains(cardPlayed)) {
									//hand.replace(cardPlayed, "");
									outputWriter.println(cardPlayed);
									break;
								}
								System.out.print("Invalid card. Please enter correct card: ");
							}
							System.out.println("Waiting for other players to choose cards\n");
							if (inputReader.readLine().equals(MyConstants.PLAYED_CARDS))
								System.out.println(inputReader.readLine());
							if (inputReader.readLine().equals(MyConstants.TRICK_WINNER))
								System.out.println(inputReader.readLine());
							
						}
						tricksPlayed = 0;
						// ROUND ENDS
						// Display the score of each team
						if (inputReader.readLine().equals(MyConstants.ROUND_SCORE))
							System.out.println(inputReader.readLine());
						roundOver = true;
					}
				}
			}

			System.out.println("Disconnected");
			socket.close();
			inputKeyboard.close();
			System.exit(1);
		} catch (

		IOException e) {
			e.printStackTrace();
		}
	}

	public void displayAllBids(BufferedReader inputReader) throws IOException {
		if (inputReader.readLine().equals(MyConstants.PLAYER_BIDS)) {
			System.out.println(inputReader.readLine());
		}
		if (inputReader.readLine().equals(MyConstants.TEAM1_BIDS)) {
			System.out.println(inputReader.readLine());
		}
		if (inputReader.readLine().equals(MyConstants.TEAM2_BIDS)) {
			System.out.println(inputReader.readLine());
		}

	}

	public static void main(String[] args) {
		new Client();
	}
}
