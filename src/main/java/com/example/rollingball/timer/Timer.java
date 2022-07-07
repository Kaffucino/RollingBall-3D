package com.example.rollingball.timer;

import javafx.animation.AnimationTimer;

import java.util.Arrays;

public class Timer extends AnimationTimer {
	private long previous;
	private Updatable updatables[];
	
	public Timer ( Updatable ...updatables ) {
		this.updatables = new Updatable[updatables.length];
		for ( int i = 0; i < updatables.length; ++i ) {
			this.updatables[i] = updatables[i];
		}
	}
	
	@Override public void handle ( long now ) {
		if ( this.previous == 0 ) {
			this.previous = now;
		}
		
		double deltaSeconds = ( now - this.previous ) / 1e9;
		
		this.previous = now;
		
		Arrays.stream ( this.updatables ).forEach ( updatable -> updatable.update ( deltaSeconds ) );
	}
}
