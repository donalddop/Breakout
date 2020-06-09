/*
 * File: Breakout.java
 * -------------------
 * Name: Donald Nikkessen 10260102
 * Section Leader: Martijn Stegemans
 * 
 * This is a simple breakout game, you use the mouse to control a paddle. A ball
 * starts moving when you click, the objective is to remove all the bricks with the 
 * ball without allowing the ball to touch the bottom of the screen.
 * 
 * Methods organised in order:
 * - Variable instantiations
 * - Main methods
 * - Setup methods
 * - Control methods
 * - Game mechanics
 * - String messages
 * - Scoreboard methods
 * - Testing/Debugging methods
 */

import acm.graphics.*;
import acm.program.*;
import acm.util.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;

public class Breakout extends GraphicsProgram
{
	/** Width and height of application window in pixels */
	public static final int APPLICATION_WIDTH = 400;
	public static final int APPLICATION_HEIGHT = 600;

	/** Dimensions of game board (usually the same) */
	private static final int WIDTH = APPLICATION_WIDTH;
	private static final int HEIGHT = APPLICATION_HEIGHT;

	/** Dimensions of the paddle */
	private static final int PADDLE_WIDTH = 60;
	private static final int PADDLE_HEIGHT = 10;

	/** Offset of the paddle up from the bottom */
	private static final int PADDLE_Y_OFFSET = 30;

	/** Number of bricks per row */
	private static final int NBRICKS_PER_ROW = 10;

	/** Number of rows of bricks */
	private static final int NBRICK_ROWS = 10;

	/** Separation between bricks */
	private static final int BRICK_SEP = 4;

	/** Width of a brick */
	private static final int BRICK_WIDTH =
		(WIDTH - (NBRICKS_PER_ROW - 1) * BRICK_SEP) / NBRICKS_PER_ROW;

	/** Height of a brick */
	private static final int BRICK_HEIGHT = 8;

	/** Radius of the ball in pixels */
	private static final int BALL_RADIUS = 10;

	/** Offset of the top brick row from the top */
	private static final int BRICK_Y_OFFSET = 70;

	/** Number of turns */
	private static final int NTURNS = 3;
	private int turns = NTURNS;

    /** Assigning the paddle */
    private GRect paddle;

    /** Assigning the ball */
    private GOval ball;

    /** Assigning the scoreboards */
    private GLabel scoreBoard, highScoreBoard;

    /** Velocity of the ball */
    private double vx, vy;

    /** Random number generator */
    private RandomGenerator rgen = RandomGenerator.getInstance();

    /** Number of bricks on game start */
    private int brickCounter = 100;
	private int bricksBroken = 0;

    /** Animation delay for ball movement */
    private static final int DELAY = 17;

    /** Keeps track of the number of times the ball hits the paddle **/
    private int timesHitPaddle = 0;
	private int totalTimesHit = 0;

    /** Keeps track of the score */
    private int currentScore = 0;

    /** Keeps track of the highest score since application launch */
    private int highScore = 0;

	/** Mouse coordinates */
	private int mX;
	private int mY;

    /** Audio clip for collision sound */
    AudioClip bounceClip = MediaTools.loadAudioClip("bounce.au");


	/**** Runs the Breakout program. **/
	public void run() {
		setUpBricks();
        createPaddle();
        createBall();
        //brickCounter = 3; // To test winning scenario
        setUpScoreBoard();
        welcome();
        for (turns = NTURNS; turns > 0; turns--) {
	        play();
        }
	}

    /** Main method. Starts the round upon mouseclick */
    private void play() {  
        getBallVelocity();
        while (true) {
            pause (DELAY);
            movingBall();
			movePaddle();
            //autoPilot(); // Testing method for automatic paddle movement

			// When the ball leaves the bottom of the window            
			if (ball.getY() > getHeight() && turns >= 0) { 
                ballLost();   
            }
            if (brickCounter == 0) { // Winning conditions met
                winner();
				gameOver();                
                checkHighScore();
                waitForClick(); // Click to restart the game
                reset();
            }
        }
    }

	/**** Setup methods **/

    /** Drawing the paddle */
    private void createPaddle() {
        int padHeight = (APPLICATION_HEIGHT - PADDLE_Y_OFFSET);
        paddle = new GRect(APPLICATION_WIDTH/2 - PADDLE_WIDTH /2, padHeight,
                           PADDLE_WIDTH, PADDLE_HEIGHT);
	    paddle.setFillColor(Color.BLACK);
    	paddle.setFilled(true);
        add(paddle);
        addMouseListeners();
    }

