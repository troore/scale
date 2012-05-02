###############################################################################
#
# Copyright 2004 by the <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
# <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
# <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
# Amherst MA. 01003, USA<br>
# All Rights Reserved.<br>
# <p>
# $Id: Makefile,v 1.9 2006-12-05 21:02:08 burrill Exp $
#
###############################################################################

PACKAGE    := scale/frontend/c
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/CReader.class \
		$(DEST)/CPreprocessor.class \
		$(DEST)/C99Lexer.class \
		$(DEST)/C99Parser.class \
		$(DEST)/C99ParserTokenTypes.class \
		$(DEST)/SourceC.class \
		$(DEST)/C99.class

ANTLR = ../antlr.jar

MODULES :=

c99:
	@$(MAKE) CLASSPATH=$(ANTLR):$(CLASSPATH) java

all: c99
	@$(MAKE) CLASSPATH=$(ANTLR):$(CLASSPATH) javaall

C99Parser.java C99Lexer.java: c99.g
	@echo "Creating " $@
	@$(JAVA) -cp $(ANTLR):$(CLASSPATH) antlr.Tool -traceParser c99.g

clean: javaclean
	rm -f C99Lexer* C99Parser*

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
