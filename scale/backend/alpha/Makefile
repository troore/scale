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
# $Id: Makefile,v 1.7 2002-01-03 21:41:42 burrill Exp $
###############################################################################
PACKAGE    := scale/backend/alpha
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/PrologMarker.class \
		$(DEST)/BeginMarker.class \
		$(DEST)/EndMarker.class \
		$(DEST)/AlphaLineMarker.class \
		$(DEST)/MemoryInstruction.class \
		$(DEST)/BranchInstruction.class \
		$(DEST)/LoadInstruction.class \
		$(DEST)/LoadAddressInstruction.class \
		$(DEST)/StoreInstruction.class \
		$(DEST)/JmpInstruction.class \
		$(DEST)/FetchInstruction.class \
		$(DEST)/BarriorInstruction.class \
		$(DEST)/TrapInstruction.class \
		$(DEST)/RPCCInstruction.class \
		$(DEST)/FltCvtInstruction.class \
		$(DEST)/FltOpInstruction.class \
		$(DEST)/IntOpInstruction.class \
		$(DEST)/IntOpLitInstruction.class \
		$(DEST)/Opcodes.class \
		$(DEST)/AlphaMachine.class \
		$(DEST)/AlphaAssembler.class \
		$(DEST)/AlphaRegisterSet.class \
		$(DEST)/AlphaGenerator.class

MODULES    := 

alpha: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
