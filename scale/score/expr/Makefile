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
# $Id: Makefile,v 1.28 2004-06-02 13:54:16 burrill Exp $
###############################################################################
PACKAGE    := scale/score/expr
DEST       := $(CLASSDEST)/$(PACKAGE)

CLASSFILES :=	$(DEST)/Expr.class \
		$(DEST)/CompareMode.class \
		$(DEST)/DualExpr.class \
		$(DEST)/BinaryExpr.class \
		$(DEST)/SubscriptExpr.class \
		$(DEST)/LoadExpr.class \
		$(DEST)/LoadDeclValueExpr.class \
		$(DEST)/LoadDeclAddressExpr.class \
		$(DEST)/FieldExpr.class \
		$(DEST)/LoadFieldValueExpr.class \
		$(DEST)/LoadFieldAddressExpr.class \
		$(DEST)/LoadValueIndirectExpr.class \
		$(DEST)/ValueExpr.class \
		$(DEST)/LiteralExpr.class \
		$(DEST)/NilExpr.class \
		$(DEST)/UnaryExpr.class \
		$(DEST)/NegativeExpr.class \
		$(DEST)/TranscendentalExpr.class \
		$(DEST)/Transcendental2Expr.class \
		$(DEST)/NotExpr.class \
		$(DEST)/AllocateExpr.class \
		$(DEST)/AdditionExpr.class \
		$(DEST)/SubtractionExpr.class \
		$(DEST)/MultiplicationExpr.class \
		$(DEST)/AbsoluteValueExpr.class \
		$(DEST)/ExponentiationExpr.class \
		$(DEST)/BitComplementExpr.class \
		$(DEST)/BitAndExpr.class \
		$(DEST)/BitOrExpr.class \
		$(DEST)/BitXorExpr.class \
		$(DEST)/BitShiftExpr.class \
		$(DEST)/DivisionExpr.class \
		$(DEST)/RemainderExpr.class \
		$(DEST)/GreaterExpr.class \
		$(DEST)/GreaterEqualExpr.class \
		$(DEST)/LessExpr.class \
		$(DEST)/LessEqualExpr.class \
		$(DEST)/MatchExpr.class \
		$(DEST)/EqualityExpr.class \
		$(DEST)/NotEqualExpr.class \
		$(DEST)/CompareExpr.class \
		$(DEST)/AndExpr.class \
		$(DEST)/OrExpr.class \
		$(DEST)/NaryExpr.class \
		$(DEST)/CallExpr.class \
		$(DEST)/CallFunctionExpr.class \
		$(DEST)/CallMethodExpr.class \
		$(DEST)/PhiExpr.class \
		$(DEST)/VectorExpr.class \
		$(DEST)/MaxExpr.class \
		$(DEST)/MinExpr.class \
		$(DEST)/ConversionExpr.class \
		$(DEST)/TernaryExpr.class \
		$(DEST)/ArrayIndexExpr.class \
		$(DEST)/ConditionalExpr.class \
		$(DEST)/ExprPhiExpr.class \
		$(DEST)/VarArgExpr.class \
		$(DEST)/VaArgExpr.class \
		$(DEST)/VaEndExpr.class \
		$(DEST)/VaStartExpr.class \
		$(DEST)/ComplexValueExpr.class

MODULES    := 

expr: java

all: javaall

clean: javaclean

include $(SCALE)/$(SCALERELEASE)/scale/Makefile.defs
