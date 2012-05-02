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
# $Id: Makefile,v 1.24 2007-11-01 16:52:17 burrill Exp $
###############################################################################
PACKAGE    := scale/backend
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Assembler.class \
		$(DEST)/BBIS.class \
		$(DEST)/Branch.class \
		$(DEST)/CommentMarker.class \
		$(DEST)/DiffDisplacement.class \
		$(DEST)/Displacement.class \
		$(DEST)/DominanceFrontier.class \
		$(DEST)/Domination.class \
		$(DEST)/ResultMode.class \
		$(DEST)/Generator.class \
		$(DEST)/ICEstimator.class \
		$(DEST)/Instruction.class \
		$(DEST)/IntegerDisplacement.class \
		$(DEST)/FloatDisplacement.class \
		$(DEST)/Label.class \
		$(DEST)/LabelDisplacement.class \
		$(DEST)/LineMarker.class \
		$(DEST)/Marker.class \
		$(DEST)/Node.class \
		$(DEST)/OffsetDisplacement.class \
		$(DEST)/QDRA.class \
		$(DEST)/RegisterAllocator.class \
		$(DEST)/RegisterSet.class \
		$(DEST)/SpaceAllocation.class \
		$(DEST)/Stabs.class \
		$(DEST)/StackDisplacement.class \
		$(DEST)/SymbolDisplacement.class \

MODULES    := alpha sparc mips ppc trips2 x86 xyz

backend: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
