package example;

import lib.FaceDb;
import lib.FacialRecognition;
import lib.FacialRecognition.PotentialFace;
import lib.WebCam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;

public class FaceApplet extends JApplet implements Runnable, MouseListener {

  private final WebCam cam = new WebCam();

  private BufferedImage currentFace = null;

  private boolean running = true;
  private boolean dialog = false;

  @Override
  public void init() {
    cam.start();
    setSize(1600, 900);
    addMouseListener(this);
  }

  @Override
  public void start() {
    new Thread(this).start();
  }

  public void run() {
    while (running) {
      repaint();
    }
  }

  @Override
  public void update(Graphics g) {
    paint(g);
  }

  @Override
  public void paint(Graphics g) {
    final BufferedImage image = cam.capture();
    if (image == null || dialog) {
      return;
    }
    drawFaces(image);
    g.drawImage(image, 0, 0, null);
  }

  public void drawFaces(BufferedImage image) {
    final List<PotentialFace> faces = FacialRecognition.run(image, db);
    if (faces.isEmpty()) {
      return;
    }
    Graphics2D g2 = image.createGraphics();
    g2.setStroke(new BasicStroke(2));
    for (PotentialFace face : faces) {
      final Rectangle r = face.box;
      final Color c;
      final String msg;
      if (face.name == null) {
        c = Color.RED;
        msg = "Click to tag";
      } else {
        c = new Color(face.name.hashCode());
        msg = String.format("%s: %f", face.name, face.confidence);
      }
      g2.setColor(c);
      g2.drawRect(r.x, r.y, r.width, r.height);
      g2.drawString(msg, r.x + 5, r.y - 5);
    }
  }

  @Override
  public void stop() {
    running = false;
  }

  @Override
  public void destroy() {
    cam.stop();
  }

  private final FaceDb db = new FaceDb();

  @Override
  public void mouseClicked(MouseEvent mouseEvent) {
    if (currentFace == null) {
      System.out.println("No face in frame");
      return;
    }
    dialog = true;
    final String name = (String) JOptionPane.showInputDialog(null, "Save as?", "Tag", JOptionPane.QUESTION_MESSAGE, new ImageIcon(currentFace, "Preview"), null, null);
    dialog = false;
    if (name != null) {
      System.out.println("Saving " + name + "...");
      db.add(name, currentFace);
    }
  }

  @Override
  public void mousePressed(MouseEvent mouseEvent) {
  }

  @Override
  public void mouseReleased(MouseEvent mouseEvent) {
  }

  @Override
  public void mouseEntered(MouseEvent mouseEvent) {
  }

  @Override
  public void mouseExited(MouseEvent mouseEvent) {
  }
}
