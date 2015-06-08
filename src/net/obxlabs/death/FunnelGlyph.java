/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.death;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

public class FunnelGlyph {
	PApplet p;
	PVector location;
	PVector offset;
	PVector velocity;
	PVector acceleration;
	PVector target;
	float targetMult;
	float width, height;
	
	float sepMult, aliMult, cohMult;
	float desiredSeparation;
	float aliNeighborDist;
	float cohNeighborDist;

	//float r;
	float maxforce;    // Maximum steering force
	float maxspeed;    // Maximum speed
	float friction;
	char value;
	
	boolean dying;
	
	public FunnelGlyph(PApplet p, char v, float x, float y, float w, float h) {
		this.p = p;
		this.width = w;
		this.height = h;
		value = v;
		acceleration = new PVector(0, 0);
		target = new PVector(0, 0);
		offset = new PVector(x, y);

	    velocity = new PVector(0, -1, 0);

	    location = new PVector(x, y);
	    //r = 144.0f; //set with font size XXX
	    maxspeed = 25f;
	    maxforce = 0.03f;		
	    friction = 0.02f;
	    
	    sepMult = 1.5f;
	    aliMult = 0.8f;
	    cohMult = 1.0f;
	    
	    desiredSeparation = 40.0f;
	    aliNeighborDist = 100;
	    cohNeighborDist = 20;
	    
	    dying = false;
	}
	
	public void kill() {
		dying = true;
	}
	
	public boolean isDead() { return dying && velocity.mag() < 0.1f; }
	
	public boolean outside(int x1, int y1, int x2, int y2) {
		//check if the glyph is within bounds
		//not really accurate, just get a larger box than needed
		if (location.x + width < x1 ||
			location.x - width > x2 ||
			location.y + height < y1)
			//location.y - height > y2) //don't check for this as text always goes up
			return true;
		
		return false;
	}
	
	void setTarget(float x, float y, float m, float o) {
		target.set(x, y);
		
		target.add(offset.x*o, offset.y*o, offset.z*o);
		targetMult = m;
	}
	
	void setFlock(float s, float a, float c, float m, float ds, float ad, float cd) {
		sepMult = s;
		aliMult = a;
		cohMult = c;
		maxspeed = m;
		desiredSeparation = ds;
		aliNeighborDist = ad;
		cohNeighborDist = cd;
	}
	
	void setPosition(float x, float y) {
		location.set(x, y);
	}
	
	void applyForce(PVector force) {
		// We could add mass here if we want A = F / M
		acceleration.add(force);
	}

	// We accumulate a new acceleration each time based on three rules
	void flock(ArrayList<FunnelGlyph> glyphs) {
		PVector sep = separate(glyphs);   // Separation
		PVector ali = align(glyphs);      // Alignment
	    PVector coh = cohesion(glyphs);   // Cohesion
	    PVector up = seek(target);
	    // Arbitrarily weight these forces
	    sep.mult(sepMult);
	    ali.mult(aliMult);
	    up.mult(targetMult);
	    coh.mult(cohMult);
	    // Add the force vectors to acceleration
	    applyForce(sep);
	    applyForce(ali);
	    applyForce(up);
	    applyForce(coh);
	}

	// Method to update location
	void update(ArrayList<FunnelGlyph> glyphs) {
		if (!dying) 
			flock(glyphs);

		// Update velocity
		velocity.add(acceleration);
		// Limit speed
		velocity.limit(maxspeed);
		location.add(velocity);
		
		velocity.mult(1-friction);
		acceleration.mult(0);
	}

	// A method that calculates and applies a steering force towards a target
	// STEER = DESIRED MINUS VELOCITY
	PVector seek(PVector target) {
		PVector desired = PVector.sub(target, location);  // A vector pointing from the location to the target
		// Scale to maximum speed
		desired.normalize();
		desired.mult(maxspeed);

		// Above two lines of code below could be condensed with new PVector setMag() method
		// Not using this method until Processing.js catches up
		// desired.setMag(maxspeed);

		// Steering = Desired minus Velocity
		PVector steer = PVector.sub(desired, velocity);
		steer.limit(maxforce);  // Limit to maximum steering force
		return steer;
	}

	void draw(PGraphics pg) {
		// Draw a triangle rotated in the direction of velocity
		//float theta = velocity.heading() + PApplet.radians(90);
	    
	    pg.pushMatrix();
	    pg.translate(location.x, location.y);
	    //pg.rotate(theta);
	    pg.text(value, 0, 0);
	    pg.popMatrix();
	}
	
	// Separation
	// Method checks for nearby boids and steers away
	PVector separate (ArrayList<FunnelGlyph> glyphs) {
		PVector steer = new PVector(0, 0, 0);
		int count = 0;
		// For every boid in the system, check if it's too close
	    for (FunnelGlyph other : glyphs) {
	    	float d = PVector.dist(location, other.location);
	    	// If the distance is greater than 0 and less than an arbitrary amount (0 when you are yourself)
	    	if ((d > 0) && (d < desiredSeparation)) {
	    		// Calculate vector pointing away from neighbor
	    		PVector diff = PVector.sub(location, other.location);
	    		diff.normalize();
	    		diff.div(d);        // Weight by distance
	    		steer.add(diff);
	    		count++;            // Keep track of how many
	    	}
	    }
	    // Average -- divide by how many
	    if (count > 0) {
	    	steer.div((float)count);
	    }

	    // As long as the vector is greater than 0
	    if (steer.mag() > 0) {
	    	// First two lines of code below could be condensed with new PVector setMag() method
	    	// Not using this method until Processing.js catches up
	    	// steer.setMag(maxspeed);

	    	// Implement Reynolds: Steering = Desired - Velocity
	    	steer.normalize();
	    	steer.mult(maxspeed);
	    	steer.sub(velocity);
	    	steer.limit(maxforce);
	    }
	    return steer;
	}

	// Alignment
	// For every nearby boid in the system, calculate the average velocity
	PVector align (ArrayList<FunnelGlyph> glyphs) {		
		PVector sum = new PVector(0, 0);
		int count = 0;
		for (FunnelGlyph other : glyphs) {
			float d = PVector.dist(location, other.location);
			if ((d > 0) && (d < aliNeighborDist)) {
				sum.add(other.velocity);
				count++;
			}
		}
		if (count > 0) {
			sum.div((float)count);
			// First two lines of code below could be condensed with new PVector setMag() method
			// Not using this method until Processing.js catches up
			// sum.setMag(maxspeed);

			// Implement Reynolds: Steering = Desired - Velocity
			sum.normalize();
			sum.mult(maxspeed);
			PVector steer = PVector.sub(sum, velocity);
			steer.limit(maxforce);
			return steer;
		} 
		else {
			return new PVector(0, 0);
		}
	}

	// Cohesion
	// For the average location (i.e. center) of all nearby boids, calculate steering vector towards that location
	PVector cohesion (ArrayList<FunnelGlyph> glyphs) {
		PVector sum = new PVector(0, 0);   // Start with empty vector to accumulate all locations
		int count = 0;
		for (FunnelGlyph other : glyphs) {
			float d = PVector.dist(location, other.location);
			if ((d > 0) && (d < cohNeighborDist)) {
				sum.add(other.location); // Add location
				count++;
			}
		}
		if (count > 0) {
			sum.div(count);
			//sum.add(offset);
			//PApplet.println(sum);
			return seek(sum);  // Steer towards the location
		} 
		else {
			return new PVector(0, 0);
		}
	}	
}
