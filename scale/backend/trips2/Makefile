###############################################################################
#
# Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
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
# $Id: Makefile,v 1.28 2007-02-08 14:53:28 burrill Exp $
###############################################################################
PACKAGE    := scale/backend/trips2
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES := $(DEST)/BeginMarker.class \
	$(DEST)/BlockSplitter.class \
	$(DEST)/ConstantInstruction.class \
	$(DEST)/DataflowAnalysis.class \
	$(DEST)/EnterInstruction.class \
	$(DEST)/GeneralInstruction.class \
	$(DEST)/Hyperblock.class \
	$(DEST)/HyperblockFormation.class \
	$(DEST)/ImmediateInstruction.class \
	$(DEST)/LoadInstruction.class \
	$(DEST)/Opcodes.class \
	$(DEST)/Peepholer.class \
	$(DEST)/PhiInstruction.class \
	$(DEST)/PredicateBlock.class \
	$(DEST)/SSA.class \
	$(DEST)/StoreInstruction.class \
	$(DEST)/TILReader.class \
	$(DEST)/Trips2Allocator.class \
	$(DEST)/Trips2AllocatorHybrid.class \
	$(DEST)/Trips2Assembler.class \
	$(DEST)/Trips2Generator.class \
	$(DEST)/Trips2LineMarker.class \
	$(DEST)/Trips2Machine.class \
	$(DEST)/Trips2RegisterSet.class \
	$(DEST)/TripsBranch.class \
	$(DEST)/TripsInstruction.class \
	$(DEST)/TripsIntrinsics.class \
	$(DEST)/TripsLabel.class \
	$(DEST)/TripsLoopICEstimator.class \
	$(DEST)/TripsPGenerator.class 

MODULES    := 

alpha: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
