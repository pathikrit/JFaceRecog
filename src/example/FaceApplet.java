package example;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JApplet;
import javax.swing.JOptionPane;

import lib.FaceDb;
import lib.FacialRecognition;
import lib.FacialRecognition.PotentialFace;
import lib.WebCam;

public class FaceApplet extends JApplet implements Runnable, MouseListener {

  private final WebCam cam = new WebCam();

  private BufferedImage currentFace = null;

  private boolean running = true;
  private boolean dialog = false;


  @Override public void init() {
    cam.start();
    setSize(1600, 900);
    addMouseListener(this);
  }

  @Override public void start() {
    new Thread(this).start();
  }

  public void run() {
    while (running) {
      repaint();
    }
  }

  @Override public void update(Graphics g) {
    paint(g);
  }

  @Override public void paint(Graphics g) {
    final BufferedImage image = cam.capture();
    if (image == null || dialog) {
      return;
    }
    drawFaces(g, image);
    g.drawImage(image, 0, 0, null);
  }

  public void drawFaces(Graphics g, BufferedImage image) {
    final List<PotentialFace> faces = FacialRecognition.run(image, db);
    if (faces.isEmpty()) {
      return;
    }
    Graphics2D g2 = image.createGraphics();
    g2.setColor(Color.RED);
    g2.setStroke(new BasicStroke(2));
    if (faces.size() == 1) {
      Rectangle r = faces.get(0).box;
      if (currentFace != null) {
        g.clearRect(image.getWidth(), 0, currentFace.getWidth(), currentFace.getHeight());
      }
      currentFace = image.getSubimage(r.x, r.y, r.width, r.height);
      g.drawImage(currentFace, image.getWidth(), 0, null);
    }
    for (PotentialFace face : faces) {
      final Rectangle r = face.box;
      g2.drawRect(r.x, r.y, r.width, r.height);
      if (face.name != null) {
        final String stats = String.format("%s: %f", face.name, face.confidence);
        g2.drawString(stats, r.x+5, r.y-5);
      }
    }
  }


  @Override public void stop() {
    running = false;
  }

  @Override public void destroy() {
    cam.stop();
  }

  private final FaceDb db = new FaceDb();


  @Override
  public void mouseClicked(MouseEvent mouseEvent) {
    if (currentFace != null) {
      dialog = true;
      final String name = JOptionPane.showInputDialog(null, "Save as?");
      dialog = false;
      db.add(name, currentFace);
      System.out.println("Saving " + name + "...");
    } else {
      System.out.println("No face in frame");
    }
  }

  @Override
  public void mousePressed(MouseEvent mouseEvent) {}

  @Override
  public void mouseReleased(MouseEvent mouseEvent) {}

  @Override
  public void mouseEntered(MouseEvent mouseEvent) {}

  @Override
  public void mouseExited(MouseEvent mouseEvent) {}
}
