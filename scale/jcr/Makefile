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
# $Id: Makefile,v 1.5 2002-01-03 21:45:05 burrill Exp $
###############################################################################
PACKAGE    := scale/jcr
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/AttributeInfo.class \
	$(DEST)/CPInfo.class \
	$(DEST)/ClassCPInfo.class \
	$(DEST)/ClassFile.class \
	$(DEST)/CodeAttribute.class \
	$(DEST)/ConstantValueAttribute.class \
	$(DEST)/DoubleCPInfo.class \
	$(DEST)/ExceptionEntry.class \
	$(DEST)/ExceptionsAttribute.class \
	$(DEST)/FieldInfo.class \
	$(DEST)/FieldRefCPInfo.class \
	$(DEST)/FloatCPInfo.class \
	$(DEST)/Info.class \
	$(DEST)/IntCPInfo.class \
	$(DEST)/InterfaceMethodRefCPInfo.class \
	$(DEST)/LineNumberEntry.class \
	$(DEST)/LineNumberTableAttribute.class \
	$(DEST)/LocalVariableTableAttribute.class \
	$(DEST)/LocalVariableEntry.class \
	$(DEST)/LongCPInfo.class \
	$(DEST)/MethodInfo.class \
	$(DEST)/MethodRefCPInfo.class \
	$(DEST)/NameAndTypeCPInfo.class \
	$(DEST)/OtherAttribute.class \
	$(DEST)/RefCPInfo.class \
	$(DEST)/SourceFileAttribute.class \
	$(DEST)/StringCPInfo.class \
	$(DEST)/Utf8CPInfo.class


reader: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
