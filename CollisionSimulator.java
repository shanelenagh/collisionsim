import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Map;

public class CollisionSimulator {
	
	private static Font font = new Font(Font.SANS_SERIF, Font.BOLD, 14);
	
	public static void main(String[] args) {
		Frame f = new Frame("Collision Simulator");
		f.setSize(550,550);
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
	
	private static class Aircraft {
		final Point2D.Double loc;
		final double startX, startY;
		final Color color;
		final double velo;
		final int heading;
		
		public Aircraft(Point2D.Double loc, Color color, double velo, int heading) {
			this.loc = loc;
			this.startX = loc.x;
			this.startY = loc.y;
			this.color = color;
			this.velo = velo;
			this.heading = heading;
		}
		
		public double veloX() { return Math.cos(Math.toRadians(heading))*velo; }
		public double veloY() { return Math.sin(Math.toRadians(heading))*velo; }
	}

	private static class CollisionSimulatorGUI extends Canvas implements Runnable {
		// Below block items are the config parameters for the simulation--change as desired (until UI developed)
		private double timeAccelerator = 60.0;
		private double milesPerPixel = 1.0;
		private Aircraft ac1 = new Aircraft(new Point2D.Double(0 / milesPerPixel,0 / milesPerPixel),
			new Color(0, 153, 0), 400.0, 45);
		private Aircraft ac2 = new Aircraft(new Point2D.Double(60 / milesPerPixel,0 / milesPerPixel),
			new Color(153, 0, 0), 300.0, 90);		
		private int collisionMilesLimit = 10;
		private boolean isColliding = false;
		private double distPx;
		
		private long timeCounter = System.currentTimeMillis();
		private double secondCounter = 0;
		private int cellSize;
		private boolean isStateChanged = true;
		
		private Image backBuffer, ac1Image, ac2Image;
		private Graphics backBufferGraphics;
		
		public CollisionSimulatorGUI(int cellSizeInPixels) {
			this.cellSize = cellSizeInPixels;
			
		}	
		
		@Override
		public void run() {
			final double projCollisionTime = calculatedCollisionTime(ac1, ac2);
			final Point2D.Double ac1Coll = aircraftPointAtTime(ac1, projCollisionTime), ac2Coll = aircraftPointAtTime(ac2, projCollisionTime);
			System.out.println("Calculated collision time is "+projCollisionTime*60.0*60.0
				+" at "+ac1Coll+" and "+ac2Coll+" at distance of "+Point2D.distance(ac1Coll.x, ac1Coll.y, ac2Coll.x, ac2Coll.y)); 
			final long startTime = System.currentTimeMillis();
			while (true)
				if (System.currentTimeMillis() - timeCounter > 130) {
					this.timeCounter = System.currentTimeMillis();
					final long timediff = this.timeCounter - startTime;
					this.secondCounter = (timediff/1000.00);
					ac1.loc.x = ac1.startX + ac1.veloX() / 60.0 / 60.0 * timeAccelerator * (timediff/1000.00);
					ac1.loc.y = ac1.startY + ac1.veloY() / 60.0 / 60.0 * timeAccelerator * (timediff/1000.00);
					ac2.loc.x = ac2.startX + ac2.veloX() / 60.0 / 60.0 * timeAccelerator * (timediff/1000.00);
					ac2.loc.y = ac2.startY + ac2.veloY() / 60.0 / 60.0 * timeAccelerator * (timediff/1000.00);					
					this.distPx = Point2D.distance(ac1.loc.x, ac1.loc.y, ac2.loc.x, ac2.loc.y);
					this.isColliding = distPx*milesPerPixel < collisionMilesLimit;
					this.isStateChanged = true;
					repaint();
				} else
					try { 
						this.isStateChanged = false; 
						Thread.sleep(100); 
					} catch (InterruptedException ie) { /* Interruptions can just continue */ }
		}	

		private Point2D.Double aircraftPointAtTime(Aircraft ac, double time) {
			return new Point2D.Double(ac.loc.x + ac.veloX()*time, ac.loc.y + ac.veloY()*time);
		}

		private void setupBackBuffer() {			
			backBuffer = this.createImage(getWidth(), getHeight());
			backBufferGraphics = backBuffer.getGraphics();
			backBufferGraphics.setFont(font);
			Map<?, ?> desktopHints = 
				(Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
			Graphics2D g2d = (Graphics2D) backBufferGraphics;
			if (desktopHints != null) {
				g2d.setRenderingHints(desktopHints);
			}			
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
			showStatus(backBufferGraphics);

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
				ac1g.setColor(ac1.color);
				ac1g.rotate(Math.toRadians(ac1.heading), 5, 5);
				ac1g.fillPolygon(new int[] { 0, 0, 10, 0 },  new int[] { 0, 10, 5, 0 }, 4);	
				ac1g.dispose();
			}
			g.drawImage(ac1Image, (int) ac1.loc.x, (int) ac1.loc.y, this);
			g.setColor(ac1.color);
			g.drawLine(((int)ac1.loc.x)+5, ((int)ac1.loc.y)+5, ((int)ac1.loc.x)+5+((int)ac1.veloX()) / 10, 
				(int)ac1.loc.y+5+((int)ac1.veloY()) / 10);				
			if (this.ac2Image == null) {
				this.ac2Image = this.createImage(10,10);
				final Graphics2D ac2g = (Graphics2D) this.ac2Image.getGraphics();	
				ac2g.setColor(ac2.color);
				ac2g.rotate(Math.toRadians(ac2.heading), 5, 5);
				ac2g.fillPolygon(new int[] { 0, 0, 10, 0 },  new int[] { 0, 10, 5, 0 }, 4);		
				ac2g.dispose();
			}
			g.drawImage(ac2Image, (int) ac2.loc.x, (int) ac2.loc.y, this);
			g.setColor(ac2.color);
			g.drawLine(((int)ac2.loc.x)+5, ((int)ac2.loc.y)+5, ((int)ac2.loc.x)+5+((int)ac2.veloX()) / 10, 
				(int)ac2.loc.y+5+((int)ac2.veloY()) / 10);
		}
			
		private void drawGridlines(Graphics g) {
			if (this.isColliding)
				g.setColor(Color.RED);
			else
				g.setColor(Color.BLACK);
			for (int x = 0; x < getWidth(); x = x + cellSize)
				g.drawLine(x, 0, x, getHeight());
			for (int y = 0; y < getHeight(); y = y + cellSize)
				g.drawLine(0, y, getWidth(), y);
		}		

		public void showStatus(Graphics g) {
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(0, getHeight()-15, getWidth(), getHeight());
			g.setColor(Color.BLACK);
			final String msg = String.format(
				"%.1fs (%.0fs sim) - green: %.1f,%.1f \u2192 red: %.1f,%.1f = %.1fpx (%.1fnmi) %s",
				secondCounter, secondCounter*timeAccelerator, ac1.loc.x, ac1.loc.y, ac2.loc.x, ac2.loc.y, 
				distPx, distPx*milesPerPixel, isColliding ? "COLLIDING" : "");
			g.drawString(msg, 0, getHeight()-2);
			System.out.println(msg);
		}
		
		private double calculatedCollisionTime(Aircraft ac1, Aircraft ac2) {
			final double a = (ac2.loc.x - ac1.loc.x) * milesPerPixel;
			final double b = ac2.veloX() - ac1.veloX();
			final double c = (ac2.loc.y - ac1.loc.y) * milesPerPixel;
			final double d = ac2.veloY() - ac1.veloY();
			
			return - ((a*b + c*d) / (b*b + d*d));
		}
	}
}