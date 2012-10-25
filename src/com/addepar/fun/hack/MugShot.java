package com.addepar.fun.hack;

import java.awt.image.BufferedImage;
import java.util.UUID;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class MugShot {
  private final String id = UUID.randomUUID().toString();
  private final BufferedImage image;

  public MugShot(BufferedImage image) {
    this.image = image;
  }

  public BufferedImage getBuffer() {
    return image;
  }

  public String getId() {
    return id;
  }

  private final int X = 100, Y = 100;
  private BufferedImage scale(BufferedImage image) {
    return null;
  }

}
