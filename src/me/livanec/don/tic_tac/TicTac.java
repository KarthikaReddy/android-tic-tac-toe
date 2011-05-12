package me.livanec.don.tic_tac;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

public class TicTac extends Activity {
	protected boolean _active = true;
	protected int _splashTime = 500; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		// thread for displaying the SplashScreen
		Thread splashThread = new Thread() {

			@Override
			public void run() {
				try {
					int waited = 0;
					while (_active && (waited < _splashTime)) {
						sleep(100);
						if (_active) {
							waited += 100;
						}
					}
				} catch (InterruptedException e) {
				} catch (IllegalStateException e) {
				} finally {
					finish();
					startActivity(new Intent(TicTac.this, Game.class));
				}
			}

		};
		splashThread.start();
	}

}
