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
# $Id: Makefile,v 1.15 2004-06-02 13:54:13 burrill Exp $
###############################################################################
PACKAGE    := scale/score/analyses
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Aliases.class \
		$(DEST)/FindAliases.class \
		$(DEST)/PlaceIndirectOps.class \
		$(DEST)/PlaceIndirectOpsSteen.class \
		$(DEST)/PlaceIndirectOpsSH.class \
		$(DEST)/MayUse.class \
		$(DEST)/MayDef.class \
		$(DEST)/CategoriesAliases.class \
		$(DEST)/AliasAnnote.class \
		$(DEST)/VirtualVar.class \
		$(DEST)/SuperVirtualVar.class \
		$(DEST)/SubVirtualVar.class

MODULES    := 

analyses: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
