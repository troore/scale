package scale.common;

import java.awt.Color;

/**
 * This enum specifies graphical display colors - red,
 * blue, etc.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public enum DColor
{
  WHITE       (0xFFFFFF, "white"),
  BLUE        (0x0000FF, "blue"),
  RED         (0xFF0000, "red"),
  GREEN       (0x00FF00, "green"),

  YELLOW      (0xFFFF00, "yellow"),
  MAGENTA     (0xFF00FF, "magenta"),
  CYAN        (0x00FFFF, "cyan"),
  DARKGREY    (0xA9A9A9, "darkgrey"),

  DARKBLUE    (0x00008B, "darkblue"),
  DARKRED     (0x8B0000, "darkred"),
  DARKGREEN   (0x006400, "darkgreen"),
  DARKYELLOW  (0x8B8B00, "darkyellow"),

  DARKMAGENTA (0x8B008B, "darkmagenta"),
  DARKCYAN    (0x008B8B, "darkcyan"),
  GOLD        (0xFFD700, "gold"),
  LIGHTGREY   (0xD3D3D3, "lightgrey"),

  LIGHTBLUE   (0xADD8E6, "lightblue"),
  LIGHTRED    (0xFF8470, "lightred"),
  LIGHTGREEN  (0x90EE90, "lightgreen"),
  LIGHTYELLOW (0xFFFFE0, "lightyellow"),

  LIGHTMAGENTA(0xFF00FF, "lightmagenta"),
  LIGHTCYAN   (0xE0FFFF, "lightcyan"),
  LILAC       (0xE6E6FA, "lilac"),
  TURQUOISE   (0x40E0D0, "turquoise"),

  AQUAMARINE  (0x7FFFD4, "aquamarine"),
  KHAKI       (0xF0E68C, "khaki"),
  PURPLE      (0x800080, "purple"),
  YELLOWGREEN (0x9ACD32, "yellowgreen"),

  PINK        (0xFFC0CB, "pink"),
  ORANGE      (0xFFA500, "orange"),
  ORCHID      (0xDA70D6, "orchid"),
  BLACK       (0x000000, "black"),
 
  BROWN       (0xA52A2A, "brown");

  /**
   * A 24-bit (red, green, blue) integer value indexed by the color
   * index.
   */
  private int    value;
  private String name;
  private Color  color;

  /**
   * @param valuse is a 24-bit (red, green, blue) integer value -
   * 0Xrrggbb - red, green, blue.
   */
  private DColor(int value, String name)
  {
    this.name  = name;
    this.value = value;
    this.color = new Color((value >> 16) & 0xff, (value >> 8) & 0xff, value &0xff);
  }

  public int value()
  {
    return value;
  }

  public int red()
  {
    return (value >> 16) & 0xFF;
  }

  public int green()
  {
    return (value >> 8) & 0xFF;
  }

  public int blue()
  {
    return value & 0xFF;
  }

  public String sName()
  {
    return name;
  }

  public Color color()
  {
    return color;
  }
}


