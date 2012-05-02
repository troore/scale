###############################################################################
#
# Copyright 2002 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
# <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
# <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
# Amherst MA. 01003, USA<br>
# All Rights Reserved.<br>
# <p>
# Compiles the java classes and recurses to sub-directories
#
# Makefiles that include this makefile should have rules called
#   all    - make everything
#   clean  - remove everything that is built
# These rules should depend on java, javaall, and javaclean.
# <p>
# $Id: Makefile,v 1.8 2003-11-17 22:41:41 burrill Exp $
###############################################################################
PACKAGE    := scale/backend/sparc
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/SparcMachine.class \
		$(DEST)/Opcodes.class \
		$(DEST)/BeginMarker.class \
		$(DEST)/EndMarker.class \
		$(DEST)/SparcLineMarker.class \
		$(DEST)/SparcBranch.class \
		$(DEST)/CallInstruction.class \
		$(DEST)/BranchInstruction.class \
		$(DEST)/BranchCCInstruction.class \
		$(DEST)/BranchRegInstruction.class \
		$(DEST)/JmplInstruction.class \
		$(DEST)/JmplLitInstruction.class \
		$(DEST)/ReturnInstruction.class \
		$(DEST)/ReturnLitInstruction.class \
		$(DEST)/SparcInstruction.class \
		$(DEST)/OpConInstruction.class \
		$(DEST)/SethiInstruction.class \
		$(DEST)/IlltrapInstruction.class \
		$(DEST)/FltOpInstruction.class \
		$(DEST)/FltOp2Instruction.class \
		$(DEST)/FltCmpInstruction.class \
		$(DEST)/FmoveInstruction.class \
		$(DEST)/IntOpInstruction.class \
		$(DEST)/IntOpLitInstruction.class \
		$(DEST)/FtnOpInstruction.class \
		$(DEST)/FtnOpLitInstruction.class \
		$(DEST)/ImoveInstruction.class \
		$(DEST)/ImoveLitInstruction.class \
		$(DEST)/LoadInstruction.class \
		$(DEST)/LoadLitInstruction.class \
		$(DEST)/StoreInstruction.class \
		$(DEST)/StoreLitInstruction.class \
		$(DEST)/Inst7Instruction.class \
		$(DEST)/Inst8Instruction.class \
		$(DEST)/CasaInstruction.class \
		$(DEST)/AsiLitInstruction.class \
		$(DEST)/MembarInstruction.class \
		$(DEST)/TrapInstruction.class \
		$(DEST)/TrapLitInstruction.class \
		$(DEST)/ReadRegInstruction.class \
		$(DEST)/WriteRegInstruction.class \
		$(DEST)/WriteRegLitInstruction.class \
		$(DEST)/AnnulMarker.class \
		$(DEST)/SparcMachine.class \
		$(DEST)/SparcAssembler.class \
		$(DEST)/SparcRegisterSet.class \
		$(DEST)/SparcV9RegisterSet.class \
		$(DEST)/SparcV8RegisterSet.class \
		$(DEST)/SparcGenerator.class

MODULES    := 

sparc: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
