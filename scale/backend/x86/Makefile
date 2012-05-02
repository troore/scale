###############################################################################
#
# Copyright 2008 by the
# <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
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
# $Id: Makefile,v 1.1 2007-11-01 16:52:29 burrill Exp $
###############################################################################
PACKAGE    := scale/backend/x86
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=   $(DEST)/X86Machine.class \
                $(DEST)/X86Instruction.class \
                $(DEST)/X86DInstruction.class \
                $(DEST)/X86RInstruction.class \
                $(DEST)/X86RDInstruction.class \
                $(DEST)/X86RRInstruction.class \
                $(DEST)/X86RRDInstruction.class \
                $(DEST)/X86RRDDInstruction.class \
                $(DEST)/X86RRRInstruction.class \
                $(DEST)/X86RRRDInstruction.class \
                $(DEST)/X86RRRDDInstruction.class \
                $(DEST)/X86Branch.class \
                $(DEST)/X86DBranch.class \
                $(DEST)/X86RBranch.class \
                $(DEST)/X86RDBranch.class \
                $(DEST)/X86RRBranch.class \
                $(DEST)/X86RRDBranch.class \
		$(DEST)/Opcodes.class \
		$(DEST)/X86Assembler.class \
		$(DEST)/X86RegisterSet.class \
		$(DEST)/X86Generator.class

MODULES    := 

alpha: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
