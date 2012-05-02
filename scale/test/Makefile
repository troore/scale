###############################################################################
#
# Copyright 2002 by the <a href="http://spa-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
# <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
# <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
# Amherst MA. 01003, USA<br>
# All Rights Reserved.<br>
# <p>
# $Id: Makefile,v 1.22 2007-08-02 20:17:03 burrill Exp $
#
###############################################################################

PACKAGE    := scale/test
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/CmdParam.class \
                $(DEST)/AnnotationFile.class \
                $(DEST)/LOC.class \
		$(DEST)/Stats.class \
		$(DEST)/GeomeanTime.class \
		$(DEST)/TripsTimes.class \
		$(DEST)/TestGen.class \
		$(DEST)/Scale.class \
		$(DEST)/CC.class

test: java

version:
	@( \
	  export XD ; \
	  XD=`date` ; \
	  export XC ; \
	  XC=`echo "sed 's/version *= *\".*\";/version  = \"" $$XD "\";/g' Scale.java"` ; \
	  eval $$XC  >xxx.java ; \
	  export X1 ; \
	  export X2 ; \
	  X1=`wc xxx.java   | awk '{print $$1}'` ; \
	  X2=`wc Scale.java | awk '{print $$1}'` ; \
	  if \
	    test $$X1 -eq $$X2 ; \
	  then \
	    cp xxx.java Scale.java ; \
	  fi ; \
	  rm -f xxx.java ; \
	)

all:
	$(MAKE) javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
