/*
 * Copyright (C) <2015>  <Jason Lewis>
  
    This program is free software: you can redistribute it and/or modify
    it under the terms of the BSD 3 clause with added Attribution clause license.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    BSD 3 clause with added Attribution clause License for more details.
 */

package net.obxlabs.death;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import processing.core.PApplet;

import pitaru.sonia_v2_9.*;

/**
 * Sound manager for Death sketch.
 * 
 * $LastChangedRevision$
 * $LastChangedDate$
 * $LastChangedBy$
 */
public class SoundManager {
	
	static Logger logger = Logger.getLogger(SoundManager.class);

	//the parent applet
	PApplet p;
	
	//constant for left and right channels
	static final int LEFT = 0;
	static final int RIGHT = 1;
	
	HashMap<String, Sample> samples;	//loaded samples
	HashMap<String, Shift> shifts; 		//shifting samples
	HashMap<String, Integer> heads;		//current frames
	
	/**
	 * Constructor.
	 * @param parent parent applet
	 */
	public SoundManager(PApplet parent) {
		p = parent;
		Sonia.start(p);

		samples = new HashMap<String, Sample>();
		shifts = new HashMap<String, Shift>();
		heads = new HashMap<String, Integer>();
		
		logger.info("Initialized sound manager.");
	}
	
	public int head(String id) { return heads.containsKey(id) ? heads.get(id) : 0; }

	public void loadSample(String id, String path) {
		Sample s = new Sample(path);
		
		//mute by default
		s.setVolume(0, LEFT);
		s.setVolume(0, RIGHT);
		
		samples.put(id, s);
		
		logger.info("Loaded '"+id+"': " + path);
	}

	public void crossfade(String in, String out, int duration) {
		Sample sin = samples.get(in);
		Sample sout = samples.get(out);

		//keep track of outgoing sample frame
		heads.put(out, sout.getCurrentFrame()/(float)sout.getNumFrames() > 0.95f ? 0 : sout.getCurrentFrame());
		
		//shift samples
		shifts.put(in, new Fade(sin, 1, duration, 0, false));		
		shifts.put(out, new Fade(sout, 0, duration, 0, true));
		
		//if the incoming sample is stopped, restart
		if (!sin.isPlaying())
			sin.repeat(head(in), sin.getNumFrames());
	}
	
	public void repeat(String id) {
		Sample s = samples.get(id);
		s.repeat();
	}
	
	public void repeat(String id, float volume) {
		Sample s = samples.get(id);
		s.setVolume(volume, LEFT);
		s.setVolume(volume, RIGHT);
		s.repeat();	
	}
	
	public void play(String id, float volume) {
		Sample s = samples.get(id);
		s.setVolume(volume, LEFT);
		s.setVolume(volume, RIGHT);
		s.play();	
	}

	public void fade(String id, float target, int duration) {
		Sample s = samples.get(id);
		
		//shift sample
		shifts.put(id, new Fade(s, target, 0, 0, false));
	}
	
	public void fadein(String in, int duration) {
		Sample sin = samples.get(in);
		
		//shift sample
		shifts.put(in, new Fade(sin, 1, duration, 0, false));
	}
	
	public void fadeout(String out, int duration) {
		Sample sout = samples.get(out);
		
		//keep track of outgoing sample frame
		heads.put(out, sout.getCurrentFrame());
		
		//shift sample
		shifts.put(out, new Fade(sout, 0, duration, 0, true));
	}
	
	/**
	 * Update the samples.
	 */
	public void update() {		
		//update the shifting samples
		Iterator<String> shiftIt = shifts.keySet().iterator();
		while(shiftIt.hasNext()) {
			Shift s = shifts.get(shiftIt.next());
			s.update();
			if(s.isDone())
				shiftIt.remove();
		}
	}

	/**
	 * Stop the sound manager.
	 */
	public void stop() { Sonia.stop(); }
	
	/**
	 * Shift interface.
	 * @author Bruno
	 *
	 */
	interface Shift {
		/**
		 * Update the shifting.
		 */
		public void update();
		
		/**
		 * Check if the shift is done.
		 * @return true when done
		 */
		public boolean isDone();
	}
	
	/**
	 * Fade transition for a sound sample.
	 */
	class Fade implements Shift {
		Sample sample;			//the sample
		float from, to;			//from and to volumes
		int in;					//duration
		long start;				//when to start in millis
		boolean stopWhenDone;	//flag to stop the sample when done fading
		boolean done;			//true when the fade is done
		
		/**
		 * Constructor.
		 * @param sample		the sample
		 * @param to			volume to fade to
		 * @param in			fade duration
		 * @param delay			delay before fade
		 * @param stopWhenDone	true to stop the sample when done fading
		 */
		public Fade(Sample sample, float to, int in, int delay, boolean stopWhenDone) {
			this.sample = sample;
			this.from = sample.getVolume(LEFT); //assume left = right
			this.to = to;
			this.in = in;
			this.stopWhenDone = stopWhenDone;
			this.start = p.millis() + delay;
			this.done = false;
		}
		
		/**
		 * Update
		 */
		public void update() {
			//already done? nothing to do
			if (done) return; 
			
			//check if we reached the start time for the fade
			long duration = p.millis()-start;
			if (duration < 0) return;

			//if we're done, set the volume to final target volume
			if (duration >= in) {
				sample.setVolume(to, LEFT);
				sample.setVolume(to, RIGHT);
				if(stopWhenDone)
					sample.stop();
				done = true;
				return;
			}
			//if we're still in the fade duration, adjust the volume
			else {
				float volume = from + (duration/(float)in)*(to-from);
				sample.setVolume(volume, LEFT);
				sample.setVolume(volume, RIGHT);
			}
		}
		
		/**
		 * Check if the fade is done.
		 */
		public boolean isDone() { return done; }
	}
}
