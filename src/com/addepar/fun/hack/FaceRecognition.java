package com.addepar.fun.hack;


import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import com.google.common.collect.Lists;

public class FaceRecognition {

  private final BufferedImage src;
  private final List<PotentialFace> faces = Lists.newArrayList();

  public FaceRecognition(BufferedImage src) {
    this.src = src;
  }

  public List<PotentialFace> getMatches() {
    return faces;
  }

  protected void add(Rectangle r, String name, double confidence) {
    faces.add(new PotentialFace(r, name, confidence));
  }

  public static class PotentialFace {
    private final Rectangle r;
    private final String name;
    private final double confidence;  // confidence that face at r is name

    public PotentialFace(Rectangle r, String name, double confidence) {
      this.r = r;
      this.name = name;
      this.confidence = confidence;
    }

    @Override
    public String toString() {
      return String.format("%s found at (%d,%d) with confidence = %s", name, r.x, r.y, confidence);
    }
  }
}
