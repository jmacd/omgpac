package art.lookingup.patterns.pacman;

import static processing.core.PConstants.P2D;

import java.awt.Color;

import processing.core.PApplet;
import ch.bildspur.artnet.ArtNetClient;

public class OMG extends PApplet {
    public static void main( String[] args ) {
        PApplet.main("art.lookingup.patterns.pacman.OMG");
    }

    PacmanBoard board;
    PacmanSprite sprite;
    PacmanGame game;
    ArtNetClient artnet;

    public void settings() {
	size((int)FULLWIDTH, (int)FULLWIDTH, P2D);
    }

    public void setup() {
        board = new PacmanBoard(this);
        sprite = new PacmanSprite(this);
	game = new PacmanGame(this, board, sprite);
	artnet = new ArtNetClient(null);
	artnet.start();
    }

    float telapsed;
    float rotation;
    float rotSpeed;

    public final static Heading input = null;  // This is not used.

    public final static float MAX_GAME_MILLIS = 600000;

    public final static float FULLWIDTH = 64*10;
    public final static float HALFWIDTH = 32*10;
    public final static float QUARTWIDTH = 16*10;

    public void draw() {
	double deltaMs = 10;
	double speed = 2;
	telapsed += (float) (deltaMs * speed);

        if (game.finished() || this.telapsed > (float)(speed * MAX_GAME_MILLIS)) {
            this.board.reset();
            this.telapsed = (float)(deltaMs * speed);
            game = new PacmanGame(this, this.board, this.sprite);
        }
        game.render(telapsed, input);

        // if (game.pacTicks == 0) {
        //     draw1(deltaMs);
        //     return;
        // }
        background(0);

	translate(HALFWIDTH, HALFWIDTH);
	rotate(rotation);
	translate(-HALFWIDTH, -HALFWIDTH);
	
        boolean pacIsRight = false;

        float aX = game.pacX();
        float aY = game.pacY();

        float bX = game.redX();
        float bY = game.redY();

        if (aX > bX || (aX == bX && bY > aY)) {
            float tX, tY;

            tX = bX;
            tY = bY;

            bX = aX;
            bY = aY;

            aX = tX;
            aY = tY;

            pacIsRight = true;
        }

        float aD = (float) Math.sqrt(aX * aX + aY * aY);
        float bD = (float) Math.sqrt(bX * bX + bY * bY);

        float dX = aX - bX;
        float dY = aY - bY;

        float dAB = (float)Math.sqrt(dX * dX + dY * dY);


	float dRatio;
	if (dAB < 24) {
	    dRatio = HALFWIDTH/24f;
	} else if (dAB < HALFWIDTH) {
	    dRatio = HALFWIDTH/dAB;
	} else {
	    dRatio = 0.75F * PacmanBoard.MAX_DISTANCE / dAB;
	}

	// Render 

	translate(QUARTWIDTH, HALFWIDTH);

        // Note: this determines whether Pac is on the right or the left side.
        if (!pacIsRight) {
            float xoffset = HALFWIDTH / 2;
            translate(xoffset, 0);
            rotate((float) -Math.PI);
            translate(-xoffset, 0);
        }

	if (pacIsRight) {
	    rotSpeed += Math.PI/100000f;
	    rotSpeed = Math.min(rotSpeed, (float)Math.PI/10000f);
	} else {
	    rotSpeed -= Math.PI/100000f;
	    rotSpeed = Math.max(rotSpeed, -(float)Math.PI/10000f);
	}

	rotation += rotSpeed * (float)deltaMs;

        float rr;
        if (dX == 0) {
            rr = (float) Math.PI * 3 / 2;
        } else {
            rr = (float) Math.atan(dY / dX);
        }

        rotate((float)Math.PI * 2f - rr);

        scale(dRatio, dRatio);

        translate(-aX, -aY);

	image(game.buffer, 0, 0);

	transmit();
    }

    // Pacman stays on screen w/ this draw()
    public void draw1(double deltaMs) {
        background(0);

        float pacX = game.pacX();
        float pacY = game.pacY();

        float xratio = (float) FULLWIDTH / (float) game.buffer.width;

        translate(0, -HALFWIDTH);
        scale(xratio, xratio);
        translate(0, -pacY);

        // the READY! text is 6 blocks above the start position, pan the camera down.
        if (telapsed < PacmanGame.STANDSTILL_MILLIS) {
            if (telapsed < PacmanGame.READY_MILLIS) {
                translate(0, xratio*PacmanBoard.BLOCK_PIXELS * 6f);
            } else {
                float ratio = (telapsed - PacmanGame.READY_MILLIS) /
                    (PacmanGame.STANDSTILL_MILLIS - PacmanGame.READY_MILLIS);
                translate(0, (1 - ratio) * PacmanBoard.BLOCK_PIXELS * 6f);
            }
        }

        image(game.buffer, 0, 0);
    }

    public void transmit() {
	if (frameCount % 100 != 0) {
	    return;
	}
	loadPixels();

	byte[] dmxData = new byte[512];	

	for (int i = 0; i < pixels.length; i++) {
	    for (int j = 0; j < 510; j += 3) {
		dmxData[j] = (byte)Colors.red(pixels[i]);
		dmxData[j+1] = (byte)Colors.green(pixels[i]);
		dmxData[j+2] = (byte)Colors.blue(pixels[i]);
	    }

	    artnet.unicastDmx("127.0.0.1", 0, 0, dmxData);
	}

	updatePixels();
    }
}
