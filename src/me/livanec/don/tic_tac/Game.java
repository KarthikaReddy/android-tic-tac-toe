package me.livanec.don.tic_tac;

import java.util.Arrays;
import java.util.Collections;

import me.livanec.don.tic_tac.helper.GameSession;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class Game extends Activity {
	private static final String TAG = Game.class.getSimpleName();
	private GameSession session;
	private BoardView boardView;
	private MediaPlayer mp;
	static final int[][][] allSolutions = { { { 1, 2 }, { 3, 6 }, { 4, 8 } }, { { 0, 2 }, { 4, 7 } },
			{ { 0, 1 }, { 4, 6 }, { 5, 8 } }, { { 0, 6 }, { 4, 5 } }, { { 1, 7 }, { 3, 5 }, { 2, 6 }, { 0, 8 } },
			{ { 2, 8 }, { 3, 4 } }, { { 0, 3 }, { 2, 4 }, { 7, 8 } }, { { 1, 4 }, { 6, 8 } },
			{ { 0, 4 }, { 2, 5 }, { 6, 7 } } };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		startNewGame();
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mp = MediaPlayer.create(this, R.raw.outstanding);
	}

	private void startNewGame() {
		Log.d(TAG, "Starting a new game.");
		session = new GameSession();
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
		//update game session
		removeSelection(value);
		session.setUserSelected(true);
		int count = session.getUserPickCount();
		session.getUserPicks()[count] = value;
		session.setUserPickCount(++count);
		if (isWinner(value, session.getUserPicks())) {
			mp.start();
			Toast.makeText(this, getResources().getString(R.string.won), Toast.LENGTH_SHORT).show();
		} else if (session.getUnselected().size() == 0) {
			session.setGameOver(true);
			Toast.makeText(this, getResources().getString(R.string.game_over), Toast.LENGTH_SHORT).show();
		}
		return true;
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
		if (session.getUnselected().size() > 0) {
			Collections.shuffle(session.getUnselected());
			int tile = session.getUnselected().get(0).intValue();
			int count = session.getCompPickCount();
			session.getCompPicks()[count] = tile;
			session.setCompPickCount(++count);
			session.getPuzzle()[tile] = 2;
			session.getUnselected().remove(0);
			session.setUserSelected(false);
			if (isWinner(tile, session.getCompPicks())) {
				Toast.makeText(this, getResources().getString(R.string.lost), Toast.LENGTH_SHORT).show();
			} else if (session.getUnselected().size() == 0) {
				session.setGameOver(true);
				Toast.makeText(this, getResources().getString(R.string.game_over), Toast.LENGTH_SHORT).show();
			}
		}
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
		}
		return true;
	}

	protected int getUnselectedCount() {
		return session.getUnselected().size();
	}

	/**
	 *   0 | 1 | 2
	 *   ---------
	 *   3 | 4 | 5
	 *   ---------
	 *   6 | 7 | 8
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

	protected boolean isSquareAlreadySelected(int value, int[] alreadySelected) {
		return Arrays.binarySearch(alreadySelected, value) >= 0;
	}

	public GameSession getSession() {
		return session;
	}

	public void setSession(GameSession session) {
		this.session = session;
	}

}
