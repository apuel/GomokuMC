package org.us._42.laphicet.gomoku;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Gomoku implements Cloneable {
	public static final int BOARD_LENGTH = 19;
	public static final int PLAYER_COUNT = 2;
	public static final int CAPTURES_TO_WIN = 10;
	public static final int ADJACENT_TO_WIN = 5;
	
	public static enum Alignment {
		HORIZONTAL(1, 0),
		VERTICAL(0, 1),
		ASCENDING(1, 1),
		DESCENDING(1, -1);
		
		public final int dx;
		public final int dy;
		
		private Alignment(int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
		}
	}
	
	private static class Token {
		private int[] adjacent = new int[Alignment.values().length];
		private final int value;
		
		private Token(int value) {
			this.value = value;
		}
		
		@Override
		public Token clone() {
			Token token = new Token(this.value);
			for (Alignment alignment : Alignment.values()) {
				token.adjacent[alignment.ordinal()] = this.adjacent[alignment.ordinal()];
			}
			return (token);
		}
	}
	
	private Token[][] board = new Token[BOARD_LENGTH][BOARD_LENGTH];
	
	private GameStateReporter reporter;
	private List<String> logs = new ArrayList<String>();
	
	private PlayerController[] players = new PlayerController[PLAYER_COUNT];
	private Set<PlayerController> set = new HashSet<PlayerController>();
	
	private int x = -1;
	private int y = -1;
	private boolean submitted = false;
	
	private Random keygen = new SecureRandom();
	private long key = 0;
	
	private int[] captures = new int[PLAYER_COUNT];
	private int[] placed = new int[PLAYER_COUNT];
	
	private int turn = 0;
	
	private Token check5 = null;
	private int winner = 0;
	
	private boolean started = false;
	private boolean abort = false;
	private boolean running = false;
	
	/**
	 * Creates a new game controller for Gomoku.
	 * 
	 * @param reporter A handler for updated game states.
	 * @param players The player controllers representing the participants.
	 */
	public Gomoku(GameStateReporter reporter, PlayerController... players) {
		this.reporter = reporter;
		
		if (players.length < PLAYER_COUNT) {
			throw new IllegalArgumentException("Not enough players to start a game!");
		}
		
		for (int i = 0; i < PLAYER_COUNT; i++) {
			if (players[i] == null) {
				throw new IllegalArgumentException("Players may not be null!");
			}
			this.players[i] = players[i];
			this.set.add(players[i]);
		}
	}
	
	/**
	 * Creates a new game controller for Gomoku.
	 * 
	 * @param players The player controllers representing the participants.
	 */
	public Gomoku(PlayerController... players) {
		this(null, players);
	}
	
	/**
	 * Duplicates another Gomoku instance, overwriting this instance.
	 * 
	 * @param gomoku The instance to duplicate.
	 */
	public void cloneOf(Gomoku gomoku) {
		for (int y = 0; y < BOARD_LENGTH; y++) {
			for (int x = 0; x < BOARD_LENGTH; x++) {
				this.board[y][x] = null;
				if (gomoku.board[y][x] != null) {
					this.board[y][x] = gomoku.board[y][x].clone();
					if ((gomoku.check5 != null) && (gomoku.check5 == gomoku.board[y][x])) {
						this.check5 = this.board[y][x];
					}
				}
			}
		}
		
		for (int i = 0; i < PLAYER_COUNT; i++) {
			this.captures[i] = gomoku.captures[i];
			this.placed[i] = gomoku.placed[i];
		}
		
		this.x = gomoku.x;
		this.y = gomoku.y;
		this.submitted = gomoku.submitted;
		
		this.logs.addAll(gomoku.logs);
		this.turn = gomoku.turn;
		this.winner = gomoku.winner;
		this.started = gomoku.started;
		this.abort = gomoku.abort;
	}
	
	/**
	 * See {@link Gomoku#clone()}.
	 * Allows specification of a new {@link GameStateReporter} and {@link PlayerController}s.
	 * 
	 * @param reporter A handler for updated game states.
	 * @param players The player controllers representing the participants.
	 * @return A clone of this instance.
	 */
	public Gomoku clone(GameStateReporter reporter, PlayerController... players) {
		Gomoku gomoku = new Gomoku(reporter, players);
		gomoku.cloneOf(this);
		return (gomoku);
	}
	
	@Override
	public Gomoku clone() {
		return (this.clone(this.reporter, this.players));
	}
	
	/**
	 * Gets a player by their token value.
	 * 
	 * @param value The player's token value.
	 * @return The PlayerController.
	 */
	public PlayerController getPlayerController(int value) {
		if ((value > 0) && (value <= PLAYER_COUNT)) {
			return (this.players[value - 1]);
		}
		return (null);
	}
	
	/**
	 * Gets the winning token value.
	 * 
	 * @return The winning player's token value.
	 */
	public int getWinner() {
		return (this.winner);
	}
	
	/**
	 * Gets the current turn.
	 * 
	 * @return The current turn.
	 */
	public int getTurn() {
		return (this.turn);
	}
	
	/**
	 * Gets the number of captures a certain player has done.
	 * 
	 * @param value The player's token value.
	 * @return The number of successful captures.
	 */
	public int getCaptureCount(int value) {
		if ((value > 0) && (value <= PLAYER_COUNT)) {
			return (this.captures[value - 1]);
		}
		return (-1);
	}
	
	/**
	 * Gets the number of tokens that a player currently has placed.
	 * 
	 * @param value The player's token value.
	 * @return The number of currently placed tokens.
	 */
	public int getTokensPlaced(int value) {
		if ((value > 0) && (value <= PLAYER_COUNT)) {
			return (this.placed[value - 1]);
		}
		return (-1);
	}
	
	/**
	 * Get the number of tokens adjacent of a certain token (including itself) in a specific alignment.
	 * 
	 * @param x The x coordinate of the token in question.
	 * @param y The y coordinate of the token in question.
	 * @param alignment The alignment on the adjacent tokens.
	 * @return The number of tokens adjacent of a certain token, including itself.
	 */
	public int getAdjacentTokenCount(int x, int y, Alignment alignment) {
		if ((x < 0) || (x >= BOARD_LENGTH) || (y < 0) || (y >= BOARD_LENGTH)) {
			return (-1);
		}
		if (this.board[y][x] == null) {
			return (0);
		}
		return (this.board[y][x].adjacent[alignment.ordinal()]);
	}
	
	/**
	 * Submits the move for the current player.
	 * 
	 * @param x The x coordinate to place at.
	 * @param y The y coordinate to place at.
	 * @param key This MUST be the same key as passed by {@link PlayerController#getMove(Gomoku, int, long)}.
	 */
	public void submitMove(int x, int y, long key) {
		if (this.key == key) {
			this.x = x;
			this.y = y;
			this.submitted = true;
		}
	}
	
	/**
	 * Returns the value for the token at the location.
	 * 
	 * @param x The x coordinate for the token.
	 * @param y The y coordinate for the token.
	 * @return The value of the token at the given coordinates.
	 */
	public int getToken(int x, int y) {
		if ((x < 0) || (x >= BOARD_LENGTH) || (y < 0) || (y >= BOARD_LENGTH)) {
			return (-1);
		}
		if (this.board[y][x] != null) {
			return (this.board[y][x].value);
		}
		return (0);
	}
	
	/**
	 * Updates all adjacent tokens in a certain direction.
	 * 
	 * @param x The added token's x coordinate.
	 * @param y The added token's y coordinate.
	 * @param token The token being placed.
	 * @param alignment The alignment on the adjacent tokens.
	 */
	private void updateAdjacents(int x, int y, Token token, Alignment alignment) {
		int dx = alignment.dx;
		int dy = alignment.dy;
		int index = alignment.ordinal();
		
		int prev = 0;
		int next = 0;
		
		if (this.getToken(x - dx, y - dy) == token.value) {
			prev = this.board[y - dy][x - dx].adjacent[index];
		}
		if (this.getToken(x + dx, y + dy) == token.value) {
			next = this.board[y + dy][x + dx].adjacent[index];
		}
		
		token.adjacent[index] = prev + 1 + next;
		for (int i = 1; i <= prev; i++) {
			this.board[y - (dy * i)][x - (dx * i)].adjacent[index] = token.adjacent[index];
		}
		for (int i = 1; i <= next; i++) {
			this.board[y + (dy * i)][x + (dx * i)].adjacent[index] = token.adjacent[index];
		}
	}
	
	/**
	 * Places a token on the game board.
	 * 
	 * @param x The x coordinate for the token.
	 * @param y The y coordinate for the token.
	 * @param value The value of the token.
	 * @return Whether the piece was place or not.
	 */
	private boolean setToken(int x, int y, int value) {
		if (this.board[y][x] != null) {
			return (false);
		}
		
		this.board[y][x] = new Token(value);
		for (Alignment alignment : Alignment.values()) {
			this.updateAdjacents(x, y, this.board[y][x], alignment);
		}
		return (true);
	}
	
	/**
	 * Re-calculates the number of adjacent tokens in a direction.
	 * It then updates them all accordingly.
	 * 
	 * @param x The added token's x coordinate.
	 * @param y The added token's y coordinate.
	 * @param token The token being removed.
	 * @param alignment The alignment on the adjacent tokens.
	 */
	private void resetAdjacents(int x, int y, Token token, Alignment alignment) {
		int dx = alignment.dx;
		int dy = alignment.dy;
		int index = alignment.ordinal();
		
		int prev = 0;
		for (int i = 1; this.getToken(x - (dx * i), y - (dy * i)) == token.value; i++) {
			prev++;
		}
		for (int i = 1; i <= prev; i++) {
			this.board[y - (dy * i)][x - (dx * i)].adjacent[index] = prev;
		}
		
		int next = 0;
		for (int i = 1; this.getToken(x + (dx * i), y + (dy * i)) == token.value; i++) {
			next++;
		}
		for (int i = 1; i <= next; i++) {
			this.board[y + (dy * i)][x + (dx * i)].adjacent[index] = next;
		}
	}
	
	/**
	 * Removes a token on the game board.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 */
	private void clearToken(int x, int y) {
		if (this.board[y][x] == null) {
			return;
		}
		
		for (Alignment alignment : Alignment.values()) {
			this.resetAdjacents(x, y, this.board[y][x], alignment);
		}
		
		if (this.board[y][x] == this.check5) {
			this.logs.add(this.players[this.check5.value - 1].name(this, this.check5.value) + " no longer has 5 tokens in a row!");
			this.check5 = null;
		}
		
		this.board[y][x] = null;
	}
	
	private static String CAPTURE_FORMAT = "%s captured %s's token at %d, %d.";
	
	/**
	 * Applies a capture (if possible) within the direction of dx and dy.
	 * 
	 * @param x The x coordinate of the placed token.
	 * @param y The y coordinate of the placed token.
	 * @param value The value of the placed token.
	 * @param dx The x direction to attempt a capture in.
	 * @param dy The y direction to attempt a capture in.
	 */
	private void applyCapture(int x, int y, int value, int dx, int dy) {
		PlayerController player = this.players[value - 1];
		
		if (this.getToken(x + (dx * 3), y + (dy * 3)) == value) {
			int x1 = x + (dx * 1);
			int y1 = y + (dy * 1);
			int x2 = x + (dx * 2);
			int y2 = y + (dy * 2);
			
			int v1 = this.getToken(x1, y1);
			int v2 = this.getToken(x2, y2);
			
			if ((v1 > 0) && (v1 != value) && (v2 > 0) && (v2 != value)) {
				this.logs.add(String.format(CAPTURE_FORMAT, player.name(this, value), this.players[v1 - 1].name(this, v1), x1, y1));
				this.logs.add(String.format(CAPTURE_FORMAT, player.name(this, value), this.players[v2 - 1].name(this, v2), x2, y2));
				
				if (this.reporter != null) {
					this.reporter.reportChange(this, x1, y1, 0);
					this.reporter.reportChange(this, x2, y2, 0);
				}
				for (PlayerController p : this.set) {
					p.informChange(this, x1, y1, 0);
					p.informChange(this, x2, y2, 0);
				}
				
				this.placed[v1 - 1]--;
				this.placed[v2 - 1]--;
				this.clearToken(x1, y1);
				this.clearToken(x2, y2);
				this.captures[this.turn % PLAYER_COUNT]++;
			}
		}
	}
	
	/**
	 * Applies captures possible from a given move.
	 * 
	 * @param x The x coordinate of the placed token.
	 * @param y The y coordinate of the placed token.
	 * @param value The value of the placed token.
	 */
	private void applyCaptures(int x, int y, int value) {
		for (Alignment alignment : Alignment.values()) {
			this.applyCapture(x, y, value, alignment.dx, alignment.dy);
			this.applyCapture(x, y, value, -(alignment.dx), -(alignment.dy));
		}
	}
	
	/**
	 * Checks if placing a token at (x2, y2) would capture the token at (x1, y1).
	 * 
	 * @param x1 The x coordinate of the token in question.
	 * @param y1 The y coordinate of the token in question.
	 * @param x2 The x coordinate of the capturing token.
	 * @param y2 The y coordinate of the capturing token.
	 * @param value The value of the capturing token.
	 * @return Whether or not placing a token at (x2, y2) would capture the token at (x1, y1);
	 */
	public boolean wouldCapture(int x1, int y1, int x2, int y2, int value) {
		int token = this.getToken(x1, y1);
		if ((token == value) || (token <= 0) || (value <= 0) || (this.getToken(x2, y2) != 0)) {
			return (false);
		}
		
		int dx = x2 - x1;
		int dy = y2 - y1;
		
		int adx = Math.abs(dx);
		int ady = Math.abs(dy);
		if ((adx > 2) || (ady > 2) || ((adx != ady) && (adx != 0) && (ady != 0))) {
			return (false);
		}
		
		if ((adx == 2) || (ady == 2)) {
			dx /= 2; dy /= 2;
			return ((this.getToken(x1 - dx, y1 - dy) == value) && (this.getToken(x1 + dx, y1 + dy) == token));
		}
		else {
			return ((this.getToken(x1 - (dx * 2), y1 - (dy * 2)) == value) && (this.getToken(x1 - dx, y1 - dy) == token));
		}
	}
	
	/**
	 * Checks if placing a token would capture any tokens within the direction of dx and dy.
	 * 
	 * @param x The x coordinate of the placed token.
	 * @param y The y coordinate of the placed token.
	 * @param value The value of the placed token.
	 * @param dx The x direction to attempt a capture in.
	 * @param dy The y direction to attempt a capture in.
	 * @return 1 for capture, 0 for no capture.
	 */
	private int checkCapture(int x, int y, int value, int dx, int dy) {
		if (this.getToken(x + (dx * 3), y + (dy * 3)) == value) {
			int v1 = this.getToken(x + (dx * 1), y + (dy * 1));
			int v2 = this.getToken(x + (dx * 2), y + (dy * 2));
			if ((v1 > 0) && (v1 != value) && (v2 > 0) && (v2 != value)) {
				return (1);
			}
		}
		return (0);
	}
	
	/**
	 * Counts the number of captures that would occur if the token would be placed.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 * @param value The value of the token.
	 * @return The number of captures that would occur if the token would be placed.
	 */
	public int countCaptures(int x, int y, int value) {
		int captures = 0;
		for (Alignment alignment : Alignment.values()) {
			captures += this.checkCapture(x, y, value, alignment.dx, alignment.dy);
			captures += this.checkCapture(x, y, value, -(alignment.dx), -(alignment.dy));
		}
		return (captures);
	}
	
	/**
	 * Checks if placing this token would put it in a state of capture within the direction of dx and dy.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 * @param value The value of the token.
	 * @param dx The x direction to attempt a capture in.
	 * @param dy The y direction to attempt a capture in.
	 * @return Whether or not placing this token would put it in a state of capture in this direction.
	 */
	private boolean checkCaptured(int x, int y, int value, int dx, int dy) {
		int prev = this.getToken(x - dx, y - dy);
		int next = this.getToken(x + dx, y + dy);
		
		if ((prev == value) && (next != value) && (next > 0)) {
			return (this.getToken(x - (dx * 2), y - (dy * 2)) == next);
		}
		if ((next == value) && (prev != value) && (prev > 0)) {
			return (this.getToken(x + (dx * 2), y + (dy * 2)) == prev);
		}
		
		return (false);
	}
	
	/**
	 * Checks if placing the token would put it in a state of capture.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 * @param value The value of the token.
	 * @return Whether or not placing the token would put it in a state of capture.
	 */
	public boolean isCaptured(int x, int y, int value) {
		for (Alignment alignment : Alignment.values()) {
			if (this.checkCaptured(x, y, value, alignment.dx, alignment.dy)) {
				return (true);
			}
		}
		return (false);
	}
	
	/**
	 * Checks if a token is in danger of capture within the direction of dx and dy.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 * @param value The value of the token.
	 * @param dx The x direction to attempt a capture in.
	 * @param dy The y direction to attempt a capture in.
	 * @return Whether or not the token is in danger of capture in this direction.
	 */
	private boolean checkDanger(int x, int y, int value, int dx, int dy) {
		int prev = this.getToken(x - dx, y - dy);
		int next = this.getToken(x + dx, y + dy);
		
		if (prev == value && next != value && next > 0) {
			return (this.getToken(x - (dx * 2), y - (dy * 2)) == 0);
		}
		if (next == value && prev != value && prev > 0) {
			return (this.getToken(x + (dx * 2), y + (dy * 2)) == 0);
		}
		if (prev == value && next == 0) {
			int token = this.getToken(x - (dx * 2), y - (dy * 2));
			return (token > 0 && token != value);
		}
		if (next == value && prev == 0) {
			int token = this.getToken(x + (dx * 2), y + (dy * 2));
			return (token > 0 && token != value);
		}
		
		return (false);
	}
	
	/**
	 * Checks if a token is in danger of being captured in a certain alignment.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 * @param value The value of the token.
	 * @param alignment The alignment to check.
	 * @return Whether or not the token is in danger of being captured in a certain alignment.
	 */
	public boolean isInDanger(int x, int y, int value, Alignment alignment) {
		return (this.checkDanger(x, y, value, alignment.dx, alignment.dy));
	}
	
	/**
	 * Checks if a token is in danger of being captured.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 * @param value The value of the token.
	 * @return Whether or not the token is in danger of being captured.
	 */
	public boolean isInDanger(int x, int y, int value) {
		for (Alignment alignment : Alignment.values()) {
			if (this.checkDanger(x, y, value, alignment.dx, alignment.dy)) {
				return (true);
			}
		}
		return (false);
	}
	
	/**
	 * Checks for a free three within the direction of dx and dy.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 * @param value The value of the token.
	 * @param dx The x direction to attempt a capture in.
	 * @param dy The y direction to attempt a capture in.
	 * @return Whether or not the token would create a free three in this direction.
	 */
	private boolean createsFreeThree(int x, int y, int value, int dx, int dy) {
		int prev = 0;
		int next = 0;
		
		int last = 0;
		boolean spaced = false;
		
		for (int i = 1; ; i++) {
			int token = this.getToken(x - (dx * i), y - (dy * i));
			if (((token != 0) && (token != value)) || ((token == 0) && this.isCaptured(x + (dx * i), y + (dy * i), value))) {
				if ((i == 1) || (last != 0)) {
					return (false);
				}
				break;
			}
			last = token;
			
			if (token == 0) {
				if (!spaced) {
					spaced = true;
					continue;
				}
				break;
			}
			prev++;
		}
		
		// The last previous token was the space, ignore it
		if (last == 0) {
			spaced = false;
		}
		
		for (int i = 1; ; i++) {
			int token = this.getToken(x + (dx * i), y + (dy * i));
			if (((token != 0) && (token != value)) || ((token == 0) && this.isCaptured(x + (dx * i), y + (dy * i), value))) {
				if ((i == 1) || (last != 0)) {
					return (false);
				}
				break;
			}
			last = token;
			
			if (token == 0) {
				if (!spaced) {
					spaced = true;
					continue;
				}
				break;
			}
			next++;
		}
		
		return ((prev + 1 + next) == (ADJACENT_TO_WIN - 2));
	}
	
	/**
	 * Checks for a free three created by placing a token at an alignment.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 * @param value The value of the token.
	 * @param alignment The alignment to check for a free three at.
	 * @return Whether or not the token would create a double three.
	 */
	public boolean createsFreeThree(int x, int y, int value, Alignment alignment) {
		return (this.createsFreeThree(x, y, value, alignment.dx, alignment.dy));
	}
	
	/**
	 * Checks for double threes created by placing a token.
	 * 
	 * @param x The x coordinate of the token.
	 * @param y The y coordinate of the token.
	 * @param value The value of the token.
	 * @return Whether or not the token would create a double three.
	 */
	public boolean createsDoubleThree(int x, int y, int value) {
		int threes = 0;
		for (Alignment alignment : Alignment.values()) {
			if (this.createsFreeThree(x, y, value, alignment.dx, alignment.dy)) {
				threes++;
			}
		}
		return (threes >= 2);
	}
	
	/**
	 * Validates if placing a token at the given coordinates is allowed.
	 * 
	 * @param x The x coordinate for the token.
	 * @param y The y coordinate for the token.
	 * @param value The value of the token.
	 * @return Whether or not the move is valid.
	 */
	private boolean validateMove(int value) {
		PlayerController player = this.players[value - 1];
		
		if (!(this.submitted)) {
			player.report(this, "Nothing was submitted!");
			return (false);
		}
		
		int token = getToken(this.x, this.y);
		
		if (token < 0) {
			player.report(this, String.format("Coordinates (%d, %d) not in bounds!", this.x, this.y));
			return (false);
		}
		
		if (token != 0) {
			player.report(this, String.format("There is already a token at (%d, %d)!", this.x, this.y));
			return (false);
		}
		
		if (this.isCaptured(this.x, this.y, value)) {
			player.report(this, "You may not place a token into a capture!");
			return (false);
		}
		
		if (this.createsDoubleThree(this.x, this.y, value) && (this.countCaptures(this.x, this.y, value) == 0)) {
			player.report(this, "You may not play a token that would cause a double three!");
			return (false);
		}
		
		return (true);
	}
	
	/**
	 * Checks if the placed piece binds a row of ADJACENT_TO_WIN or more tokens.
	 * If it does, it saves the context of the token and waits a turn to check for interruptions.
	 * If not interrupted, a player is awarded with a win.
	 * 
	 * @param x The x coordinate of the placed token.
	 * @param y The y coordinate of the placed token.
	 * @param value The value of the placed token.
	 */
	private void checkAdjacent(int x, int y, int value) {
		if (this.check5 == null) {
			for (int i = 0; i < Alignment.values().length; i++) {
				if (this.board[y][x].adjacent[i] >= ADJACENT_TO_WIN) {
					this.check5 = this.board[y][x];
					this.logs.add(String.format("%s placed at least %d tokens in a row!",
							this.players[value - 1].name(this, value), ADJACENT_TO_WIN));
					this.logs.add("This move must be countered before the next turn or they will win!");
					break;
				}
			}
		}
		else {
			for (int i = 0; i < Alignment.values().length; i++) {
				if (this.check5.adjacent[i] >= ADJACENT_TO_WIN) {
					this.winner = this.check5.value;
					this.logs.add(this.players[this.check5.value - 1].name(this, this.check5.value) + " has won!");
					break;
				}
			}
			
			if (this.winner == 0) {
				this.logs.add(String.format("%s no longer has %d tokens in a row!",
						this.players[this.check5.value - 1].name(this, this.check5.value), ADJACENT_TO_WIN));
				this.check5 = null;
				this.checkAdjacent(x, y, value);
			}
		}
	}
	
	/**
	 * Tries to fetch the next input from 
	 */
	private void nextTurn() {
		if (!(this.started)) {
			for (int i = 0; i < PLAYER_COUNT; i++) {
				this.players[i].gameStart(this, i + 1);
			}
			this.started = true;
		}
		
		if ((this.winner == 0) && !(this.abort)) {
			PlayerController player = this.players[this.turn % PLAYER_COUNT];
			int value = (this.turn % PLAYER_COUNT) + 1;
			
			this.x = -1; this.y = -1; this.key = this.keygen.nextLong(); this.submitted = false;
			if (!(player.getMove(this, value, this.key))) {
				return;
			}
			if (!(this.validateMove(value))) {
				return;
			}
			
			this.setToken(this.x, this.y, value);
			this.placed[this.turn % PLAYER_COUNT]++;
			this.logs.add(String.format("%s placed a token at %d, %d.", player.name(this, value), this.x, this.y));
			
			for (PlayerController p : this.set) {
				p.informChange(this, this.x, this.y, value);
			}
			
			if (this.reporter != null) {
				this.reporter.reportChange(this, this.x, this.y, value);
			}
			
			this.applyCaptures(this.x, this.y, value);
			int captures = this.captures[this.turn % PLAYER_COUNT];
			if (captures >= CAPTURES_TO_WIN) {
				this.winner = value;
				this.logs.add(String.format("%s has captured %d times and won!", player.name(this, value), captures));
			}
			else {
				this.checkAdjacent(this.x, this.y, value);
			}
			
			if (this.winner != 0) {
				for (PlayerController p : this.set) {
					p.informWinner(this, this.winner);
				}
			}
			
			if (this.reporter != null) {
				this.reporter.logTurn(this, this.logs);
			}
			this.logs.clear();
			
			if (this.winner == 0) {
				this.turn++;
			}
		}
	}
	
	/**
	 * Runs the next turn.
	 */
	public void next() {
		if (this.running) {
			return;
		}
		
		this.running = true;
		this.nextTurn();
		this.running = false;
	}
	
	/**
	 * Auto runs the duration of the game.
	 */
	public void auto() {
		if (this.running) {
			return;
		}
		
		this.running = true;
		while (this.winner == 0 && !(this.abort)) {
			this.nextTurn();
		}
		this.running = false;
	}
	
	/**
	 * Forces a fatal exit condition on the game's main loop.
	 */
	public void abort() {
		this.abort = true;
	}
	
	/**
	 * Resets the state of the game.
	 */
	public void reset() {
		if (!this.running) {
			for (int y = 0; y < BOARD_LENGTH; y++) {
				for (int x = 0; x < BOARD_LENGTH; x++) {
					this.board[y][x] = null;
				}
			}
			
			if (this.started) {
				for (PlayerController p : this.set) {
					p.gameEnd(this);
				}
			}
			
			for (int i = 0; i < PLAYER_COUNT; i++) {
				this.captures[i] = 0;
				this.placed[i] = 0;
			}
			
			this.logs.clear();
			this.turn = 0;
			this.check5 = null;
			this.winner = 0;
			this.started = false;
			this.abort = false;
		}
	}
	
	/**
	 * Resets the state of the game.
	 * 
	 * @param reporter A handler for updated game states.
	 * @param players The player controllers representing the participants.
	 */
	public void reset(GameStateReporter reporter, PlayerController... players) {
		if (!this.running) {
			this.reset();
			this.set.clear();
			
			this.reporter = reporter;
			
			if (players.length < PLAYER_COUNT) {
				throw new IllegalArgumentException("Not enough players to start a game!");
			}
			
			for (int i = 0; i < PLAYER_COUNT; i++) {
				if (players[i] == null) {
					throw new IllegalArgumentException("Player may not be null!");
				}
				this.players[i] = players[i];
				this.set.add(players[i]);
			}
		}
	}
	
	/**
	 * Resets the state of the game.
	 * 
	 * @param players The player controllers representing the participants.
	 */
	public void reset(PlayerController... players) {
		this.reset(null, players);
	}
}
