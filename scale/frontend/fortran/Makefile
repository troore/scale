###############################################################################
#
# Copyright 2006 by the <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
# <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
# <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
# Amherst MA. 01003, USA<br>
# All Rights Reserved.<br>
# <p>
# $Id: Makefile,v 1.6 2006-12-05 21:02:10 burrill Exp $
#
###############################################################################

PACKAGE    := scale/frontend/fortran
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/MK.class \
		$(DEST)/Keywords.class \
		$(DEST)/Intrinsics.class \
		$(DEST)/ImpliedDo.class \
		$(DEST)/EquivSet.class \
		$(DEST)/SourceFortran.class \
		$(DEST)/F95.class

MODULES :=

fortran: Keywords.java Intrinsics.java java

Keywords.java Intrinsics.java: $(DEST)/MK.class keywords.txt
	java -cp $(CLASSDEST) scale.frontend.fortran.MK

all: Keywords.java Intrinsics.java javaall

clean: javaclean
	-rm -f Keywords.java

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