    /** Draws a new ball if there are any turns left */
    private void createBall() {
        if (turns > 0) {
            int centerX = APPLICATION_WIDTH/2;
            int centerY = APPLICATION_HEIGHT/2;
            ball = new GOval(centerX - 10, centerY - 10, 2 * BALL_RADIUS, 2 * BALL_RADIUS);
            ball.setFillColor(Color.BLACK);
            ball.setFilled(true);
            add(ball);
        } else if ( turns == 0 ) { // no turns left = you lose
            loserMessage();
			gameOver();
            checkHighScore();
            waitForClick(); // click to restart the game
            reset();    
        }
    }

    /** Drawing the colored bricks */
    private void setUpBricks() {
        // Array for the colors of the bricks 
        Color colours[] = new Color[10];
		colours[0] = Color.RED;
		colours[1] = Color.RED;
		colours[2] = Color.ORANGE;
		colours[3] = Color.ORANGE;
		colours[4] = Color.YELLOW;
		colours[5] = Color.YELLOW;
		colours[6] = Color.GREEN;
        colours[7] = Color.GREEN;
        colours[8] = Color.CYAN;
        colours[9] = Color.CYAN;

        int y = BRICK_Y_OFFSET;
        for (int i = 0; i < 10; i++) {
            int x = BRICK_SEP -2;
            for (int j = 0; j < 10; j++) {
                GRect brick = new GRect(x, y, BRICK_WIDTH, BRICK_HEIGHT);
                brick.setLocation(x, y); 
                brick.setColor(colours[i]);
		        brick.setFillColor(colours[i]);
	        	brick.setFilled(true);
                add(brick);
                x += (BRICK_SEP + BRICK_WIDTH);
            }  
            y += (BRICK_SEP + BRICK_HEIGHT);          
        }
    }

    /** Resets the game to starting conditions */
    private void reset() {
        removeAll();
        turns = NTURNS;
        timesHitPaddle = 0;
		totalTimesHit = 0;
        currentScore = 0;
		bricksBroken = 0;
		brickCounter = 100;
        run();
    }

	/**** Control methods **/

    /** Moves the paddle to the x-coordinate of the mouse */

	public void mouseMoved(MouseEvent e) {
		mX = (int) e.getPoint().getX();
		mY = (int) e.getPoint().getY();
	}

    public void movePaddle() {
		// Paddle is on screen and not aligned with cursor
        if (mX > 0 && mX < APPLICATION_WIDTH &&
			mX < paddle.getX() + PADDLE_WIDTH /2 -2 ||
			mX > paddle.getX() + PADDLE_WIDTH /2 +2) {
			// move left
			if (mX < paddle.getX() + PADDLE_WIDTH /2 && paddle.getX() > 5) { 
				paddle.move(-5.0, 0.0);
			// move right
			} else if (mX > paddle.getX() + PADDLE_WIDTH /2 &&
					   paddle.getX() + PADDLE_WIDTH < APPLICATION_WIDTH - 5) {
				paddle.move(5.0, 0.0);
			}
		// Cursor is out of bounds or not frozen
		} else if (mX < paddle.getX() + PADDLE_WIDTH /2 -2 &&
					mX > paddle.getX() + PADDLE_WIDTH /2 +2) {
			// move left
			if (mX -6 < paddle.getX() + PADDLE_WIDTH /2 && paddle.getX() > 5) { 
				paddle.move(-5.0, 0.0);
			// move right
			} else if (mX +6 > paddle.getX() + PADDLE_WIDTH /2 &&
					   paddle.getX() + PADDLE_WIDTH < APPLICATION_WIDTH - 5) {
				paddle.move(5.0, 0.0);
			}
		}
    }

	/**** Game mechanics methods **/

    /** Losing a ball */
    private void ballLost() {
        remove(ball);
		turns--;
		createBall();
		getBallVelocity();
		totalTimesHit += timesHitPaddle;
        timesHitPaddle = 0;
		ballLostMessage();       
    }

    /** Gives the ball its initial vector */
    private void getBallVelocity() {
        vy = 3.0;        
        vx = rgen.nextDouble(1.0, 5.0);
        if (rgen.nextBoolean(0.5)) {
            vx = -vx;
        }
    }

