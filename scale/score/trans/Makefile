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
# $Id: Makefile,v 1.39 2007-03-21 13:32:15 burrill Exp $
###############################################################################
PACKAGE    := scale/score/trans
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Optimization.class \
		$(DEST)/Noopt.class \
		$(DEST)/DeadVarElimination.class \
		$(DEST)/UselessCopy.class \
		$(DEST)/URJ.class \
		$(DEST)/AASR.class \
		$(DEST)/PRE.class \
		$(DEST)/ValNum.class \
		$(DEST)/SCC.class	\
		$(DEST)/ExprMap.class \
		$(DEST)/BasicBlockOps.class \
		$(DEST)/CP.class \
		$(DEST)/LICM.class \
		$(DEST)/ScalarReplacement.class \
		$(DEST)/GlobalVarReplacement.class \
		$(DEST)/SFIR.class \
		$(DEST)/LoopTrans.class \
		$(DEST)/LoopPermute.class \
		$(DEST)/Inlining.class \
		$(DEST)/TreeHeight.class

MODULES    := 

trans: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
