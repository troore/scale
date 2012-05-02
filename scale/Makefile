###############################################################################
#
# Copyright 2008 by the
# <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
# <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
# <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
# Amherst MA. 01003, USA<br>
# All Rights Reserved.<br>
# <p>
# $Id: Makefile,v 1.94 2007-08-27 18:39:38 burrill Exp $
#
###############################################################################

PACKAGE    := scale
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=

MODULES := \
		common \
		annot \
		clef \
		callGraph \
		score \
		alias \
		clef2scribble \
		jcr \
		j2s \
		frontend \
		backend \
		visual \
		test

.PHONY: scale java javaall javaclean jar javadoc antlr

scale: java

clean: javaclean

all: javaall

jar: java antlr
	(cd $(SCALE)/$(SCALERELEASE)/classes; jar -cfm scale.jar $(SCALE)/$(SCALERELEASE)/scale/manifest.txt scale antlr;)

antlr:
	(cd $(SCALE)/$(SCALERELEASE)/classes; jar -xf $(SCALE)/$(SCALERELEASE)/scale/frontend/antlr.jar;)

javadoc:
	@mkdir -p $(JAVADDEST)
	@-cp docs/html/*.html $(JAVADDEST)
	@-cp docs/html/*.jpg $(JAVADDEST)
	$(JAVAD) $(JAVADFLAGS) -d $(JAVADDEST) -sourcepath $(SCALE)/$(SCALERELEASE) -overview overview.html \
		scale.common \
		scale.annot \
		scale.clef \
		scale.clef.type \
		scale.clef.decl \
		scale.clef.stmt \
		scale.clef.expr \
		scale.clef.symtab \
		scale.score \
		scale.score.expr \
		scale.score.chords \
		scale.score.dependence \
		scale.score.dependence.omega \
		scale.score.dependence.omega.omegaLib \
		scale.score.dependence.banerjee \
		scale.score.pred \
		scale.score.pp \
		scale.score.trans \
		scale.score.analyses \
		scale.visual \
		scale.callGraph \
		scale.alias \
		scale.alias.steensgaard \
		scale.alias.shapirohorowitz \
		scale.clef2scribble \
		scale.frontend \
		scale.frontend.c \
		scale.frontend.fortran \
		scale.backend \
		scale.backend.alpha \
		scale.backend.sparc \
		scale.backend.mips \
		scale.backend.ppc \
		scale.backend.trips2 \
		scale.backend.x86 \
		scale.backend.xyz \
		scale.jcr \
		scale.j2s \
		scale.test

include Makefile.defs




