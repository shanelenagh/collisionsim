import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class CollisionSimulator {
	
	public static void main(String[] args) {
		Frame f = new Frame("Collision Simulator");
		f.setSize(450,450);
		CollisionSimulatorGUI csg = new CollisionSimulator.CollisionSimulatorGUI(60);
		f.add(csg);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) { 
				System.exit(0); 
			}
		});
		f.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent ce) {
				csg.resize();
			}
		});
		f.setVisible(true);
		new Thread(csg).start();
	}

	private static class CollisionSimulatorGUI extends Canvas implements Runnable {
		// Below block items are the config parameters for the simulation--change as desired (until UI developed)
		private double timeAccelerator = 30.0;
		private int milesPerPixel = 1;
		private Point ac1 = new Point(0 / milesPerPixel,0 / milesPerPixel);
		private Point ac2 = new Point(55 / milesPerPixel,0 / milesPerPixel);
		private double ac1Velo = 500.0;
		private double ac2Velo = 300.0;
		private int ac1Heading = 45;
		private int ac2Heading = 0;
		private int collisionMilesLimit = 10;
		private boolean isColliding = false;
		private double distMiles;
		
		private long timeCounter = System.currentTimeMillis();
		private int secondCounter = 0;
		private int cellSize;
		private boolean isStateChanged = true;
		
		private Image backBuffer, ac1Image, ac2Image;
		private Graphics backBufferGraphics;
		
		public CollisionSimulatorGUI(int cellSizeInPixels) {
			this.cellSize = cellSizeInPixels;
		}	
		
		@Override
		public void run() {
			System.out.println("Calculated collision time is "
				+calculatedCollisionTime(ac1, ac2, ac1Heading, ac2Heading)*60.0*60.0); 
			while (true)
				if (System.currentTimeMillis() - timeCounter > 1000) {
					this.timeCounter = System.currentTimeMillis();
					this.secondCounter += 1;
					ac1.x += Math.sin(Math.toRadians(ac1Heading)) * ac1Velo / 60.0 / 60.0 * timeAccelerator;
					ac1.y += Math.cos(Math.toRadians(ac1Heading)) * ac1Velo / 60.0 / 60.0 * timeAccelerator;
					ac2.x += Math.sin(Math.toRadians(ac2Heading)) * ac2Velo / 60.0 / 60.0 * timeAccelerator;
					ac2.y += Math.cos(Math.toRadians(ac2Heading)) * ac2Velo / 60.0 / 60.0 * timeAccelerator;
					this.distMiles = Point2D.distance(ac1.x, ac1.y, ac2.x, ac2.y)*milesPerPixel;
					this.isColliding = distMiles < collisionMilesLimit;
					this.isStateChanged = true;
					repaint();
				} else
					try { 
						this.isStateChanged = false; 
						Thread.sleep(100); 
					} catch (InterruptedException ie) { /* Interruptions can just continue */ }
		}		

		private void setupBackBuffer() {			
			backBuffer = this.createImage(getWidth(), getHeight());
			backBufferGraphics = backBuffer.getGraphics();			
		}
	
		public void resize() {
			setupBackBuffer();
			this.isStateChanged = true;
			repaint();
		}
		
		@Override
		public void update(Graphics g) {
			if (backBufferGraphics == null)
				setupBackBuffer();
			else
				backBufferGraphics.clearRect(0, 0, getWidth(), getHeight());			
			
			drawGridlines(backBufferGraphics);	
			drawAircraft(backBufferGraphics);
			drawStatusBar(backBufferGraphics);

			g.drawImage(backBuffer, 0, 0, this);			
		}
		
		@Override
		public void paint(Graphics g) {
			if (isStateChanged)
				update(g);
		}		
		
		private void drawAircraft(Graphics g) {
			// Draw AC's as red/green triangles pointed in the direction of their heading
			if (this.ac1Image == null) {
				this.ac1Image = this.createImage(10,10);
				final Graphics2D ac1g = (Graphics2D) this.ac1Image.getGraphics();
				ac1g.setColor(new Color(0, 153, 0));
				ac1g.rotate(Math.toRadians(-ac1Heading), 5, 5);
				ac1g.fillPolygon(new int[] { 0, 10, 5, 0 },  new int[] { 0, 0, 10, 0 }, 4);	
				ac1g.dispose();
			}
			g.drawImage(ac1Image, ac1.x, ac1.y, this);
			if (this.ac2Image == null) {
				this.ac2Image = this.createImage(10,10);
				final Graphics2D ac2g = (Graphics2D) this.ac2Image.getGraphics();	
				ac2g.setColor(new Color(153, 0, 0));
				ac2g.rotate(Math.toRadians(-ac2Heading), 5, 5);
				ac2g.fillPolygon(new int[] { 0, 10, 5, 0 },  new int[] { 0, 0, 10, 0 }, 4);		
				ac2g.dispose();
			}
			g.drawImage(ac2Image, ac2.x, ac2.y, this);	
		}
			
		private void drawGridlines(Graphics g) {
			if (this.isColliding)
				g.setColor(Color.RED);
			for (int x = 0; x < getWidth(); x = x + cellSize)
				g.drawLine(x, 0, x, getHeight());
			for (int y = 0; y < getHeight(); y = y + cellSize)
				g.drawLine(0, y, getWidth(), y);
			g.setColor(Color.BLACK);
		}		

		public void drawStatusBar(Graphics g) {
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(0, getHeight()-15, getWidth(), getHeight());
			g.setColor(Color.BLACK);
			g.drawString(String.format("%ds (%.0fs sim) - green: %d,%d \u2192 red: %d,%d = %.1fnmi %s",
				secondCounter, secondCounter*timeAccelerator, ac1.x, ac1.y, ac2.x, ac2.y, distMiles, 
				isColliding ? "COLLIDING" : ""), 0, getHeight()-2);
		}
		
		private double calculatedCollisionTime(Point p1, Point p2, double velo1, double velo2) {
			final double a = (p2.x - p1.y) * milesPerPixel;
			final double b = velo2*Math.sin(Math.toRadians(-ac2Heading)) - velo1*Math.cos(Math.toRadians(-ac1Heading));
			final double c = (p2.y- p1.y) * milesPerPixel;
			final double d = velo2*Math.cos(Math.toRadians(-ac2Heading)) - velo1*Math.cos(Math.toRadians(-ac1Heading));
			
			return - ((a*b + c*d) / (b*b + d*d));
		}
	}
}