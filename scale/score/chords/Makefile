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
# $Id: Makefile,v 1.20 2007-03-21 13:32:11 burrill Exp $
###############################################################################
PACKAGE    := scale/score/chords
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Chord.class \
		$(DEST)/SequentialChord.class \
		$(DEST)/NullChord.class \
		$(DEST)/LoopPreHeaderChord.class \
		$(DEST)/LoopExitChord.class \
		$(DEST)/LoopHeaderChord.class \
		$(DEST)/LoopTailChord.class \
		$(DEST)/BeginChord.class \
		$(DEST)/EndChord.class \
		$(DEST)/ExprChord.class \
		$(DEST)/PhiExprChord.class \
		$(DEST)/DecisionChord.class \
		$(DEST)/SwitchChord.class \
		$(DEST)/IfThenElseChord.class \
		$(DEST)/BranchChord.class \
		$(DEST)/GotoChord.class \
		$(DEST)/LeaveChord.class \
		$(DEST)/ReturnChord.class \
		$(DEST)/ExitChord.class \
		$(DEST)/MarkerChord.class

MODULES    := 

chords: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
