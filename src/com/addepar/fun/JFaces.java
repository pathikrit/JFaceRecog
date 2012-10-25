package com.addepar.fun;

import com.addepar.fun.hack.FaceDetector;

import java.applet.Applet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JApplet;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;

import static com.googlecode.javacv.cpp.opencv_core.IplImage;

public class JFaces extends JApplet implements Runnable {

  private FrameGrabber grabber = null;
  private IplImage grabbedImage = null;
  private boolean stop = false;
  private BufferedImage currentFace = null;
  private List<Rectangle> faces = null;

  @Override public void init() {
    setSize(1600, 900);
  }

  @Override public void start() {
    new Thread(this).start();
  }

  public void run() {
    try {
      int CAMERA_NUMBER = 0;
      try {
        grabber = FrameGrabber.createDefault(CAMERA_NUMBER);
      } catch (Exception e) {
        if (grabber != null) grabber.release();
        grabber = new OpenCVFrameGrabber(CAMERA_NUMBER);
      }

      grabber.start();
      grabbedImage = grabber.grab();

      stop = false;
      while (!stop && (grabbedImage = grabber.grab()) != null) {
        if (faces == null) {
          faces = FaceDetector.detectFaces(grabbedImage);
          repaint();
        }
      }
      grabbedImage = null;
      grabber.stop();
      grabber.release();
      grabber = null;
    } catch (Exception e) {
     e.printStackTrace();
    }
  }

  @Override public void update(Graphics g) {
    paint(g);
  }

  @Override public void paint(Graphics g) {
    if (grabbedImage == null) {
      return;
    }
    BufferedImage image = grabbedImage.getBufferedImage(2.2 / grabber.getGamma());
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
    stop = true;
  }

  @Override public void destroy() {
    try {
      ImageIO.write(currentFace, "png", new File("capture.png"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public double getMatchProbability() {
    return 90 + 10*Math.random();
  }
}
