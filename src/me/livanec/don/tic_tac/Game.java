package me.livanec.don.tic_tac;

import java.util.Arrays;
import java.util.Collections;

import me.livanec.don.tic_tac.helper.Difficulty;
import me.livanec.don.tic_tac.helper.GameSession;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class Game extends Activity {
	private static final int NO_MOVE_SET = -1;
	private static final String TAG = Game.class.getSimpleName();
	private SharedPreferences preferences;
	private GameSession session;
	private BoardView boardView;
	private MediaPlayer mp;
	private Difficulty difficulty;
	private long won;
	private long loss;
	private long tie;

	static final int[][][] allSolutions = { { { 1, 2 }, { 3, 6 }, { 4, 8 } }, { { 0, 2 }, { 4, 7 } },
			{ { 0, 1 }, { 4, 6 }, { 5, 8 } }, { { 0, 6 }, { 4, 5 } }, { { 1, 7 }, { 3, 5 }, { 2, 6 }, { 0, 8 } },
			{ { 2, 8 }, { 3, 4 } }, { { 0, 3 }, { 2, 4 }, { 7, 8 } }, { { 1, 4 }, { 6, 8 } },
			{ { 0, 4 }, { 2, 5 }, { 6, 7 } } };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		difficulty = Difficulty.Hard;
		Log.d(TAG, "onCreate");
		startNewGame();
		final Object data = getLastNonConfigurationInstance();
		if (data!=null)
			session = (GameSession) data;
		initStats();
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mp = MediaPlayer.create(this, R.raw.outstanding);
	}

	private void initStats() {
		preferences = getSharedPreferences(getString(R.string.scoreboard), 0);
		won = preferences.getLong(getString(R.string.won_stat), 0);
		loss = preferences.getLong(getString(R.string.loss_stat), 0);
		tie = preferences.getLong(getString(R.string.tie_stat), 0);
	}

	private void startNewGame() {
		startNewGame(difficulty);
	}

	private void startNewGame(Difficulty difficulty) {
		Log.d(TAG, "Starting a new game.");
		session = new GameSession(difficulty);
		boardView = new BoardView(this);
		setContentView(boardView);
		boardView.requestFocus();
	}

	/** Change the square only if it's a valid move */
	protected boolean setSquareIfValid(int x, int y, int value) {
		Log.d(TAG, "if square is not selected, select, and update game session.");
		if (!session.getUnselected().contains(new Integer(value))) {
			return false;
		}
		setSquare(x, y, value);
		// update game session
		removeSelection(value);
		session.setUserSelected(true);
		int count = session.getUserPickCount();
		session.getUserPicks()[count] = value;
		session.setUserPickCount(++count);
		if (isWinner(value, session.getUserPicks())) {
			mp.start();
			Toast.makeText(this, getResources().getString(R.string.won), Toast.LENGTH_SHORT).show();
			preferences.edit().putLong(getString(R.string.won_stat), ++won).commit();
		} else if (session.getUnselected().size() == 0) {
			session.setGameOver(true);
			Toast.makeText(this, getResources().getString(R.string.game_over), Toast.LENGTH_SHORT).show();
			preferences.edit().putLong(getString(R.string.tie_stat), ++tie).commit();
		}
		return true;
	}

	protected void pickSquare(int tile, int count) {
		session.getCompPicks()[count] = tile;
		session.setCompPickCount(++count);
		session.getPuzzle()[tile] = 2;
		removeSelection(tile);
		session.setUserSelected(false);
	}

	/** Change the tile at the given coordinates */
	private void setSquare(int x, int y, int value) {
		session.getPuzzle()[value] = 1;
	}

	/** Return the tile at the given coordinates */
	private int getSquare(int x) {
		return session.getPuzzle()[x];
	}

	private void removeSelection(int value) {
		for (Integer index : session.getUnselected()) {
			if (value == index.intValue()) {
				session.getUnselected().remove(index);
				return;
			}
		}
	}

	protected String getSqureString(int x, int y) {
		int v = getSquare(x + y * 3);
		String value = "";
		if (v == 0)
			return "";
		else if (v == 1)
			value = getResources().getString(R.string.x);
		else
			value = getResources().getString(R.string.o);
		return value;
	}

	/**
	 * select square for computer player
	 */
	public void compSelect() {
		Log.d(TAG, "computer player selecting a square, then updating session.");
		int move = NO_MOVE_SET;
		int count = session.getCompPickCount();

		if (difficulty == Difficulty.Medium) {
			move = defensivePick();
		} else if (difficulty == Difficulty.Hard) {
			move = defenseAndOffensePick();
		}
		if (move == NO_MOVE_SET && session.getUnselected().size() > 0) {
			Collections.shuffle(session.getUnselected());
			move = session.getUnselected().get(0).intValue();
		}
		pickSquare(move, count);
		checkForWinner(move);
	}

	private void checkForWinner(int move) {
		if (isWinner(move, session.getCompPicks())) {
			Toast.makeText(this, getResources().getString(R.string.lost), Toast.LENGTH_SHORT).show();
			preferences.edit().putLong(getString(R.string.loss_stat), ++loss).commit();
		} else if (session.getUnselected().size() == 0) {
			session.setGameOver(true);
			Toast.makeText(this, getResources().getString(R.string.game_over), Toast.LENGTH_SHORT).show();
			preferences.edit().putLong(getString(R.string.tie_stat), ++tie).commit();
		}
	}

	protected boolean isCenterOpen() {
		return session.isCenterOpen();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.new_game:
			startNewGame();
			break;
		case R.id.difficulty:
			new AlertDialog.Builder(this).setTitle(R.string.new_game_title).setItems(R.array.difficulty,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialoginterface, int i) {
							difficulty = i == 0 ? Difficulty.Easy : i == 1 ? Difficulty.Medium : Difficulty.Hard;
							startNewGame(difficulty);
						}
					}).show();
			break;
		case R.id.score:
			startActivity(new Intent(this, ScoreBoard.class));
			break;
		}
		return true;
	}

	protected int getUnselectedCount() {
		return session.getUnselected().size();
	}

	/**
	 * 0 | 1 | 2 --------- 3 | 4 | 5 --------- 6 | 7 | 8
	 * 
	 * @param pick
	 * @param alreadySelected
	 * @return
	 */
	public boolean isWinner(int pick, int[] alreadySelected) {
		Arrays.sort(alreadySelected);
		int[][] solutionForGivenSquare = allSolutions[pick];
		for (int i = 0; i < solutionForGivenSquare.length; i++) {
			int[] aSolution = solutionForGivenSquare[i];
			boolean match = false;
			for (int j = 0; j < aSolution.length; j++) {
				match = isSquareAlreadySelected(aSolution[j], alreadySelected);
				if (!match)
					break;
			}
			if (match) {
				session.setWinner(true);
				session.setGameOver(true);
				session.setWinningSeries(aSolution, pick);
				return true;
			}
		}
		return false;
	}

	public int defensivePick() {
		// can the user win?block
		int move = isInWinningPosition(session.getUserPicks(), session.getCompPicks());
		// is the center open, hit it
		if (move == NO_MOVE_SET && session.isCenterOpen())
			move = GameSession.CENTER_SQUARE;
		return move;
	}

	public int offensivePick(int defenseMove) {
		// can computer win? win
		int move = isInWinningPosition(session.getCompPicks(), session.getUserPicks());
		if (move > NO_MOVE_SET)
			return move;
		// if -1 make offensive move
		// get numbers selected
		// for each number
		if (move == NO_MOVE_SET && defenseMove != NO_MOVE_SET)
			move = defenseMove;
		else
			move = session.getCompPickCount() == 0 ? calculateFirstMove() : calculateMove();

		// can i make a move that generates two winning next moves?
		// can i make a move that generates winning next
		// random
		return move;
	}

	private int calculateFirstMove() {
		int move;
		move = session.isCenterOpen() ? GameSession.CENTER_SQUARE : getAvailableCorner();
		return move;
	}

	private int calculateMove() {
		// the comp has picked moves, the user & comp can't win
		// loop through picks
		// find the pick with the most winning options
		int move = NO_MOVE_SET;
		int mostSolutions = 0;
		int[] picks = session.getCompPicks();
		int count = picks.length;
		for (int i = 0; i < count; i++) {
			if (picks[i] == 99)
				break;
			int[][] solutionsForIndex = allSolutions[i];
			// loop through all solutions for a given pick, find if they are
			// blocked or still in play
			int numSolutions = 0;
			for (int j = 0; j < solutionsForIndex.length; j++) {
				int[] aSolution = solutionsForIndex[j];
				if (isPotentialSolutions(aSolution)) {
					numSolutions++;
				}
			}
			if (numSolutions > mostSolutions) {
				mostSolutions = numSolutions;
				move = i;
			}
		}
		// now move represents solutions
		int[][] solutions = allSolutions[move];
		for (int i = 0; i < solutions.length; i++) {
			if (isPotentialSolutions(solutions[i])) {
				move = solutions[i][0];
				break;
			}
		}
		return move;
	}

	private boolean isPotentialSolutions(int[] solutions) {
		int numSolutions = 0;
		boolean isOpen = false;
		// loop through user selections
		// inner loop through solution array
		// if the solution array is not the user solution, increment
		// numSolutions
		int[] userPicks = session.getUserPicks();
		for (int i = 0; i < userPicks.length; i++) {
			if (userPicks[i] == 99)
				break;
			int pick = userPicks[i];
			isOpen = Arrays.binarySearch(solutions, pick) < 0;
			if (!isOpen)
				break;
		}
		return isOpen;
	}

	private int getAvailableCorner() {
		int length = session.getPuzzle().length;
		int cornerIndex = -1;
		for (int i = 0; i < length; i++) {
			if (isCorner(i) && isSquareOpen(i)) {
				cornerIndex = i;
				break;
			}
		}
		return cornerIndex;
	}

	private boolean isSquareOpen(int index) {
		return session.getPuzzle()[index] == 0;
	}

	private boolean isCorner(int index) {
		// 0,2,6,8
		return index % 2 == 0 && index != 4;
	}

	private int defenseAndOffensePick() {
		int defenseMove = defensivePick();
		// return defenseMove != NO_MOVE_SET || defenseMove ==
		// GameSession.CENTER_SQUARE ? defenseMove : offensivePick();
		return offensivePick(defenseMove);
	}

	public int isInWinningPosition(int[] picks, int[] otherPics) {
		int result = NO_MOVE_SET;
		for (int i = 0; i < picks.length; i++) {
			if (picks[i] < 10) {
				int[][] allSolutionsForPick = allSolutions[picks[i]];
				for (int j = 0; j < allSolutionsForPick.length; j++) {
					int[] aSolution = allSolutionsForPick[j];
					Arrays.sort(aSolution);
					if (Arrays.binarySearch(picks, aSolution[0]) >= 0 && Arrays.binarySearch(otherPics, aSolution[1]) < 0)
						return aSolution[1];
					if (Arrays.binarySearch(picks, aSolution[1]) >= 0 && Arrays.binarySearch(otherPics, aSolution[0]) < 0)
						return aSolution[0];
				}
			}
		}
		return result;
	}

	protected boolean isSquareAlreadySelected(int value, int[] alreadySelected) {
		return Arrays.binarySearch(alreadySelected, value) >= 0;
	}

	public GameSession getSession() {
		return session;
	}

	public void setSession(GameSession session) {
		this.session = session;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return session;
	}
	
	

}
