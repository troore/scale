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
# $Id: Makefile,v 1.28 2007-05-10 16:48:03 burrill Exp $
###############################################################################

PACKAGE    := scale/clef/decl
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES := 	$(DEST)/Declaration.class \
		$(DEST)/DeclTable.class \
		$(DEST)/TypeDecl.class \
		$(DEST)/TypeName.class \
		$(DEST)/ValueDecl.class \
		$(DEST)/RoutineDecl.class \
		$(DEST)/LabelDecl.class \
		$(DEST)/CaseLabelDecl.class \
		$(DEST)/ExceptionDecl.class \
		$(DEST)/VariableDecl.class \
		$(DEST)/RenamedVariableDecl.class \
		$(DEST)/EquivalenceDecl.class \
		$(DEST)/FormalDecl.class \
		$(DEST)/FieldDecl.class \
		$(DEST)/ProcedureDecl.class \
		$(DEST)/ForwardProcedureDecl.class \
		$(DEST)/FileDecl.class \
		$(DEST)/EnumElementDecl.class \
		$(DEST)/UnknownFormals.class \
		$(DEST)/StmtFtnDecl.class \
		$(DEST)/Visibility.class \
		$(DEST)/Residency.class \
		$(DEST)/Accessibility.class \
		$(DEST)/Assigned.class \
		$(DEST)/ParameterMode.class

decl: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
