package org.us._42.laphicet.gomoku;

public interface PlayerController {
	/**
	 * Returns a name for the Player.
	 * 
	 * @param game The Gomoku game controller.
	 * @param value A value representing the player's token.
	 * This can be used to implement multiple players within one PlayerController.
	 * @return A name.
	 */
	public String name(Gomoku game, int value);
	
	/**
	 * Reports an error message to the player.
	 * 
	 * @param message An error message.
	 */
	public void report(Gomoku game, String message);
	
	/**
	 * Informs the player that a token on the board has been changed.
	 * 
	 * @param game The Gomoku game controller.
	 * @param x The x coordinate on the game board.
	 * @param y The y coordinate on the game board.
	 * @param value The value of the token.
	 */
	public void informChange(Gomoku game, int x, int y, int value);
	
	/**
	 * Informs the player that someone has won.
	 * 
	 * @param game The Gomoku game controller.
	 * @param value The value representing the winning player's token.
	 */
	public void informWinner(Gomoku game, int value);
	
	/**
	 * Requests a move to be played on the given Gomoku game board.
	 * The move should be submitted with {@link Gomoku#submitMove(int, int, long)}.
	 * The exact same key should be used as the third argument.
	 * 
	 * @param game The Gomoku game controller.
	 * @param value The piece to be played.
	 * @param key Secret value used to submit your move.
	 * @return Whether the player made a move or not.
	 */
	public boolean getMove(Gomoku game, int value, long key);
	
	/**
	 * Signals the start of a new game.
	 * 
	 * @param game The Gomoku game controller.
	 * @param value A value representing the player's token.
	 */
	public void gameStart(Gomoku game, int value);
	
	/**
	 * Signals the end of a game.
	 * 
	 * @param game The Gomoku game controller.
	 */
	public void gameEnd(Gomoku game);
}
