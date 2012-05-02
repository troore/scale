package scale.backend;

/** 
 * This class represents a displacement field in an instruction when 
 * the displacement refers to an offset that must be relocated by the loader.
 * <p>
 * $Id: SymbolDisplacement.java,v 1.25 2007-09-20 18:57:40 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */

public class SymbolDisplacement extends Displacement
{
  private static int sequence = 0; // Each symbol displacement has a unique id.

  /**
   * Return the relocation sequence number.
   */
  public static int used()
  {
    return sequence;
  }

  private int     handle;       // Index to the variable.
  private int     id;           // The sequence number of the Displacement.
  private String  name;         // The symbol's name.
  private boolean addressTaken; // If this displacement may have an alias.
  

  /**
   * Create a displacement that the loader can relocate.
   * @param name is the symbol's name
   * @param handle is information associated with the displacement for
   * use by the code generator.
   */
  public SymbolDisplacement(String name, int handle)
  {
    this(name, handle, false);
  }
  
  public SymbolDisplacement(String name, int handle, boolean addressTaken)
  {
    super();
    this.handle       = handle;
    this.name         = name;
    this.id           = sequence++;
    this.addressTaken = addressTaken;
  }

  /**
   * Return a unique displacement.  For the Alpha, each symbol
   * reference must have a unique identification number.
   */
  public Displacement unique()
  {
    return new SymbolDisplacement(name, handle, addressTaken);
  }

  /**
   * Set that the address of this symbol has been taken.
   * In other words, it may be aliased.
   */
  public void setAddressTaken(boolean addressTaken)
  {
    this.addressTaken = addressTaken;
  }
  
  /**
   * Return true if the address of this symbol was taken.
   */
  public boolean addressTaken()
  {
    return addressTaken;
  }
  
  /**
   * Reset the relocation information for each generated assembly
   * language file.
   */
  public static void reset()
  {
    sequence = 0;
  }

  /**
   * Return the relocation sequence number.
   */
  public int getSequence()
  {
    return id;
  }

  /**
   * Return the symbolic name.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Return the data area handle associated with the displacement.
   */
  public int getHandle()
  {
    return handle;
  }

  /**
   * Return true if the displacement is from a symbol.
   */
  public boolean isSymbol()
  {
    return true;
  }
  
  /**
   * Generate a String representation that can be used by the
   * assembly code generater.
   */
  public String assembler(Assembler asm)
  {
    return name;
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer("(sym ");
    buf.append(name);
    buf.append(' ');
    buf.append(id);
    buf.append(')');
    return buf.toString();
  }

  /**
   * Return true if the displacements are equivalent.
   */
  public boolean equivalent(Object o)
  {
    return false;
  }
}
