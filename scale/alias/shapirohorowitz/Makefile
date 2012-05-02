###############################################################################
#
#   File:    Makefile
#   Date:    July 1999
#   Purpose: Makefile for dependence
#   Prerequisites:
#       - Requires SCALEHOME, SCALERELEASE, and CLASSDEST
#         environment variables to be set.
#
# $Id: Makefile,v 1.3 2001-05-02 19:26:49 burrill Exp $
#
###############################################################################

PACKAGE    := scale/alias/shapirohorowitz
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES := 	$(DEST)/FunctionTypeCat.class \
		$(DEST)/LocationTypeCat.class \
		$(DEST)/TypeVarCat.class \
		$(DEST)/ValueTypeCat.class \
		$(DEST)/ShapiroHorowitz.class

shapirohorowitz: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs

