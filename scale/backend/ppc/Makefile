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
# $Id: Makefile,v 1.7 2004-09-01 14:47:42 burrill Exp $
###############################################################################
PACKAGE    := scale/backend/ppc
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Opcodes.class \
		$(DEST)/PPCInstruction.class \
		$(DEST)/PPCBranch.class \
		$(DEST)/IFormInstruction.class \
		$(DEST)/BFormInstruction.class \
		$(DEST)/SCFormInstruction.class \
                $(DEST)/FDInstruction.class \
                $(DEST)/FDcInstruction.class \
                $(DEST)/FDrInstruction.class \
                $(DEST)/FDrcInstruction.class \
                $(DEST)/FDrccInstruction.class \
                $(DEST)/FDrcccInstruction.class \
                $(DEST)/FDrdInstruction.class \
                $(DEST)/FDrrInstruction.class \
                $(DEST)/FDrrcInstruction.class \
                $(DEST)/FDrrccInstruction.class \
                $(DEST)/FDrrrInstruction.class \
                $(DEST)/FcInstruction.class \
                $(DEST)/FccInstruction.class \
                $(DEST)/FcccInstruction.class \
                $(DEST)/FcrInstruction.class \
                $(DEST)/FcrdInstruction.class \
                $(DEST)/FcrrInstruction.class \
                $(DEST)/FrInstruction.class \
                $(DEST)/FrcInstruction.class \
                $(DEST)/FrrInstruction.class \
                $(DEST)/FrrcccInstruction.class \
                $(DEST)/FrrdInstruction.class \
                $(DEST)/FrrrInstruction.class \
                $(DEST)/PPCMachine.class \
                $(DEST)/PPCLineMarker.class \
		$(DEST)/PPCAssembler.class \
		$(DEST)/PPCRegisterSet.class \
		$(DEST)/PPCG4RegisterSet.class \
		$(DEST)/PPCG5RegisterSet.class \
		$(DEST)/BeginMarker.class\
		$(DEST)/EndMarker.class\
		$(DEST)/MemoryInstruction.class\
		$(DEST)/StoreInstruction.class\
		$(DEST)/LoadInstruction.class\
		$(DEST)/StorexInstruction.class\
		$(DEST)/LoadxInstruction.class\
		$(DEST)/PrologMarker.class\
		$(DEST)/FDccInstruction.class\
		$(DEST)/FDdInstruction.class\
		$(DEST)/FDdrInstruction.class\
		$(DEST)/PPCGenerator.class

MODULES    := 

ppc: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
