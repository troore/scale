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
# $Id: Makefile,v 1.4 2002-03-18 22:11:15 burrill Exp $
###############################################################################
PACKAGE    := scale/backend/mips
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/BeginMarker.class \
		$(DEST)/CmpBranchInstruction.class \
		$(DEST)/CondMovInstruction.class \
		$(DEST)/EndMarker.class \
		$(DEST)/FltBranchInstruction.class \
		$(DEST)/FltCmpInstruction.class \
		$(DEST)/FltOpInstruction.class \
		$(DEST)/IntOpInstruction.class \
		$(DEST)/IntOpLitInstruction.class \
		$(DEST)/JumpLabelInstruction.class \
		$(DEST)/JumpRegInstruction.class \
		$(DEST)/LoadImmediateInstruction.class \
		$(DEST)/LoadInstruction.class \
		$(DEST)/MFSpecialInstruction.class \
		$(DEST)/MipsAssembler.class \
		$(DEST)/MipsBranch.class \
		$(DEST)/MipsGenerator.class \
		$(DEST)/MipsInstruction.class \
		$(DEST)/MipsLineMarker.class \
		$(DEST)/MipsMachine.class \
		$(DEST)/MipsRegisterSet.class \
		$(DEST)/MultInstruction.class \
		$(DEST)/Opcodes.class \
		$(DEST)/PrologMarker.class \
		$(DEST)/StoreInstruction.class \
		$(DEST)/TrapInstruction.class

MODULES    := 

mips: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
