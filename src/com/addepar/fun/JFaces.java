package com.addepar.fun;

import com.addepar.fun.hack.FaceDetector;
import com.addepar.fun.hack.WebCam;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JApplet;

public class JFaces extends JApplet implements Runnable {

  private final WebCam cam = new WebCam();

  private BufferedImage image = null;
  private BufferedImage currentFace = null;
  private List<Rectangle> faces = null;
  private boolean running = true;

  @Override public void init() {
    cam.start();
    setSize(1600, 900);
  }

  @Override public void start() {
    new Thread(this).start();
  }

  public void run() {
    while (running && (image = cam.run()) != null) {
      if (faces == null) {
        faces = FaceDetector.detectFaces(image);
        repaint();
      }
    }
  }

  @Override public void update(Graphics g) {
    paint(g);
  }

  @Override public void paint(Graphics g) {
    if (image == null) {
      return;
    }
    Graphics2D g2 = image.createGraphics();
    if (faces != null) {
      g2.setColor(Color.RED);
      g2.setStroke(new BasicStroke(2));
      for (Rectangle r : faces) {
        if (currentFace != null) {
          g.clearRect(image.getWidth(), 0, currentFace.getWidth(), currentFace.getHeight());
        }
        currentFace = image.getSubimage(r.x, r.y, r.width, r.height);
        g.drawImage(currentFace, image.getWidth(), 0, null);
        g2.drawRect(r.x, r.y, r.width, r.height);
      }
      faces = null;
    }
    g.drawImage(image, 0, 0, null);
  }


  @Override public void stop() {
    running = false;
  }

  @Override public void destroy() {
    cam.stop();
  }
}
