package com.addepar.fun.hack;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public interface FaceAPI {

  /**
   * Saves given pic with given name and returns unique id of the pic
   */
  public String save(String name, BufferedImage pic);

  /**
   * Returns a map of name to probability of the pic being that name
   */
  public Map<String, Double> recognize(BufferedImage pic);

  /**
   * Get list of saved pics for name
   * pic.id is the id of the image (useful for sending a delete request)
   */
  public List<MugShot> get(String name);

  /**
   * returns true iff successfully deletes pic with given id
   */
  public boolean delete(String id);
}
