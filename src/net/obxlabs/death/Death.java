/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.death;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

import controlP5.*;

import TUIO.TuioCursor;
import TUIO.TuioProcessing;

import processing.core.PApplet;
import processing.core.PFont;

public class Death extends PApplet {

	private static final long serialVersionUID = -7026006910482457618L;

	static Logger logger = Logger.getLogger(Death.class);
	
	static final int FPS = 30;
	
	//properties editable in the config.properties file
	static boolean FULLSCREEN;				//true to open in fullscreen
	static int FRAME_WIDTH;					//frame width in window mode
	static int FRAME_HEIGHT;				//frame height in window mode
	static int SMOOTH_LEVEL;				//anti-aliasing level	
	
	static boolean TUIO_ENABLE;				//true to enable tuio input
	static int TUIO_PORT;					//port of the TUIO connection

	static boolean DEBUG;					//true to show debug layer
	static boolean MENU;					//true to show menu layer

	static String AUDIO_FORMAT = "aif";
	static String AUDIO_DRONE = "drone";
	static String AUDIO_SHORT = "short";
	static float AUDIO_DRONE_VOLUME = 1.0f;
	static float AUDIO_SHORT_VOLUME = 0.5f;
	static int AUDIO_NUM_SHORT_SOUNDS = 8;
	
	static float[] SCROLL_VERTICAL_MARGINS;		//top margin between edge and text
	static float[] SCROLL_HORIZONTAL_MARGINS;	//left and right margins between e	
	static String[] SCROLL_TEXT_FILES;
	static String[] SCROLL_FONTS;
	static float[] SCROLL_SPEEDS;
	static int[] SCROLL_COLORS;
	static float SCROLL_FLICKER_SPEED;
	static float SCROLL_FLICKER_TIME;
	static float SCROLL_FLICKER_ACTIVITY_MULTIPLIER;
	static boolean[] SCROLL_FLICKER_LAYERS;
	
	static String[] FUNNEL_TEXT_FILES;
	static String FUNNEL_FONT;
	static int FUNNEL_FONT_SIZE;
	static int[] FUNNEL_COLOR_RANGE;
	static float FUNNEL_SPEED;
	static float FUNNEL_PRE_SEPARATION;
	static float FUNNEL_PRE_ALIGNMENT;
	static float FUNNEL_PRE_COHESION;
	static float FUNNEL_PRE_MAX_SPEED;
	static float FUNNEL_PRE_SEPARATION_DISTANCE;
	static float FUNNEL_PRE_ALIGNMENT_DISTANCE;
	static float FUNNEL_PRE_COHESION_DISTANCE;
	static float FUNNEL_POST_SEPARATION;
	static float FUNNEL_POST_ALIGNMENT;
	static float FUNNEL_POST_COHESION;
	static float FUNNEL_POST_MAX_SPEED;
	static float FUNNEL_POST_SEPARATION_DISTANCE;
	static float FUNNEL_POST_ALIGNMENT_DISTANCE;
	static float FUNNEL_POST_COHESION_DISTANCE;
	
	static int LIGHTNING_CELL_HEIGHT;
	static float LIGHTNING_CELL_MAX_SCALE;
	static float LIGHTNING_ROWS_SPEED;
	static int LIGHTNING_COLUMNS;
	static float LIGHTNING_HORIZONTAL_NOISE;
	static float LIGHTNING_VERTICAL_NOISE;
	static float LIGHTNING_HORIZONTAL_SPEED;
	static float LIGHTNING_VERTICAL_SPEED;
	static float LIGHTNING_TOUCH_EFFECT;
	static float LIGHTNING_MASS;
	static float LIGHTNING_MASS_SPEED;
	static int LIGHTNING_COLOR;
	static int LIGHTNING_MARGIN_TOP;
	static int LIGHTNING_MARGIN_BOTTOM;
	
	static int SKY_COLOR;
	static int GROUND_COLOR;
	static int HORIZON_MARGIN;
	static float HORIZON_SPEED;

	static float TOUCH_ACTIVITY_INCREMENT;
	static float TOUCH_ACTIVITY_DECREMENT;
	static int TOUCH_ACTIVITY_HOLD_TIME;

	PFont debugFont;						//font used in the debug layer
	
	TuioProcessing tuioClient;				//TUIO client
	Map<Integer, Touch> touches;			//map of current active touches
	String tuioServerAddr;					//address of the TUIO server
	float touchActivity;					//activity counter from 0-0.5 for both touch
	long touchActivityEnd; 					//time that touch activity ends
	int touchCount;							//current number of touches
	
	Funnel[] funnels;
	
	ArrayList<TextLayer> layers;
	HashMap<String, HashMap<Integer, PFont>> textFonts;
	
	ControlP5 cp5;
	
	float cell_width;						//width of lightning cells (pixels)
	
	int now;
	
