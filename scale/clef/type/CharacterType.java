package scale.clef.type;

import java.util.Enumeration;

import scale.common.*;
import scale.clef.*;

/**
 * This class represents the character type for languages other than C.
 * <p>
 * $Id: CharacterType.java,v 1.47 2007-08-27 18:13:31 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * An {@link scale.clef.type.IntegerType IntegerType} is used for the
 * C <code>char</code> type.
 * <p>
 * Ideally, the source language should not specify a particular character
 * format.  However, many C programs rely on the ANSI character set, and
 * Java prescribes the use of Unicode.  Hence, user code may use these
 * attributes to specify the format (and implicitly the size) of the
 * character set.  If the language does not require a particular format
 * it may specify <tt>Any</tt>.  The generation interface assumes a default
 * of <tt>Any</tt>.
 */

public class CharacterType extends NumericType 
{
  private static Vector<CharacterType> types; // A list of all the unique character types.

  /**
   * Character representation is unknown.
   */
  public static final int cAny = 0;
  /**
   * Character representation is 8-bit ASCII.
   */
  public static final int cAnsi = 1;
  /**
   * Character representation is 8-bit EBCDIC.
   */
  public static final int cEbcdic = 2;
  /**
   * Character representation is 16-bit unicode.
   */
  public static final int cUnicode = 3;

  /**
   * Map from character type to a string representing that type.
   */
  public static final String[] formatMap = {"<?>", "<A>", "<E>", "<U>"};

  /**
   * The character format.
   */
  private int format;

  /**
   * Re-use an existing instance of a particular character type.
   * If no equivalent character type exists, create a new one.
   * @param format the type of character format
   */

  public static CharacterType create(int format)
  {
    if (types != null) {
      int n = types.size();
      for (int i = 0; i < n; i++) {
        CharacterType ta = types.elementAt(i);
        if (ta.format == format) {
          return ta;
        }
      }
    }
    CharacterType a = new CharacterType(format);
    return a;
  }

  private CharacterType(int format)
  {
    super();
    this.format = format;
    if (types == null)
      types = new Vector<CharacterType>(2);
    types.addElement(this);
  }

  /**
   * Return true if type represents a character type.
   */
  public final boolean isCharacterType()
  {
    return true;
  }

  public final CharacterType returnCharacterType()
  {
    return this;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("<");
    buf.append("char");
    buf.append(formatMap[format]);
    buf.append('>');
    return buf.toString();
  }

  public void visit(Predicate p)
  {
    p.visitCharacterType(this);
  }

  public void visit(TypePredicate p)
  {
    p.visitCharacterType(this);
  }

  public final int getFormat()
  {
    return format;
  }

  /**
   * Return true if the types are equivalent.
   */
  public boolean equivalent(Type t)
  {
    Type tc = t.getEquivalentType();
    if (tc == null)
      return false;

    if (tc.getClass() != getClass())
      return false;

    CharacterType t2 = (CharacterType) tc;
    return (format == t2.format);
  }

  /**
   * Return an enumeration of all the different types.
   */
  public static Enumeration<CharacterType> getTypes()
  {
    if (types == null)
      return new EmptyEnumeration<CharacterType>();
    return types.elements();
  }

  public int bitSize()
  {
    if (format == cUnicode)
      return 16;
    return 8; 
  }

  /**
   * Map a type to a C string. The string representation is
   * based upon the size of the type.
   * @return the string representation of the type
   */
  public String mapTypeToCString()
  {
    int cf = getFormat();
    if (cf == CharacterType.cUnicode) {
      return "unsigned short";
    } else
      return "char";
  }

  /**
   * Remove static lists of types.
   */
  public static void cleanup()
  {
    types = null;
  }
}
