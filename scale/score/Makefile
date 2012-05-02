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
# $Id: Makefile,v 1.54 2007-08-23 16:41:00 burrill Exp $
###############################################################################
PACKAGE    := scale/score
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Note.class \
		$(DEST)/Predicate.class \
		$(DEST)/Scribble.class \
		$(DEST)/Domination.class \
		$(DEST)/DominanceFrontier.class \
		$(DEST)/PureFunctionAnalyser.class \
		$(DEST)/InductionVar.class \
		$(DEST)/SSA.class \
		$(DEST)/Scribble2C.class \
		$(DEST)/CDG.class

MODULES    := expr chords pred dependence trans analyses pp

score: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
