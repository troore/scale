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
# $Id: Makefile,v 1.55 2007-10-04 19:58:11 burrill Exp $
###############################################################################
PACKAGE    := scale/common
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Machine.class \
		$(DEST)/BitVect.class \
		$(DEST)/Cost.class \
		$(DEST)/Debug.class \
		$(DEST)/DisjointSet.class \
		$(DEST)/EmptyEnumeration.class \
		$(DEST)/SingleEnumeration.class \
		$(DEST)/DoubleEnumeration.class \
		$(DEST)/EmptyIterator.class \
		$(DEST)/SingleIterator.class \
		$(DEST)/DoubleIterator.class \
                $(DEST)/Error.class \
		$(DEST)/Exception.class \
		$(DEST)/HashMap.class \
		$(DEST)/HashSet.class \
		$(DEST)/WorkArea.class \
		$(DEST)/Table.class \
		$(DEST)/Vector.class \
		$(DEST)/Stack.class \
		$(DEST)/InternalError.class \
		$(DEST)/IntMap.class \
		$(DEST)/StringTable.class \
		$(DEST)/InvalidException.class \
		$(DEST)/InvalidKeyException.class \
		$(DEST)/InvalidMutationError.class \
		$(DEST)/InvalidTableError.class \
		$(DEST)/NoSuchElementException.class \
		$(DEST)/NotImplementedError.class \
		$(DEST)/ResourceException.class \
		$(DEST)/RuntimeException.class \
		$(DEST)/UniqueName.class \
		$(DEST)/Lattice.class \
		$(DEST)/PragmaStk.class \
		$(DEST)/DColor.class \
		$(DEST)/DShape.class \
		$(DEST)/DEdge.class \
		$(DEST)/DisplayGraph.class \
		$(DEST)/DisplayNode.class \
		$(DEST)/Statistics.class \
		$(DEST)/Emit.class \
		$(DEST)/EmitToFile.class \
		$(DEST)/EmitToString.class \
		$(DEST)/Msg.class \
		$(DEST)/MsgEnglish.class \
		$(DEST)/ProfileInfo.class \
		$(DEST)/Root.class

common: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
