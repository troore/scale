package scale.backend.x86;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the class for all machine X86 branch instructions.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by James H. Burrill<br>
 * All Rights Reserved.<br>
 */

public class X86Branch extends Branch
{
  /**
   * the instruction opcode
   */
  protected int opcode;
  /**
   * True branch predicted?
   */
  protected boolean pt;

  /**
   * @param numTargets is the number of successors of this instruction.
   * @param pt is true if the true condition is predicted
   * For routine calls, it does not include the routine called.
   */
  protected X86Branch(int opcode, boolean pt, int numTargets)
  {
    super(numTargets);
    this.opcode    = opcode;
    this.pt        = pt;

    assert checkForm(opcode) : "Invalid opcode for instruction form.";
  }

  protected boolean checkForm(int opcode)
  {
    return (0 != (opcode & Opcodes.F_NONE)) && (0 != (opcode & Opcodes.F_BRANCH));
  }

  public final int getOpcode()
  {
    return opcode;
  }

  protected final void setOpcode(int opcode)
  {
    this.opcode = opcode;
  }

  /**
   * Return true if the instruction modifies memory.
   */
  public final boolean isReversed()
  {
    return (opcode & Opcodes.F_REV) != 0;
  }

  /**
   * Return true if the branch is predicited to occur.
   */
  public boolean getPt()
  {
    return pt;
  }

  public Displacement getDisplacement()
  {
    return null;
  }

  public void setDisplacement(Displacement disp)
  {
    throw new scale.common.InternalError("Not allowed.");
  }

  public int getReg()
  {
    return -1;
  }

  public void setReg(int reg)
  {
    throw new scale.common.InternalError("Not allowed.");
  }


  public int getReg2()
  {
    return -1;
  }

  public void setReg2(int reg2)
  {
    throw new scale.common.InternalError("Not allowed.");
  }

  /**
   * Specifiy the size of the struct returned by the call or 0 if none.
   */
  protected void setReturnedStructSize(int size)
  {
  }

  /**
   * Specify the registers used by this instruction.
   * @param rs is the register set in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator rs, int index, int strength)
  {
    super.specifyRegisterUsage(rs, index, strength);
  }

  public void remapRegisters(int[] map)
  {
    super.remapRegisters(map);
  }

  /**
   * Map the registers used in the instruction as sources to the specified register.
   * If the register is not used as a source register, no change is made.
   * @param oldReg is the previous source register
   * @param newReg is the new source register
   */
  public void remapSrcRegister(int oldReg, int newReg)
  {
    super.remapSrcRegister(oldReg, newReg);
  }

  /**
   * Map the registers defined in the instruction as destinations to the specified register.
   * If the register is not used as a destination register, no change is made.
   * @param oldReg is the previous destination register
   * @param newReg is the new destination register
   */
  public void remapDestRegister(int oldReg, int newReg)
  {
    super.remapDestRegister(oldReg, newReg);
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    if (super.uses(register, registers))
      return true;
    return false;
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    if (super.defs(register, registers))
      return true;
    return false;
  }

  /**
   * Return true if the instruction clobbers the register.
   */
  public boolean mods(int register, RegisterSet registers)
  {
    return super.mods(register, registers);
  }

  public void assembler(Assembler asm, Emit emit)
  {
    throw new scale.common.NotImplementedError("");
  }

  /**
   * Generate a String representation of a Displacement that can be used by the
   * assembly code generater.
   */
  public String assembleDisp(Assembler asm, Displacement disp, int ftn)
  {
    throw new scale.common.NotImplementedError("");
//     if (ftn == X86Generator.FT_NONE)
//       return disp.assembler(asm);

//     StringBuffer buf = new StringBuffer(X86Generator.ftns[ftn]);
//     buf.append('(');
//     buf.append(disp.assembler(asm));
//     buf.append(')');
//     return buf.toString();
  }

  /**
   * Return true if the instruction can be deleted without changing program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    return nullified();
  }

  /**
   * @return the number of bytes required for the Branch Instruction
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Return true if the branch is an unconditional transfer of control to a new address.
   */
  public boolean isUnconditional()
  {
    return true;
  }

  /**
   * Set the scale factor specified for the instruction.  The value
   * must be 1, 2, 4, or 8.
   */
  public void setScale(int scale)
  {
    opcode = Opcodes.setScale(opcode, scale);
  }

  /**
   * Return 1, 2, 4, or 8 depending on the size of the operand, in
   * bytes, specified for the instruction.
   */
  public int getOperandSize()
  {
    return Opcodes.getOperandSize(opcode);
  }

  /**
   * Return 'b', 'w', 'l', or 'x' depending on the size of the
   * operand specified for the instruction.
   */
  public char getOperandSizeLabel()
  {
    return Opcodes.getOperandSizeLabel(opcode);
  }

  /**
   * Set the operand size specified for the instruction.  The value
   * must be 1, 2, 4, or 8.
   */
  public void setOperandSize(int size)
  {
    opcode = Opcodes.setOperandSize(opcode, size);;
  }

  public String toString()
  {
    return Opcodes.getOp(this);
  }

  public static void buildAddress(StringBuffer buf, int baseReg, int indexReg, Displacement disp, int scale)
  {
    if (disp != null) {
      if (disp.isNumeric()) {
        long offset = disp.getDisplacement();
        if (offset != 0)
          buf.append(offset);
      } else
        buf.append(disp);
    }

    if (baseReg != -1) {
      buf.append("(%");
      buf.append(baseReg);
      if (indexReg != -1) {
        buf.append('+');
        if (scale != 1) {
          buf.append(scale);
          buf.append('*');
        }
        buf.append('%');
        buf.append(indexReg);
      }
      buf.append(')');
    }
  }
}
