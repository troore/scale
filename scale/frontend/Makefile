###############################################################################
#
# Copyright 2007 by the <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
# <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
# <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
# Amherst MA. 01003, USA<br>
# All Rights Reserved.<br>
# <p>
# $Id: Makefile,v 1.4 2007-03-21 13:32:05 burrill Exp $
#
###############################################################################

PACKAGE    := scale/frontend
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Parser.class \
		$(DEST)/SourceLanguage.class

MODULES := c fortran ada java cxx

.PHONY: c fortran ada java cxx

frontend: java

all:
	$(MAKE) javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
