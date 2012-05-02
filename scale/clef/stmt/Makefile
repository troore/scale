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
# $Id: Makefile,v 1.22 2006-06-28 16:39:03 burrill Exp $
###############################################################################
PACKAGE    := scale/clef/stmt
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES := 	$(DEST)/Statement.class \
		$(DEST)/AltCase.class \
		$(DEST)/ArithmeticIfStmt.class \
		$(DEST)/AssignedGotoStmt.class \
		$(DEST)/AssignLabelStmt.class \
		$(DEST)/BlockStmt.class \
		$(DEST)/BreakStmt.class \
		$(DEST)/CaseStmt.class \
		$(DEST)/ComputedGotoStmt.class \
		$(DEST)/ContinueStmt.class \
		$(DEST)/DeclStmt.class \
		$(DEST)/DoLoopStmt.class \
		$(DEST)/EvalStmt.class \
		$(DEST)/ExitStmt.class \
		$(DEST)/ForLoopStmt.class \
		$(DEST)/CloneForLoopStmt.class \
		$(DEST)/GotoStmt.class \
		$(DEST)/IfStmt.class \
		$(DEST)/IfThenElseStmt.class \
		$(DEST)/LabelStmt.class \
		$(DEST)/LoopStmt.class \
		$(DEST)/MultiBranchStmt.class \
		$(DEST)/NullStmt.class \
		$(DEST)/RepeatUntilLoopStmt.class \
		$(DEST)/RepeatWhileLoopStmt.class \
		$(DEST)/ReturnStmt.class \
		$(DEST)/SwitchStmt.class \
		$(DEST)/TestLoopStmt.class \
		$(DEST)/WhileLoopStmt.class

stmt: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
