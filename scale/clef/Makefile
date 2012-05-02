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
# $Id: Makefile,v 1.47 2007-05-10 16:48:00 burrill Exp $
###############################################################################
PACKAGE    := scale/clef
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Node.class \
		$(DEST)/DeclPredicate.class \
		$(DEST)/ExprPredicate.class \
		$(DEST)/StmtPredicate.class \
		$(DEST)/TypePredicate.class \
		$(DEST)/Predicate.class \
		$(DEST)/ErrorPredicate.class \
		$(DEST)/DescendPredicate.class \
		$(DEST)/LiteralMap.class \
		$(DEST)/PureFunctionAnnotation.class \
		$(DEST)/Clef2C.class \
		$(DEST)/Display.class

MODULES    := type decl expr stmt symtab

clef: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
