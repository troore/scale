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
# $Id: Makefile,v 1.29 2007-05-10 16:48:05 burrill Exp $
###############################################################################
PACKAGE    := scale/clef/type
DEST       := $(CLASSDEST)/$(PACKAGE)


CLASSFILES := 	$(DEST)/Type.class \
		$(DEST)/TypeTable.class \
		$(DEST)/VoidType.class \
		$(DEST)/AtomicType.class \
		$(DEST)/RefAttr.class \
		$(DEST)/RefType.class \
		$(DEST)/CompositeType.class \
		$(DEST)/Raise.class \
		$(DEST)/RaiseWithObject.class \
		$(DEST)/RaiseWithType.class \
		$(DEST)/ProcedureType.class \
		$(DEST)/IncompleteType.class \
		$(DEST)/NumericType.class \
		$(DEST)/PointerType.class \
		$(DEST)/ArrayType.class \
		$(DEST)/AggregateType.class \
		$(DEST)/RealType.class \
		$(DEST)/ComplexType.class \
		$(DEST)/Bound.class \
		$(DEST)/FixedArrayType.class \
		$(DEST)/AllocArrayType.class \
		$(DEST)/RecordType.class \
		$(DEST)/FloatType.class \
		$(DEST)/IntegerType.class \
		$(DEST)/SignedIntegerType.class \
		$(DEST)/UnsignedIntegerType.class \
		$(DEST)/FortranCharType.class \
		$(DEST)/EnumerationType.class \
		$(DEST)/UnionType.class \
		$(DEST)/CharacterType.class \
		$(DEST)/BooleanType.class	

type: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
