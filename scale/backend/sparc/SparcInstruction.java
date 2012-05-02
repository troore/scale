package scale.backend.sparc;

import scale.common.*;
import scale.backend.*;

/** 
 * This is the base class for all Sparc instructions except branches.
 * <p>
 * $Id: SparcInstruction.java,v 1.23 2007-09-20 18:56:42 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * <pre>
 * Inst  Op Format                                Repr. Instruction           Class
 * 00    1  disp30                                call     label              CallInstruction
 * 01    0      -op2-const22                      illtrap  const22            IlltrapInstruction
 * 01    0    rd-op2-imm22                        sethi    const22,reg        SethiInstruction
 * 02    0  a-cond-op2-disp22                     fbcc     label              BranchInstruction
 * 03    0  a-cond-op2-cc1-cc0-p-disp19           fbpcc    %fccn,label        BranchCCInstruction
 * 04    0  a-rcnd-op2-d16hi-p- rs1-d16lo         brcc     reg,lab            BranchRegInstruction
 * 05    2    rd-op3- rs1-0-    -rs2              add      reg,reg,reg        IntOpInstruction
 * 05    3   fcn-op3- rs1-0-    -rs2              ldfsr    reg+reg,%fsr       FtnOpInstruction
 * 06    2    rd-op3- rs1-1-simm13                add      reg,imm,reg        IntOpLitInstruction
 * 06    3   fcn-op3- rs1-1-simm13                prefetch reg+address,fcn    FtnOpLitInstruction
 * 07    2      -op3-    -0-                      flushw                      SparcInstruction
 * 07    2      -op3- rs1-0-    -rs2              flush    reg+reg            Inst7Instruction
 * 07    2      -op3- rs1-0-    -rs2              return   reg+reg            ReturnInstruction
 * 08    2      -op3- rs1-1-simm13                flush    reg+address        Inst8Instruction
 * 08    2      -op3- rs1-1-simm13                return   reg+address        ReturnLitInstruction
 * 09    2    rd-op3- rs1-0-rcnd-    -rs2         movrcc   reg,reg,reg        IntOpInstruction
 * 10    2    rd-op3- rs1-1-rcnd-simm10           movrcc   reg,simm10,reg     IntOpLitInstruction
 * 11    3    rd-op3- rs1-0-    -rd2              casa     reg,%asi,reg,reg   CasaInstruction
 * 12    2  0000-op3-000f-0-    -cmask-mmask      membar   mask               MembarInstruction
 * 13    3    rd-op3- rs1-0- asi-rd2              casa     reg,asi,reg,reg    AsiLitInstruction
 * 14    2  imp1-op3-imp2                         impdep   const22            IlltrapInstruction
 * 15    2    rd-op3- rs1-0-    -rs2              sll      reg,reg,reg        IntOpInstruction
 * 16    2    rd-op3- rs1-1-scnt32                sll      reg,scnt32,reg     IntOpLitInstruction
 * 17    2    rd-op3- rs1-1-scnt64                sllx     reg,scnt64,reg     IntOpLitInstruction
 * 18    2    rd-op3-    -opf-rs2                 fmovs    reg,reg            FltOpInstruction
 * 19    2  cc10-op3- rs1-opf-rs2                 fcmps    %fccn,reg,reg      FltCmpInstruction
 * 20    2    rd-op3- rs1-opf-rs2                 fadds    reg,reg,reg        FltOp2Instruction
 * 21    2    rd-op3- rs1-                        rdpr     preg,reg           ReadRegInstruction
 * 21    2  0000-op3-000f-0000-                   stbar                       SparcInstruction
 * 22    2  000x-op3-                             done                        SparcInstruction
 * 24    2  cond-op3- rs1-0-cc1-cc0-    -rs2      tcc      %icc,reg+reg       TrapInstruction
 * 26    2    rd-op3-cc2-cond-0-cc1-cc0-    -rs2  movcc    %cc,reg,reg        ImoveInstruction
 * 27    2    rd-op3-cc2-cond-1-cc1-cc0-simm11    movcc    %cc,simm11,reg     ImoveLitInstruction
 * 28    2  cond-op3- rs1-1-cc1-cc0-    -swtn     tcc      %icc,reg+swtn      TrapLitInstruction
 * 29    2    rd-op3- rs1-rcnd-opf_low-rs2        fmovrcc  reg,reg,reg        FltOp2Instruction
 * 30    2    rd-op3-cond-opf_cc-opf_low-rs2      fmovscc  %cc,reg,reg        FmoveInstruction
 * </pre>
 * The Inst references Figures 33 & 34 on pages 64 & 65 of the 
 * Sparc Architecture Manual Version 9.
 */

public abstract class SparcInstruction extends Instruction
{
  /**
   * the instruction opcode
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
  public SparcInstruction(int opcode)
  {
    this.opcode = opcode;
    this.setCC  = 0;
    this.useCC  = 0;
  }

  public int getOpcode()
  {
    return opcode;
  }

  protected void setOpcode(int opcode)
  {
    this.opcode = opcode;
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
   * @return the number of bytes required for the BranchRegInstruction
   */
  public int instructionSize()
  {
    return 4;
  }

  /**
   * Specify the CC set by this instruction.
   */
  public final void setSetCC(int cc)
  {
    this.setCC |= SparcGenerator.ccFlgTab[cc];
  }

  /**
   * Specify the CCs used by this instruction.
   */
  public final void setUseCC(int cc)
  {
    this.useCC |= SparcGenerator.ccFlgTab[cc];
  }

  /**
   * Return true if the instruction sets the CC flag specified.
   * @param cc specifies the CC
   */
  public final boolean setsCC(int cc)
  {
    return (0 != (SparcGenerator.ccFlgTab[cc] & setCC));
  }

  /**
   * Return true if this instruction has a side effect of changing a
   * special register.  An example would be a condition code register
   * for architectures that set the condition and tehn branch on it
   * such as the Sparc.
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
    return (0 != (SparcGenerator.ccFlgTab[cc] & useCC));
  }

  /**
   * Return true if this instruction's CC use is independent of the
   * specified instruction.  If instructions are independent, than one
   * instruction can be moved before or after the other instruction
   * without changing the semantics of the program.
   * @param inst is the specified instruction
   */
  protected final boolean independentCC(SparcInstruction inst)
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

  /**
   * Generate a String representation of a Displacement that can be
   * used by the assembly code generater.
   */
  public String assembleDisp(Assembler asm, Displacement disp, int ftn)
  {
    if (ftn == SparcGenerator.FT_NONE)
      return disp.assembler(asm);

    StringBuffer buf = new StringBuffer(SparcGenerator.ftns[ftn]);
    buf.append('(');
    buf.append(disp.assembler(asm));
    buf.append(')');
    return buf.toString();
  }

  /**
   * Insert the assembler representation of the instruction into the
   * output stream.
   */
  public void assembler(Assembler gen, Emit emit)
  {
    emit.emit(Opcodes.getOp(this));
  }

  public String toString()
  {
    return Opcodes.getOp(this);
  }
}