	static SoundManager soundManager;		//sound manager for the application
	Stack<Integer> funnelSounds;
	
	public void setup() {
		//if set, open in fullscreen
		if (FULLSCREEN) {
			//fill up the screen
			size(displayWidth, displayHeight, OPENGL);
			if (!DEBUG) noCursor(); //hide the cursor
		}
		//if not, open in standard window mode
		else {
			size(FRAME_WIDTH, FRAME_HEIGHT, OPENGL);
		}
		
		smooth(SMOOTH_LEVEL);	//set anti-aliasing
		background(GROUND_COLOR);	//clear background
		frameRate(FPS);			//limit framerate
		noiseDetail(4, 0.5f);

		//setup font map
		textFonts = new HashMap<String, HashMap<Integer, PFont>>();
		
		//create the debug font
		debugFont = getFont("Arial", 16);
		
		//precalculate lightning cell width
		cell_width = width/(float)LIGHTNING_COLUMNS;
		
		//setup all the things!
		setupAudio();
		setupFunnels(2);
		setupControls();
		setupTouch();
		if (TUIO_ENABLE) setupTUIO();	
		setupText();
		
		//start background audio
		soundManager.repeat(AUDIO_DRONE, AUDIO_DRONE_VOLUME);
	}
	
	public void setupAudio() {
		//init sound manager
		soundManager = new SoundManager(this);

		//stop sound manager on exit
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run () {
				soundManager.stop();
			}
		}));

		funnelSounds = new Stack<Integer>();
		
		//load sounds
		soundManager.loadSample(AUDIO_DRONE, "data"+File.separator+"audio"+File.separator+AUDIO_DRONE+"."+AUDIO_FORMAT);
		for(int i = 1; i <= AUDIO_NUM_SHORT_SOUNDS; i++)
			soundManager.loadSample(AUDIO_SHORT+"-"+(i<10?"0":"")+i, "data"+File.separator+"audio"+File.separator+AUDIO_SHORT+"-"+(i<10?"0":"")+i+"."+AUDIO_FORMAT);
	}
	
	public int nextFunnelSound() {
		if (funnelSounds.isEmpty())
			resetFunnelSounds();
			
		return funnelSounds.pop();
	}
	
	public void resetFunnelSounds() {
		funnelSounds.clear();
		
		ArrayList<Integer> ordered = new ArrayList<Integer>();
		for(int i = 1; i <= AUDIO_NUM_SHORT_SOUNDS; i++)
			ordered.add(i);
		
		while(!ordered.isEmpty()) {
			int next = (int)random(ordered.size());			
			funnelSounds.push(ordered.remove(next));
		}
	}
	
	public void setFunnelSpeed(float value) {
		for(Funnel f : funnels)
			f.setSpeed(value);
	}	
	
	public void setFunnelFlock() {
		for(Funnel f : funnels)
			f.setFlock(FUNNEL_PRE_SEPARATION, FUNNEL_PRE_ALIGNMENT, FUNNEL_PRE_COHESION, FUNNEL_PRE_MAX_SPEED, FUNNEL_PRE_SEPARATION_DISTANCE, FUNNEL_PRE_ALIGNMENT_DISTANCE, FUNNEL_PRE_COHESION_DISTANCE,
					   FUNNEL_POST_SEPARATION, FUNNEL_POST_ALIGNMENT, FUNNEL_POST_COHESION, FUNNEL_POST_MAX_SPEED, FUNNEL_POST_SEPARATION_DISTANCE, FUNNEL_POST_ALIGNMENT_DISTANCE, FUNNEL_POST_COHESION_DISTANCE);
	}
	
	public void setupControls() {
		cp5 = new ControlP5(this);
		
		cp5.addKnob("LIGHTNING_ROWS_SPEED")
               .setRange(0.1f, 10f)
               .setValue(LIGHTNING_ROWS_SPEED)
               .setPosition(width-120,height-130)
               .setRadius(50)
               .setDragDirection(Knob.VERTICAL)
               .setCaptionLabel("lightning.rows.speed")
               ;

		cp5.addKnob("setFunnelSpeed")
	        .setRange(0f, 10f)
	        .setValue(FUNNEL_SPEED)
	        .setPosition(width-240,height-130)
	        .setRadius(50)
	        .setDragDirection(Knob.VERTICAL)
	        .setCaptionLabel("funnel.speed")
	        ;
		
		cp5.addKnob("LIGHTNING_MARGIN_TOP")
	        .setRange(0f, width/2)
	        .setValue(LIGHTNING_MARGIN_TOP)
	        .setPosition(width-360,height-130)
	        .setRadius(50)
	        .setDragDirection(Knob.VERTICAL)
	        .setCaptionLabel("lightning.margin.top")
	        ;
		
		cp5.addKnob("LIGHTNING_MARGIN_BOTTOM")
	        .setRange(0f, width/2)
	        .setValue(LIGHTNING_MARGIN_BOTTOM)
	        .setPosition(width-480,height-130)
	        .setRadius(50)
	        .setDragDirection(Knob.VERTICAL)
	        .setCaptionLabel("lightning.margin.bottom")
	        ;


		cp5.addKnob("FUNNEL_PRE_SEPARATION_DISTANCE")
        .setRange(1, 200)
        .setValue(FUNNEL_PRE_SEPARATION_DISTANCE)
        .setPosition(width-840,height-260)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.pre.separation.distance")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_PRE_ALIGNMENT_DISTANCE")
        .setRange(1, 200)
        .setValue(FUNNEL_PRE_ALIGNMENT_DISTANCE)
        .setPosition(width-720,height-260)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.pre.alignment.distance")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_PRE_COHESION_DISTANCE")
        .setRange(1, 200)
        .setValue(FUNNEL_PRE_COHESION_DISTANCE)
        .setPosition(width-600,height-260)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.pre.cohesion.distance")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_PRE_SEPARATION")
        .setRange(0.01f, 10f)
        .setValue(FUNNEL_PRE_SEPARATION)
        .setPosition(width-480,height-260)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.pre.separation")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;

		cp5.addKnob("FUNNEL_PRE_ALIGNMENT")
        .setRange(0.01f, 10f)
        .setValue(FUNNEL_PRE_ALIGNMENT)
        .setPosition(width-360,height-260)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.pre.alignment")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_PRE_COHESION")
        .setRange(0.01f, 10f)
        .setValue(FUNNEL_PRE_COHESION)
        .setPosition(width-240,height-260)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.pre.cohesion")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_PRE_MAX_SPEED")
        .setRange(1f, 50f)
        .setValue(FUNNEL_PRE_MAX_SPEED)
        .setPosition(width-120,height-260)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.pre.max.speed")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_POST_SEPARATION_DISTANCE")
        .setRange(1, 200)
        .setValue(FUNNEL_POST_SEPARATION_DISTANCE)
        .setPosition(width-840,height-390)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.post.separation.distance")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_POST_ALIGNMENT_DISTANCE")
        .setRange(1, 200)
        .setValue(FUNNEL_POST_ALIGNMENT_DISTANCE)
        .setPosition(width-720,height-390)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.post.alignment.distance")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_POST_COHESION_DISTANCE")
        .setRange(1, 200)
        .setValue(FUNNEL_POST_COHESION_DISTANCE)
        .setPosition(width-600,height-390)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.post.cohesion.distance")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;		
		
		cp5.addKnob("FUNNEL_POST_SEPARATION")
        .setRange(0.01f, 10f)
        .setValue(FUNNEL_POST_SEPARATION)
        .setPosition(width-480,height-390)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.post.separation")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;

		cp5.addKnob("FUNNEL_POST_ALIGNMENT")
        .setRange(0.01f, 10f)
        .setValue(FUNNEL_POST_ALIGNMENT)
        .setPosition(width-360,height-390)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.post.alignment")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_POST_COHESION")
        .setRange(0.01f, 10f)
        .setValue(FUNNEL_POST_COHESION)
        .setPosition(width-240,height-390)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.post.cohesion")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;
		
		cp5.addKnob("FUNNEL_POST_MAX_SPEED")
        .setRange(1f, 50f)
        .setValue(FUNNEL_POST_MAX_SPEED)
        .setPosition(width-120,height-390)
        .setRadius(50)
        .setDragDirection(Knob.VERTICAL)
        .setCaptionLabel("funnel.post.max.speed")
        .addCallback(new CallbackListener() {
		    public void controlEvent(CallbackEvent theEvent) {
			      switch(theEvent.getAction()) {
			        case(ControlP5.ACTION_BROADCAST):
			        setFunnelFlock();
			        break;
			      }
			    }
			 })
        ;		
		
		if (MENU) cp5.show();
		else cp5.hide();		
	}

	public void setupFunnels(int numFunnels) {
		//get the font
		PFont font = getFont(FUNNEL_FONT, FUNNEL_FONT_SIZE, "fonts"+File.separator+FUNNEL_FONT);
		
		funnels = new Funnel[numFunnels];
		
		for(int i = 0; i < numFunnels; i++) {
			//get words from the poem
			ArrayList<String> words = new ArrayList<String>();
			String[] lines = loadStrings(FUNNEL_TEXT_FILES[i]);
			for(String line : lines) {
				if (line.isEmpty()) continue;
				String[] lineWords = line.split(" ");
				for(String word : lineWords) {
					words.add(word);
				}
			}
			
			//convert to array
			String[] wordArray = new String[words.size()];
			words.toArray(wordArray);
			
			//create the funnels
			funnels[i] = new Funnel(this, wordArray, i*(width/numFunnels) + (width/numFunnels/2), height + FUNNEL_FONT_SIZE, font, FUNNEL_FONT_SIZE);
			funnels[i].setColorRange(FUNNEL_COLOR_RANGE[0], FUNNEL_COLOR_RANGE[1]);
		}
		
		setFunnelSpeed(FUNNEL_SPEED);
		setFunnelFlock();
	}
	
	public void setupTouch() {
		touches = Collections.synchronizedMap(new HashMap<Integer, Touch>());
		touchActivity = 0;
		touchActivityEnd = 0;
		touchCount = 0;
	}
	
	public void setupTUIO() {
		//init TUIO client
		logger.info("Init TUIO");
		tuioClient  = new TuioProcessing(this);
		try {
		    InetAddress addr = InetAddress.getLocalHost();

		    //convert to string
		    tuioServerAddr = addr.getHostAddress() + ":" + TUIO_PORT;
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage());
		}
	}	
	
	public void setupText() {
		//load each text file
		layers = new ArrayList<TextLayer>();
		for (int i = 0; i < SCROLL_TEXT_FILES.length; i++) {
			String[] lines = loadStrings(SCROLL_TEXT_FILES[i]);

			String font = SCROLL_FONTS[i < SCROLL_FONTS.length ? i : SCROLL_FONTS.length-1];
			
			//create each text layer
   		 	layers.add(createLayer(width-SCROLL_HORIZONTAL_MARGINS[i]*2, height-SCROLL_VERTICAL_MARGINS[i]*2, font, 12*(i+1), lines));
		
   		 	logger.debug("Fonts: " + textFonts.get(font).keySet().size());
		}
	}

	public TextLayer createLayer(float layerWidth, float layerHeight, String font, float fontSize, String[] lines) {
		final int FONT_PRECISION = 1;
		
		//get the font path
		String fontPath = "fonts"+File.separator+font;
		
		//create tmp font to calculate size of lines
		PFont tmpFont = createFont(fontPath, 72);
		
		//set the temp font
		textFont(tmpFont);
		
		//create the layer
		TextLayer layer = new TextLayer(this, layerWidth, layerHeight);
		
		//for each line of text, add a line of the right size to the layer
		//for(String line : lines) {
		for(int i = lines.length-1; i >= 0; i--) {
			String line = lines[i];
			
			if (line.length() == 0)
				layer.addLine(line, getFont(font, 24, fontPath), 24);
			else {
				//find the best size that fits the width of the screen
				//by trying the temp font at different sizes
				int tmpSize = 1;		
				do {
					textSize(tmpSize+=1);
				} while(textWidth(line) < layerWidth);
				
				layer.addLine(line, getFont(font, tmpSize%FONT_PRECISION==0?tmpSize:tmpSize+FONT_PRECISION-tmpSize%FONT_PRECISION, fontPath), tmpSize);
			}
		}
		
		return layer;
	}
	
	public PFont getFont(String name, int size) { return getFont(name, size, null); }
	public PFont getFont(String name, int size, String path) {
		//check if font family is available
		HashMap<Integer, PFont> fonts = textFonts.get(name);
		if (fonts == null) {
			fonts = new HashMap<Integer, PFont>();
			textFonts.put(name, fonts);
		}
		
		//check if font size is available
		PFont font = fonts.get(size);
			
		//if not, create the font and return it
		if (font == null) {
			font = createFont(path == null ? name : path, size);
			fonts.put(new Integer(size), font);
		}
		
		return font;
	}
	
	public void draw() {
		now = millis();
		
		soundManager.update();
		updateActivity();
		updateFunnels();
		updateLayers();
		
		drawBackground();
		drawLayer(0);
		drawFunnel(0);
		drawLayer(1);
		drawFunnel(1);
		drawLayer(2);
		
		//draw debug and menu layer
		if (DEBUG) drawDebug();
		if (MENU) drawMenu();
	}
	
	/**
	 * Update the touch activity level based on touch properties
	 */
	public void updateActivity() {
		//touch activity increases for some time after the last touch updated
		//if we haven't reached the time set to stop activity increase, then adjust activity upwards
		if (touchActivityEnd > now) {
			//increase activity based on number of touches (max 0.5 for 1 touch, 1.0 for 2.
			if (touchActivity < (touchCount==0?0.5f:touchCount*0.5f)) {
				touchActivity += TOUCH_ACTIVITY_INCREMENT;
				if (touchActivity > 1.0f) touchActivity = 1.0f;
			}
		}
		//decrease activity if touches stop some time ago
		else if (touchActivity > 0.0f) {
			touchActivity -= TOUCH_ACTIVITY_DECREMENT;
	    	if (touchActivity < 0.0f) touchActivity = 0;
		}
	}
	
	/**
	 * Update the text funnels
	 */
	public void updateFunnels() {
		//update funnels
		for(Funnel f : funnels)
			f.update();
	}
	
	/**
	 * Update the scrolling text layers
	 */
	public void updateLayers() {
		for(int i = 0; i < layers.size(); i++) {
			//get the layer
			TextLayer layer = layers.get(i);
			
			//scroll layers
			layer.y += SCROLL_SPEEDS[i];

			//make letters flicker
			
			if (SCROLL_FLICKER_LAYERS[i])
				layer.flicker(SCROLL_FLICKER_SPEED, SCROLL_FLICKER_TIME, map(touchActivity, 0, 1, 0.5f, 1)*SCROLL_FLICKER_ACTIVITY_MULTIPLIER);
		}
	}
	
	public void drawFunnel(int index) { funnels[index].draw(); }
		
	public void drawLayer(int index) {		
		pushMatrix();
		translate(SCROLL_HORIZONTAL_MARGINS[index], SCROLL_VERTICAL_MARGINS[index]);
		fill(SCROLL_COLORS[index]);		
		layers.get(index).draw();
		popMatrix();
	}
	
	public void drawBackground() {
		//clear background
		background(GROUND_COLOR);

		//draw sky
		int horizon = height/2;
		int sky_split = 0;
		
		float cell_height = LIGHTNING_CELL_HEIGHT + (int)((noise(now/10000f)-0.5f)*2*LIGHTNING_CELL_MAX_SCALE*LIGHTNING_CELL_HEIGHT);

		horizon += (noise(now*HORIZON_SPEED)-0.5f)*2*HORIZON_MARGIN;
		sky_split = (int)(noise(now/10000f)*width);
		
		//draw sky and ground
		noStroke();
		fill(SKY_COLOR);
		rect(0, 0, width, horizon);
		rect(sky_split, horizon, width-sky_split, cell_height);
		
		//offset horizon of lightning
		horizon += (noise(now/20000f)-0.5f)*2*HORIZON_MARGIN;

		//get the number of rows needed at this time
		float num_rows;
		num_rows = noise(now*(LIGHTNING_ROWS_SPEED/10000f));
		num_rows = map(num_rows, 0.25f, 0.75f, 0, 1);
		num_rows = constrain(num_rows, 0, 1);
		num_rows *= height/cell_height;		

		int start = 0;
		int end = (int)num_rows;
		
		//constrain the number of rows to not exceed lightning margins
		//if (horizon - (cell_height*num_rows)/2 < LIGHTNING_MARGIN_TOP)
			//num_rows -= ceil((LIGHTNING_MARGIN_TOP-(horizon-(cell_height*num_rows)/2))/cell_height);
		if (horizon - (cell_height*num_rows)/2 < LIGHTNING_MARGIN_TOP)
		start += ceil((LIGHTNING_MARGIN_TOP-(horizon-(cell_height*num_rows)/2))/cell_height);

		//if (horizon + (cell_height*num_rows)/2 > height-LIGHTNING_MARGIN_BOTTOM)
			//num_rows -= ceil(((horizon+(cell_height*num_rows)/2) - (height-LIGHTNING_MARGIN_BOTTOM))/cell_height);
		if (horizon + (cell_height*num_rows)/2 > height-LIGHTNING_MARGIN_BOTTOM)
		end -= ceil(((horizon+(cell_height*num_rows)/2) - (height-LIGHTNING_MARGIN_BOTTOM))/cell_height);

		//time offset
		float to = now*LIGHTNING_HORIZONTAL_SPEED;		
		
		//mass change over time
		float massMult = noise(now/(10000f/LIGHTNING_MASS_SPEED));
		
		//draw lighting
		for(int i = 0; i < LIGHTNING_COLUMNS; i++) {
			for(int j = start; j < end; j++) {
				float c = i + (i>=LIGHTNING_COLUMNS/2 ? -to : to);
				fill(noise(c*LIGHTNING_HORIZONTAL_NOISE, j*LIGHTNING_VERTICAL_NOISE - now*LIGHTNING_VERTICAL_SPEED) < LIGHTNING_MASS*massMult-touchActivity*LIGHTNING_TOUCH_EFFECT ? LIGHTNING_COLOR : color(255, 0));
				rect(i*cell_width, horizon - (cell_height*num_rows)/2 + j*cell_height, cell_width, cell_height);
			}
		}
	}
	
	/**
	 * Draw debug layer.
	 */
	public void drawDebug() {
		//draw debug info
		int vh = 24;
		fill(0);
		noStroke();
		textAlign(LEFT, BASELINE);
		textFont(debugFont);
		text("fps: " + frameRate, 10, vh);		
		text("heap: " + ((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576) +
				" / " + (Runtime.getRuntime().totalMemory()/1048576) + "mb", 10, vh+=24);
		text("TUIO server: " + tuioServerAddr, 10, vh+=24);
		text("touch activity: " + touchActivity, 10, vh+=24);
		
		
		//draw touches
		synchronized (touches) {
			Iterator<Integer> it = touches.keySet().iterator();
		    while (it.hasNext()) {
		    	Touch touch = touches.get(it.next());

		    	// draw the Touch
       			stroke(255, 0, 0);
       			ellipse(touch.x, touch.y, 50, 50);
		    }
		}
		
		//draw lightning margins
		noFill();
		stroke(255, 0, 0, 150);
		line(0, LIGHTNING_MARGIN_TOP, width, LIGHTNING_MARGIN_TOP);
		line(0, height-LIGHTNING_MARGIN_BOTTOM, width, height-LIGHTNING_MARGIN_BOTTOM);
	}
	
	/**
	 * Draw the menu options.
	 */
	public void drawMenu() {
		//draw debug info
		fill(0);
		noStroke();
		textAlign(LEFT, BASELINE);
		textFont(debugFont);
		int h = height-12;
		text("m: toggle this menu", 10, h);	
		text("d: toggle debug layer", 10, h+=24);			
	}
	
	/**
	 * Handle mouse press events.
	 */
	public void mousePressed() { 
		if (TUIO_ENABLE) return;
		addTouch(0, mouseX, mouseY);
	}
	
	/**
	 * Handle mouse release events.
	 */
	public void mouseReleased() {
		if (TUIO_ENABLE) return;
		removeTouch(0);
	}
	
	/**
	 * Handle mouse drag events.
	 */
	public void mouseDragged() {
		if (TUIO_ENABLE) return;
		updateTouch(0, mouseX, mouseY);
	}
	
	/**
	 * Handle key press events.
	 */
	public void keyPressed() {
		switch(key) {
		//show/hide debug layer
		case 'd':
		case 'D':
			DEBUG = !DEBUG;
			if (DEBUG) cursor();
			else noCursor();			
			break;
		//show/hide menu
		case 'm':
		case 'M':
			MENU = !MENU;
			if (MENU) cp5.show();
			else cp5.hide();
			break;
		}
	}
	
	/**
	 * Handle TUIO add cursor event.
	 * @param c the cursor
	 */
	public void addTuioCursor(TuioCursor c) 
	{
		addTouch(c.getCursorID(), c.getScreenX(width), c.getScreenY(height));
	}

	/**
	 * Handle TUIO update cursor event.
	 * @param c the cursor
	 */
	public void updateTuioCursor(TuioCursor c) 
	{
		updateTouch(c.getCursorID(), c.getScreenX(width), c.getScreenY(height));
	}

	/**
	 * Handle TUIO remove cursor event.
	 * @param c the cursor
	 */
	public void removeTuioCursor(TuioCursor c) 
	{
		removeTouch(c.getCursorID());
	}	
	
	/**
	 * Add a touch object to the active list.
	 * @param id id
	 * @param x x position
	 * @param y y position
	 */
	public void addTouch(int id, int x, int y) {
		//add to touches
		Touch t = new Touch(id, x, y, millis());
		
		synchronized(touches) {
			touches.put(new Integer(id), t);
			touchActivityEnd = t.start + TOUCH_ACTIVITY_HOLD_TIME;
			touchCount = touches.size();
		}
			
		//assign the touch
		assignTouch(id, x, y);
	}
	
	/**
	 * Update a touch objct in the active list.
	 * @param id id 
	 * @param x x position
	 * @param y y position
	 */
	public void updateTouch(int id, int x, int y) {
		//update the touch
		synchronized (touches) {
			Touch touch = touches.get(new Integer(id));
			if (touch != null)
				touch.set(x, y);
		}
		touchActivityEnd = millis() + TOUCH_ACTIVITY_HOLD_TIME;
		
		//update funnel target
		for(Funnel f : funnels) {
			if (f.touch == id) {
				f.setTarget(x, y);
				return;
			}
		}
		
		//if we didn't find a matching funnel, assign the touch
		assignTouch(id, x, y);
	}
	
	public void assignTouch(int id, int x, int y) {
		//if we didn't find a matching funnel, assign the touch
    	Funnel f = funnels[x < width/2 ? 0 : 1];
    	if (f.touch == -1) {
    		f.setTarget(x, y);
    		f.touch = id;
    		
    		//if the touch is not on the current string, then go to next string
    		FunnelString last = f.last();
    		if (last == null || !last.contains(x, y)) {
	    		f.next();
	    		
	    		int nextSnd = nextFunnelSound();
	    		soundManager.play(AUDIO_SHORT+"-"+(nextSnd<10?"0":"")+nextSnd, AUDIO_SHORT_VOLUME);
    		}
    	}
	}
	
	/**
	 * Remove a touch object from the active list.
	 * @param id id
	 */
	public void removeTouch(int id) {		
		//remove the touch
		synchronized (touches) {
			touches.remove(new Integer(id));			
			touchCount = touches.size();
		}
		
		//check if the touch was linked to a funnel, and detach it
		for(Funnel f : funnels)
			if (f.touch == id)
				f.touch = -1;
	}
	
	public static void main(String _args[]) {
		//configure logger
		PropertyConfigurator.configure("data"+File.separator+"logging.properties");
				
		//load properties
		Properties props = new Properties();
		try {
	        //load a properties file
			props.load(new FileInputStream("data"+File.separator+"config.properties"));
	 
	    	//get the setup properties
			FULLSCREEN = (Boolean.valueOf(props.getProperty("fullscreen", "true")));
			FRAME_WIDTH = (Integer.valueOf(props.getProperty("frame.width", "1280")));
			FRAME_HEIGHT = (Integer.valueOf(props.getProperty("frame.height", "720")));			
			SMOOTH_LEVEL = (Integer.valueOf(props.getProperty("smooth.level", "6")));
			TUIO_ENABLE = (Boolean.valueOf(props.getProperty("tuio.enable", "true")));
			TUIO_PORT = (Integer.valueOf(props.getProperty("tuio.port", "3333")));

			//get the debug properties
			DEBUG = (Boolean.valueOf(props.getProperty("debug", "false")));
			MENU = (Boolean.valueOf(props.getProperty("menu", "false")));

			//get the audio properties
			AUDIO_DRONE_VOLUME = (Float.valueOf(props.getProperty("audio.drone.volume", "1")));
			AUDIO_SHORT_VOLUME = (Float.valueOf(props.getProperty("audio.short.volume", "0.5")));
			
			String[] vms = props.getProperty("scroll.vertical.margins", "0,0,0").split(",");
			SCROLL_VERTICAL_MARGINS = new float[vms.length];
			for(int i = 0; i < vms.length; i++) SCROLL_VERTICAL_MARGINS[i] = Float.valueOf(vms[i]);
			
			String[] hms = props.getProperty("scroll.horizontal.margins", "-30,10,50").split(",");
			SCROLL_HORIZONTAL_MARGINS = new float[hms.length];
			for(int i = 0; i < hms.length; i++) SCROLL_HORIZONTAL_MARGINS[i] = Float.valueOf(hms[i]);
			
			SCROLL_TEXT_FILES = props.getProperty("scroll.text.files", "letter-3.txt,letter-2.txt,letter-1.txt").split(",");			
			SCROLL_FONTS = props.getProperty("scroll.fonts", "PragmataPro Update.ttf").split(",");
			
			String[] ss = props.getProperty("scroll.speeds", "1,2,4").split(",");
			SCROLL_SPEEDS = new float[ss.length];
			for(int i = 0; i < ss.length; i++) SCROLL_SPEEDS[i] = Float.valueOf(ss[i]);
			
			String[] sc = props.getProperty("scroll.colors", "9B000000,CD000000,FF000000").split(",");
			SCROLL_COLORS = new int[sc.length];
			for(int i = 0; i < sc.length; i++) SCROLL_COLORS[i] = unhex(sc[i]);
			
			SCROLL_FLICKER_SPEED = (Float.valueOf(props.getProperty("scroll.flicker.speed", "0.0001")));			
			SCROLL_FLICKER_TIME = (Float.valueOf(props.getProperty("scroll.flicker.time", "0.1")));
			SCROLL_FLICKER_ACTIVITY_MULTIPLIER = (Float.valueOf(props.getProperty("scroll.flicker.activity.multiplier", "1")));
			
			String[] sl = props.getProperty("scroll.flicker.layers", "false,true,true").split(",");
			SCROLL_FLICKER_LAYERS = new boolean[sl.length];
			for(int i = 0; i < sc.length; i++) SCROLL_FLICKER_LAYERS[i] = Boolean.valueOf(sl[i]);
			
			FUNNEL_TEXT_FILES = props.getProperty("funnel.text.files", "left.txt,right.txt").split(",");
			FUNNEL_FONT = props.getProperty("funnel.font", "Exo-Regular.otf");
			FUNNEL_FONT_SIZE = (Integer.valueOf(props.getProperty("funnel.font.size", "72")));
			FUNNEL_SPEED = (Float.valueOf(props.getProperty("funnel.speed", "1")));
			FUNNEL_PRE_SEPARATION = (Float.valueOf(props.getProperty("funnel.pre.separation", "1")));
			FUNNEL_PRE_ALIGNMENT = (Float.valueOf(props.getProperty("funnel.pre.alignment", "1")));
			FUNNEL_PRE_COHESION = (Float.valueOf(props.getProperty("funnel.pre.cohesion", "1")));
			FUNNEL_PRE_MAX_SPEED = (Float.valueOf(props.getProperty("funnel.pre.max.speed", "1")));
			FUNNEL_PRE_SEPARATION_DISTANCE = (Float.valueOf(props.getProperty("funnel.pre.separation.distance", "1")));
			FUNNEL_PRE_ALIGNMENT_DISTANCE = (Float.valueOf(props.getProperty("funnel.pre.alignment.distance", "1")));
			FUNNEL_PRE_COHESION_DISTANCE = (Float.valueOf(props.getProperty("funnel.pre.cohesion.distance", "1")));
			FUNNEL_POST_SEPARATION = (Float.valueOf(props.getProperty("funnel.post.separation", "1")));
			FUNNEL_POST_ALIGNMENT = (Float.valueOf(props.getProperty("funnel.post.alignment", "1")));
			FUNNEL_POST_COHESION = (Float.valueOf(props.getProperty("funnel.post.cohesion", "1")));
			FUNNEL_POST_MAX_SPEED = (Float.valueOf(props.getProperty("funnel.post.max.speed", "1")));
			FUNNEL_POST_SEPARATION_DISTANCE = (Float.valueOf(props.getProperty("funnel.post.separation.distance", "1")));
			FUNNEL_POST_ALIGNMENT_DISTANCE = (Float.valueOf(props.getProperty("funnel.post.alignment.distance", "1")));
			FUNNEL_POST_COHESION_DISTANCE = (Float.valueOf(props.getProperty("funnel.post.cohesion.distance", "1")));
			
			String[] fc = props.getProperty("funnel.color.range", "FFDCBE78,FFF0C896").split(",");
			FUNNEL_COLOR_RANGE = new int[fc.length];
			for(int i = 0; i < fc.length; i++) FUNNEL_COLOR_RANGE[i] = unhex(fc[i]);
			
			LIGHTNING_CELL_HEIGHT = (Integer.valueOf(props.getProperty("lightning.cell.height", "20")));
			LIGHTNING_CELL_MAX_SCALE = (Float.valueOf(props.getProperty("lightning.cell.max.scale", "1.5")));
			LIGHTNING_ROWS_SPEED = (Float.valueOf(props.getProperty("lightning.rows.speed", "1")));
			LIGHTNING_COLUMNS = (Integer.valueOf(props.getProperty("lightning.columns", "10")));
			LIGHTNING_HORIZONTAL_NOISE = (Float.valueOf(props.getProperty("lightning.horizontal.noise", "0.33")));
			LIGHTNING_VERTICAL_NOISE = (Float.valueOf(props.getProperty("lightning.vertical.noise", "0.1")));
			LIGHTNING_HORIZONTAL_SPEED = (Float.valueOf(props.getProperty("lightning.horizontal.speed", "0.0025")));
			LIGHTNING_VERTICAL_SPEED = (Float.valueOf(props.getProperty("lightning.vertical.speed", "0.0001")));
			LIGHTNING_TOUCH_EFFECT = (Float.valueOf(props.getProperty("lightning.touch.effect", "0.25")));
			LIGHTNING_MASS = (Float.valueOf(props.getProperty("lightning.mass", "0.8")));
			LIGHTNING_MASS_SPEED = (Float.valueOf(props.getProperty("lightning.mass.speed", "1.0")));
			LIGHTNING_COLOR = unhex(props.getProperty("lightning.color", "DC000000"));
			LIGHTNING_MARGIN_TOP = (Integer.valueOf(props.getProperty("lightning.margin.top", "200")));
			LIGHTNING_MARGIN_BOTTOM = (Integer.valueOf(props.getProperty("lightning.margin.bottom", "200")));
			
			SKY_COLOR = unhex(props.getProperty("sky.color", "FFE0FFFF"));
			GROUND_COLOR = unhex(props.getProperty("ground.color", "FF703020"));
			HORIZON_MARGIN = (Integer.valueOf(props.getProperty("horizon.margin", "200")));
			HORIZON_SPEED = (Float.valueOf(props.getProperty("horizon.speed", "0.0001")));
			
			TOUCH_ACTIVITY_INCREMENT = (Float.valueOf(props.getProperty("touch.activity.increment", "0.001")));
			TOUCH_ACTIVITY_DECREMENT = (Float.valueOf(props.getProperty("touch.activity.decrement", "0.001")));
			TOUCH_ACTIVITY_HOLD_TIME = (Integer.valueOf(props.getProperty("touch.activity.hold.time", "3000")));
			
	        logger.info("Configuration properties loaded.");
		} catch (IOException ex) {
			logger.error("Exception occurred when trying to load config file.");
			ex.printStackTrace();
	    }
				
		//launch
		if (FULLSCREEN)
			//use present mode if fullscreen
			PApplet.main(new String[] { "--present", net.obxlabs.death.Death.class.getName() });
		else
			//standard mode for window
			PApplet.main(new String[] { net.obxlabs.death.Death.class.getName() });
	}	
}