    /** The method for ball movement, includes the rules for bouncing the 
        ball off walls, bricks and the paddle. Moves the ball in pixels 
		according to the current value of vx and vy */
    private void movingBall() {
        ball.move(vx, vy);

	    // Check for handling collisions with objects 
        GObject collider = getCollider();
		if (collider == paddle) {
			paddleCollision();
			ball.move(vx, vy);
			ball.move(vx, vy);
		} else if (collider != null && 
				   collider != scoreBoard && 
				   collider != highScoreBoard) {
			brickCollision();
			ball.move(vx, vy);
		} else {

		// When the ball hits the top or sides of the window 
			if (ball.getX() + BALL_RADIUS *2 > getWidth() && vx > 0) { // Right
				bounceClip.play();
				vx = -vx;
			}
			if (ball.getX() < 0 && vx < 0) { // Left
				bounceClip.play();
				vx = -vx;
			}
			if (ball.getY() < 0 && vy < 0) { // Top
				bounceClip.play();
				vy = -vy;
			}
    	}
	}

	/** Rules for hitting the paddle */
	private void paddleCollision() {
        bounceClip.play();
		GObject collider = getCollider();
		// Bouncing off the sides of the paddle reverses both x and y velocity,
		// unless the ball's center is lower than the paddle surface.

		// Left side
    	if (paddle.getX() > ball.getX() + BALL_RADIUS) { 
	        if (vy > 0 && paddle.getY() > ball.getY() + BALL_RADIUS) {
				vy = -vy;
			}
			if (vx > 0) {
	        	vx = -vx;
			}
	    // right side
		} else if (paddle.getX() + PADDLE_WIDTH < ball.getX() + BALL_RADIUS) { 
	    	if (vy > 0 && paddle.getY() > ball.getY() + BALL_RADIUS) {
				vy = -vy;
			}
	    	if (vx < 0) {
	        	vx = -vx;
			}
		// Bouncing off the paddle's surface only reverses y-velocity
    	} else { 
			if (vy > 0) {
				vy = -vy;
			}
		} 		
        // Speed up mechanism
        speedUp();
    }
	
	/** When the ball hits a brick (condition: hits an object that isn't the paddle) */
	private void brickCollision() {
        bounceClip.play();
		GObject collider = getCollider();
		// left side
        if (collider.getX() > ball.getX() + BALL_RADIUS && vx > 0) { 
			if (vy < 0 && collider.getY() + BRICK_HEIGHT < ball.getY() + BALL_RADIUS) {
				vy = -vy;
			}
			if (vx > 0) {
	        	vx = -vx;
			}
		// right side
        } else if (collider.getX() + BRICK_WIDTH < ball.getX() + BALL_RADIUS && vx < 0) { 
			if (vy < 0 && collider.getY() + BRICK_HEIGHT < ball.getY() + BALL_RADIUS) {
				vy = -vy;
			}
			if (vx < 0) {
	        	vx = -vx;
			}
        } else {    // Hitting a brick from the top or bottom
            vy = -vy;
        }
    	keepScore();
   	 	remove(collider);
   		brickCounter--;
    }

	/** Speeds the ball up after certain amounts of hits from the paddle */
    private void speedUp() { 
        if (timesHitPaddle == 5) { // Doubles horizontal ball velocity after six hits
            vx *= 2.0;
            timesHitPaddle++; 
        } else if (timesHitPaddle == 20) { 
			// Doubles horizontal and vertical ball velocity after 20 hits
        	vx *= 2.0;
			vy *= 2.0;
            timesHitPaddle++;	
		} else {
			timesHitPaddle++; // ...more?
		}
    }

    /** Returns objects the ball collides with */
    private GObject getCollider() {
        if ((getElementAt(ball.getX(), ball.getY())) != null) {
            return getElementAt(ball.getX() , ball.getY());
        } else if ((getElementAt(ball.getX() + BALL_RADIUS*2, ball.getY())) != null) {
            return getElementAt(ball.getX() + BALL_RADIUS*2, ball.getY());
        } else if ((getElementAt(ball.getX(), ball.getY() + BALL_RADIUS*2)) != null) {
            return getElementAt(ball.getX(), ball.getY() + BALL_RADIUS*2);
        } else if ((getElementAt(ball.getX() + BALL_RADIUS*2, 
									ball.getY() + BALL_RADIUS*2)) != null) {
            return getElementAt(ball.getX() + BALL_RADIUS*2, ball.getY() + BALL_RADIUS*2);
        } else {
            return null;
        }
    }



	/**** String messages **/

    /** Rewarding message for winning the game */
    private void winner() {
        ball.setVisible(false);
        GLabel YouRule = new GLabel ("You win! Click to play again!",
                                     getWidth()/2, getHeight()/2);
        YouRule.move(-YouRule.getWidth()/2, -YouRule.getHeight());
        YouRule.setColor(Color.BLACK);
        add (YouRule);
	}

