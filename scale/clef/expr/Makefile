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
# $Id: Makefile,v 1.49 2007-05-10 16:48:04 burrill Exp $
###############################################################################
PACKAGE    := scale/clef/expr
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES := 	$(DEST)/Expression.class \
		$(DEST)/Literal.class \
		$(DEST)/SizeofLiteral.class \
		$(DEST)/StringLiteral.class \
		$(DEST)/CharLiteral.class \
		$(DEST)/BooleanLiteral.class \
		$(DEST)/IntLiteral.class \
		$(DEST)/IntArrayLiteral.class \
		$(DEST)/FloatLiteral.class \
		$(DEST)/FloatArrayLiteral.class \
		$(DEST)/AddressLiteral.class \
		$(DEST)/ComplexLiteral.class \
		$(DEST)/AssignmentOp.class \
		$(DEST)/MonadicOp.class \
		$(DEST)/DyadicOp.class \
		$(DEST)/CompoundAssignmentOp.class \
		$(DEST)/AggregateOp.class \
		$(DEST)/CallOp.class \
		$(DEST)/HeapOp.class \
		$(DEST)/IdReferenceOp.class \
		$(DEST)/IdAddressOp.class \
		$(DEST)/IdValueOp.class \
		$(DEST)/SeriesOp.class \
		$(DEST)/ParenthesesOp.class \
		$(DEST)/PositionOp.class \
		$(DEST)/PositionOffsetOp.class \
		$(DEST)/PositionIndexOp.class \
		$(DEST)/PositionFieldOp.class \
		$(DEST)/PositionRepeatOp.class \
		$(DEST)/AggregationElements.class \
		$(DEST)/AssignSimpleOp.class \
		$(DEST)/DefOp.class \
		$(DEST)/PositiveOp.class \
		$(DEST)/NegativeOp.class \
		$(DEST)/TransFtn.class \
		$(DEST)/TranscendentalOp.class \
		$(DEST)/Transcendental2Op.class \
		$(DEST)/AbsoluteValueOp.class \
		$(DEST)/MinimumOp.class \
		$(DEST)/MaximumOp.class \
		$(DEST)/AdditionOp.class \
		$(DEST)/SubtractionOp.class \
		$(DEST)/MultiplicationOp.class \
		$(DEST)/DivisionOp.class \
		$(DEST)/ModulusOp.class \
		$(DEST)/RemainderOp.class \
		$(DEST)/ExponentiationOp.class \
		$(DEST)/IncrementOp.class \
		$(DEST)/PreDecrementOp.class \
		$(DEST)/PreIncrementOp.class \
		$(DEST)/PostDecrementOp.class \
		$(DEST)/PostIncrementOp.class \
		$(DEST)/EqualityOp.class \
		$(DEST)/NotEqualOp.class \
		$(DEST)/GreaterOp.class \
		$(DEST)/GreaterEqualOp.class \
		$(DEST)/LessOp.class \
		$(DEST)/LessEqualOp.class \
		$(DEST)/BitComplementOp.class \
		$(DEST)/BitAndOp.class \
		$(DEST)/BitXorOp.class \
		$(DEST)/BitOrOp.class \
		$(DEST)/ShiftMode.class \
		$(DEST)/BitShiftOp.class \
		$(DEST)/MultiplicationAssignmentOp.class \
		$(DEST)/DivisionAssignmentOp.class \
		$(DEST)/RemainderAssignmentOp.class \
		$(DEST)/AdditionAssignmentOp.class \
		$(DEST)/SubtractionAssignmentOp.class \
		$(DEST)/BitShiftAssignmentOp.class \
		$(DEST)/BitAndAssignmentOp.class \
		$(DEST)/BitXorAssignmentOp.class \
		$(DEST)/BitOrAssignmentOp.class \
		$(DEST)/NotOp.class \
		$(DEST)/AndOp.class \
		$(DEST)/OrOp.class \
		$(DEST)/AndConditionalOp.class \
		$(DEST)/OrConditionalOp.class \
		$(DEST)/TernaryOp.class \
		$(DEST)/ExpressionIfOp.class \
		$(DEST)/AddressOp.class \
		$(DEST)/DereferenceOp.class \
		$(DEST)/NilOp.class \
		$(DEST)/ThisOp.class \
		$(DEST)/SelectOp.class \
		$(DEST)/SelectIndirectOp.class \
		$(DEST)/StatementOp.class \
		$(DEST)/SubscriptOp.class \
		$(DEST)/SubscriptValueOp.class \
		$(DEST)/SubscriptAddressOp.class \
		$(DEST)/CallFunctionOp.class \
		$(DEST)/AllocatePlacementOp.class \
		$(DEST)/AllocateSettingFieldsOp.class \
		$(DEST)/DeleteOp.class \
		$(DEST)/DeleteArrayOp.class \
		$(DEST)/CastMode.class \
		$(DEST)/TypeConversionOp.class \
		$(DEST)/ComplexOp.class \
		$(DEST)/SubstringOp.class \
		$(DEST)/VarArgOp.class \
		$(DEST)/VaArgOp.class \
		$(DEST)/VaStartOp.class \
		$(DEST)/VaCopyOp.class \
		$(DEST)/VaEndOp.class

expr: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
