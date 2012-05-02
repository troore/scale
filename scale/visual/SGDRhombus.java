package scale.visual;

import java.util.Enumeration;
import java.awt.*;
import java.awt.geom.*;

/**
 * This class represents diamond-shaped geometric display element.
 * <p>
 * $Id: SGDRhombus.java,v 1.3 2007-10-04 19:58:41 burrill Exp $
 * <p>
 * Copyright 2007 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * Because this class implements the <code>Shape</code> interface it
 * can be passed directly to the <code>draw()</code> method of the
 * <code>Graphics</code>class.  An instance of this class is its own
 * path iterator instance.
 */
public abstract class SGDRhombus extends RectangularShape implements PathIterator
{
  private int             state;
  private AffineTransform at;

  public SGDRhombus()
  {
  }

  public abstract double getX();
  public abstract double getY();
  public abstract double getWidth();
  public abstract double getHeight();
  public abstract void   setFrame(double x, double y, double w, double h);


  /**
   * A rhombus using double precision floating point coordinates.
   */
  public static final class Double extends SGDRhombus
  {
    private double x;
    private double y;
    private double w;
    private double h;

    public Double()
    {
    }

    public double getX()
    {
      return x;
    }

    public double getY()
    {
      return y;
    }

    public double getWidth()
    {
      return w;
    }

    public double getHeight()
    {
      return h;
    }

    public void setFrame(double x, double y, double w, double h)
    {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }

    public boolean isEmpty()
    {
      return (w <= 0.0) && (h <= 0.0);
    }
  }

  /**
   * A rhombus using single precision floating point coordinates.
   */
  public static final class Float extends SGDRhombus
  {
    private float x;
    private float y;
    private float w;
    private float h;

    public Float()
    {
    }

    public double getX()
    {
      return x;
    }

    public double getY()
    {
      return y;
    }

    public double getWidth()
    {
      return w;
    }

    public double getHeight()
    {
      return h;
    }

    public void setFrame(double x, double y, double w, double h)
    {
      this.x = (float) x;
      this.y = (float) y;
      this.w = (float) w;
      this.h = (float) h;
    }

    public boolean isEmpty()
    {
      return (w <= 0.0) && (h <= 0.0);
    }
  }

  private boolean intersects(double x1,
                             double y1,
                             double x2,
                             double y2,
                             double x,
                             double y,
                             double w,
                             double h)
  {
    return
      Line2D.linesIntersect(x1, y1, x2, y2, x    , y    , x + w, y    ) ||
      Line2D.linesIntersect(x1, y1, x2, y2, x    , y    , x    , y + h) ||
      Line2D.linesIntersect(x1, y1, x2, y2, x + w, y    , x + w, y + h) ||
      Line2D.linesIntersect(x1, y1, x2, y2, x    , y + h, x + w, y + h);
  }

  public boolean intersects(double x, double y, double w, double h)
  {
    double wd = getWidth();
    double ht = getHeight();
    double x1 = getX();
    double y1 = getY() + ht / 2.0;
    double x2 = x1 + wd / 2.0;
    double y2 = getY();
    double x3 = x1 + wd;
    double y3 = y1;
    double x4 = x1 + wd / 2.0;
    double y4 = y2 + ht;
    return
      intersects(x1, y1, x2, y2, x, y, w, h) ||
      intersects(x2, y2, x3, y3, x, y, w, h) ||
      intersects(x3, y3, x4, y4, x, y, w, h) ||
      intersects(x4, y4, x1, y1, x, y, w, h);
  }

  public boolean intersects(double x1, double y1, double x2, double y2, Rectangle2D r)
  {
    return r.intersectsLine(x1, y1, x2, y2);
  }

  public boolean intersects(Rectangle2D r)
  {
    double w  = getWidth();
    double h  = getHeight();
    double x1 = getX();
    double y1 = getY() + h / 2.0;
    double x2 = x1 + w / 2.0;
    double y2 = getY();
    double x3 = x1 + w;
    double y3 = y1;
    double x4 = x1 + w / 2.0;
    double y4 = y2 + h;
    return
      intersects(x1, y1, x2, y2, r) ||
      intersects(x2, y2, x3, y3, r) ||
      intersects(x3, y3, x4, y4, r) ||
      intersects(x4, y4, x1, y1, r);
  }

