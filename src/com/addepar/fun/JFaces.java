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

  private BufferedImage currentFace = null;

  private boolean running = true;

  @Override public void init() {
    cam.start();
    setSize(1600, 900);
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
    final BufferedImage image = cam.run();
    if (image == null) {
      return;
    }
    drawFaces(g, image);
    g.drawImage(image, 0, 0, null);
  }

  public void drawFaces(Graphics g, BufferedImage image) {
    final List<Rectangle> faces = FaceDetector.detectFaces(image);
    if (faces.isEmpty()) {
      return;
    }
    Graphics2D g2 = image.createGraphics();
    g2.setColor(Color.RED);
    g2.setStroke(new BasicStroke(2));
    for (Rectangle r : faces) {
      g2.drawRect(r.x, r.y, r.width, r.height);
    }

    if (faces.size() == 1) {
      Rectangle r = faces.get(0);
      if (currentFace != null) {
        g.clearRect(image.getWidth(), 0, currentFace.getWidth(), currentFace.getHeight());
      }
      currentFace = image.getSubimage(r.x, r.y, r.width, r.height);
      g.drawImage(currentFace, image.getWidth(), 0, null);
    }
  }


  @Override public void stop() {
    running = false;
  }

  @Override public void destroy() {
    cam.stop();
  }
}
