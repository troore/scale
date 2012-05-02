package scale.backend.x86;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all X86 instructions except branches.
 * <p>
 * $Id$
 * <p>
 * Copyright 2008 by James H. Burrill<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class X86Instruction extends Instruction
{
  /**
   * The instruction opcode.
   */
  protected int opcode;

  /**
   * flags that specify the condition codes set by the instruction
   */
  protected byte setCC;
  /**
   * flags that specify the condition codes used by the instruction
   */
  protected byte useCC;

  /**
   * @param opcode is the instruction's opcode
   */
  public X86Instruction(int opcode)
  {
    this.opcode = opcode;
    this.setCC  = 0;
    this.useCC  = 0;

    assert checkForm(opcode) : "Invalid opcode for instruction form.";
  }

  /**
   * Return true if the specified form is valid for this instruction
   * opcode.
   */
  protected boolean checkForm(int opcode)
  {
    return ((0 != (opcode & Opcodes.F_NONE)) &&
            (0 == (opcode & Opcodes.F_BRANCH)));
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

  public Displacement getDisplacement()
  {
    return null;
  }

  public void setDisplacement(Displacement disp)
  {
    throw new scale.common.InternalError("Not allowed.");
  }

  public Displacement getDisplacement2()
  {
    return null;
  }

  public void setDisplacement2(Displacement disp)
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

  public int getReg3()
  {
    return -1;
  }

  public void setReg3(int reg3)
  {
    throw new scale.common.InternalError("Not allowed.");
  }

  /**
   * Return true if the instruction uses the register.
   */
  public boolean uses(int register, RegisterSet registers)
  {
    return false;
  }

  /**
   * Return true if the instruction sets the register.
   */
  public boolean defs(int register, RegisterSet registers)
  {
    return false;
  }

  /**
   * Return the number of bytes required for this instruction.
   */
  public int instructionSize()
  {
    return 1;
  }

  /**
   * Specify the CC set by this instruction.
   */
  public final void setSetCC(int cc)
  {
    //    this.setCC |= X86Generator.ccFlgTab[cc];
    throw new scale.common.NotImplementedError("");
  }

  /**
   * Specify the CCs used by this instruction.
   */
  public final void setUseCC(int cc)
  {
    //    this.useCC |= X86Generator.ccFlgTab[cc];
    throw new scale.common.NotImplementedError("");
  }

  /**
   * Return true if the instruction sets the CC flag specified.
   * @param cc specifies the CC
   */
  public final boolean setsCC(int cc)
  {
    //    return (0 != (X86Generator.ccFlgTab[cc] & setCC));
    throw new scale.common.NotImplementedError("");
  }

  /**
   * Return true if this instruction has a side effect of changing a
   * special register.  An example would be a condition code register
   * for architectures that set the condition and tehn branch on it
   * such as the X86.
   */
  public boolean setsSpecialReg()
  {
    return (setCC != 0);
  }

  /**
   * Return true if the instruction uses the CC flag specified.
   * @param cc specifies the CC
   */
  public final boolean usesCC(int cc)
  {
    //    return (0 != (X86Generator.ccFlgTab[cc] & useCC));
    throw new scale.common.NotImplementedError("");
  }

  /**
   * Return true if this instruction's CC use is independent of the
   * specified instruction.  If instructions are independent, than one
   * instruction can be moved before or after the other instruction
   * without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  protected final boolean independentCC(X86Instruction inst)
  {
    return (0 == ((setCC & inst.setCC) |
                  (setCC & inst.useCC) |
                  (useCC & inst.setCC)));
  }

  /**
   * Specify the registers used and defined by this instruction.
   * Uses must be specified before definitions.
   * @param rs is the register set in use
   * @param index is an index associated with the instruction
   * @param strength is the importance of the instruction
   * @see scale.backend.RegisterAllocator#useRegister(int,int,int)
   * @see scale.backend.RegisterAllocator#defRegister(int,int)
   */
  public void specifyRegisterUsage(RegisterAllocator rs, int index, int strength)
  {
  }

  public void remapRegisters(int[] map)
  {
  }

  /**
   * Return true if the instruction can be deleted without changing
   * program semantics.
   */
  public boolean canBeDeleted(RegisterSet registers)
  {
    return nullified();
  }

  public boolean independent(Instruction inst, RegisterSet registers)
  {
    throw new scale.common.NotImplementedError("");
  }

  public void remapDestRegister(int oldReg, int newReg)
  {
    throw new scale.common.NotImplementedError("");
  }

  public void remapSrcRegister(int oldReg, int newReg)
  {
    throw new scale.common.NotImplementedError("");
  }

  /**
   * Generate a String representation of a Displacement that can be
   * used by the assembly code generater.
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
   * Return 1, 2, 4, or 8 depending on the scale factor specified for
   * the instruction.
   */
  public int getScale()
  {
    return Opcodes.getScale(opcode);
  }

  /**((X86Assembler) 
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

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler asm, Emit emit)
  {
    if (nullified())
      emit.emit("nop ! ");

    emit.emit(Opcodes.getOp(this));
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