	/** Motivating message for non-winners */
	private void loserMessage() {
		GLabel YouSuck = new GLabel ("Loser, click to restart.", getWidth()/2, getHeight()/2);
        YouSuck.move(-YouSuck.getWidth()/2, -YouSuck.getHeight());
        YouSuck.setColor(Color.BLACK);
        add (YouSuck);
	}

	/** End-game performance statistics */
	private void gameOver() {
		bricksBroken = 100 - brickCounter;
		GLabel stats = new GLabel ("You hit the ball " + totalTimesHit +
								   " time(s), and broke " + bricksBroken + " brick(s).",
                                     getWidth()/2, getHeight()/2 + 20);
        stats.move(-stats.getWidth()/2, -stats.getHeight());
        stats.setColor(Color.BLACK);
        add (stats);
    }

    /** Starting message */
    private void welcome() {
        GLabel Welcome = new GLabel ("Click to start!", getWidth()/2, getHeight()/2 + 50);
        Welcome.move(-Welcome.getWidth()/2, -Welcome.getHeight());
        Welcome.setColor(Color.BLACK);
        add (Welcome);
        waitForClick();
        remove (Welcome);
    }

    /** Display number of turns left after losing a ball */
    private void ballLostMessage() {
		int livesLeft = turns - 1;
        GLabel turnsLeft = new GLabel ("Lives left: "+ livesLeft, getWidth()/2, getHeight()/2 - 50);
        turnsLeft.move(-turnsLeft.getWidth()/2, -turnsLeft.getHeight());
        turnsLeft.setColor(Color.BLACK);
        add (turnsLeft);
        waitForClick();
        remove (turnsLeft);
    }
    
	/**** Scoreboard Methods **/

    /** Initializing the scoreboards */
    private void setUpScoreBoard() {
        scoreBoard = new GLabel ("Score: " + currentScore, getWidth()/2, getHeight() - 5);
        scoreBoard.move(-scoreBoard.getWidth()/2, 0);
        scoreBoard.setColor(Color.BLACK);
        add (scoreBoard);
	
		if (highScore == 0) { // First time playing
			highScoreBoard = new GLabel ("No high score set", getWidth()/2, 15);
    	    highScoreBoard.move(-highScoreBoard.getWidth()/2, 0);
	        highScoreBoard.setColor(Color.BLACK);
        	add (highScoreBoard);
		} else { // High score has already been set
			highScoreBoard = new GLabel ("High score: " + highScore, getWidth()/2, 15);
    	    highScoreBoard.move(-highScoreBoard.getWidth()/2, 0);
	        highScoreBoard.setColor(Color.BLACK);
        	add (highScoreBoard);
		}
    }

    /** Current score method */
    private void keepScore(){
        remove (scoreBoard);
        GObject collider = getCollider();
        Color c = collider.getColor();
        if (c == Color.RED) { // Higher colors give more points
            currentScore += 50;
        } else if (c == Color.ORANGE) {
            currentScore += 40;
        } else if (c == Color.YELLOW) {
            currentScore += 30;
        } else if (c == Color.GREEN) {
            currentScore += 20;
        } else if (c == Color.CYAN) {
            currentScore += 10;
        }
        scoreBoard = new GLabel ("Score: " + currentScore, getWidth()/2, getHeight() - 5);
        scoreBoard.move(-scoreBoard.getWidth()/2, 0);
        scoreBoard.setColor(Color.BLACK);
        add (scoreBoard);
    }

    /** Keeping track of the highscore */
    private void checkHighScore() {
		if (highScore < currentScore && currentScore != 0) { 
			remove (highScoreBoard);
            highScore = currentScore;
            highScoreBoard = new GLabel ("High score: " + highScore, getWidth()/2, 15);
            highScoreBoard.move(-highScoreBoard.getWidth()/2, 0);
            highScoreBoard.setColor(Color.BLACK);
            add (highScoreBoard);
        }
    }

	/**** Testing and debugging methods **/

    /** Testing code to make the paddle follow the ball automatically */
    private void autoPilot() {
        if (ball.getX() > PADDLE_WIDTH /2 
            && ball.getX() + BALL_RADIUS *2 < APPLICATION_WIDTH - PADDLE_WIDTH /2 ) {
            paddle.setLocation((ball.getX() + BALL_RADIUS - PADDLE_WIDTH /2),
                               (APPLICATION_HEIGHT - PADDLE_Y_OFFSET));
        }
    }
}