  public boolean contains(double x, double y, double w, double h)
  {
    return contains(x, y) && (w <= 1.0) && (y <= 1.0);
  }

  public boolean contains(Rectangle2D r)
  {
    return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
  }

  private boolean gte(double x1, double y1, double x2, double y2, double x, double y)
  {
    if (x2 == x1)
      return (x == x1) && (y == y1);

    double d  =(y - (x - x1) * (y2 - y1) / (x2 - x1));
    return  (d >= 0.0);
  }

  public boolean contains(double x, double y)
  {
    double w  = getWidth();
    double h  = getHeight();
    double x1 = getX();
    double y1 = getY() + h / 2.0;
    double x2 = x1 + w / 2.0;
    double y2 = getY();
    double x3 = x1 + w;
    double y3 = y1;
    double x4 = x1 + w / 2.0;
    double y4 = y2 + h;
    return
      gte(x1, y1, x2, y2, x, y) &&
      gte(x2, y2, x3, y3, x, y) &&
      gte(x4, y4, x3, y3, x, y) &&
      gte(x1, y1, x4, y4, x, y);
  }

  public Rectangle2D getBounds2D()
  {
    double x = getX();
    double y = getY();
    double w = getWidth();
    double h = getHeight();
    return new Rectangle2D.Double(x, y, x + w, y + h);
  }

  public PathIterator getPathIterator(AffineTransform at)
  {
    this.at = at;
    this.state = 0;
    return this;
  }

  public PathIterator getPathIterator(AffineTransform at, double flatness)
  {
    this.at = at;
    this.state = 0;
    return this;
  }

  public void next()
  {
    state++;
    if (state > 5)
      state = 5;
  }

  public boolean isDone()
  {
    return state >= 5;
  }

  public int getWindingRule()
  {
    return WIND_EVEN_ODD;
  }

  public int currentSegment(double[] coords)
  {
    switch (state) {
    case 0: {
      coords[0] = getX();
      coords[1] = getY() + getHeight() / 2.0;
      if (at != null)
        at.transform(coords, 0, coords, 0, 2);
      return SEG_MOVETO;
    }
    case 1: {
      coords[0] = getX() + getWidth() / 2.0;
      coords[1] = getY();
        if (at != null)
          at.transform(coords, 0, coords, 0, 2);
        return SEG_LINETO;
      }
    case 2: {
      coords[0] = getX() + getWidth();
      coords[1] = getY() + getHeight() / 2.0;
        if (at != null)
          at.transform(coords, 0, coords, 0, 2);
        return SEG_LINETO;
      }
    case 3: {
      coords[0] = getX() + getWidth() / 2.0;
      coords[1] = getY() + getHeight();
      if (at != null)
        at.transform(coords, 0, coords, 0, 2);
      return SEG_LINETO;
    }
    case 4: {
      return SEG_CLOSE;
    }
    }
    return SEG_LINETO;
  }

  public int currentSegment(float[] coords)
  {
    switch (state) {
    case 0: {
      coords[0] = (float) getX();
      coords[1] = (float) (getY() + getHeight() / 2.0);
      if (at != null)
        at.transform(coords, 0, coords, 0, 2);
      return SEG_MOVETO;
    }
    case 1: {
      coords[0] = (float) (getX() + getWidth() / 2.0);
      coords[1] = (float) getY();
        if (at != null)
          at.transform(coords, 0, coords, 0, 2);
        return SEG_LINETO;
      }
    case 2: {
      coords[0] = (float) (getX() + getWidth());
      coords[1] = (float) (getY() + getHeight() / 2.0);
        if (at != null)
          at.transform(coords, 0, coords, 0, 2);
        return SEG_LINETO;
      }
    case 3: {
      coords[0] = (float) (getX() + getWidth() / 2.0);
      coords[1] = (float) (getY() + getHeight());
      if (at != null)
        at.transform(coords, 0, coords, 0, 2);
      return SEG_LINETO;
    }
    case 4: {
      return SEG_CLOSE;
    }
    }
    return SEG_LINETO;
  }
}
