package scale.backend.xyz;

import scale.backend.*;
import scale.clef.type.Type;
import scale.clef.type.AggregateType;

/** 
 * This class describes the register set of the Xyz architecture.
 * <p>
 * $Id: XyzRegisterSet.java,v 1.1 2006-11-16 17:28:19 burrill Exp $
 * <p>
 * Copyright 2005 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * This class represents the register set for the specific
 * architecture.  Most of the methods below generate a "not
 * implemented error".  These must be replaced with code that is
 * specific to the register set of the architecture.  The other
 * methods may need to be modified.  Use the register set classes from
 * the other architectures to help you understand how to make changes
 * for your architecture.
 * @see scale.backend.RegisterSet
 * @see scale.backend.alpha.AlphaRegisterSet
 * @see scale.backend.sparc.SparcRegisterSet
 * @see scale.backend.ppc.PPCRegisterSet
 */

public class XyzRegisterSet extends scale.backend.RegisterSet
{
  public static final int SP_REG = 0;

  private static final short[] actualRegisters = { };

  private static final short[] preferredOrder = { };

  private static final short[] calleeUses = { };

  private static final short[] calleeSaves = { };

  private static final String[] regNames = { };

  public XyzRegisterSet()
  {
    super(actualRegisters);
  }

  public String registerName(int reg)
  {
    if (reg < actualRegisters.length)
      return regNames[reg];

    if (floatRegister(reg))
      return "$fv" + reg;

    return "$v" + reg;
  }

  public short[] getPreferredOrder()
  {
    return preferredOrder;
  }

  public boolean useContiguous()
  {
    return false;
  }

  public int registerSize(int reg)
  {
    return ((getType(reg) & sizeMask) + 1) * 8;
  }

  public short[] getCalleeSaves()
  {
    return calleeSaves;
  }

  public short[] getCalleeUses()
  {
    return calleeUses;
  }

  public int numAllocatableRegisters()
  {
    throw new scale.common.NotImplementedError("numAllocatableRegisters");
  }

  public int tempRegisterType(Type type, long bs)
  {
    throw new scale.common.NotImplementedError("tempRegisterType");
  }
}
