###############################################################################
#
# Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
# <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
# <a href="http://www.umass.edu/">University of Massachusetts</a.,<br>
# Amherst MA. 01003, USA<br>
# All Rights Reserved.<br>
# <p>
# Compiles the java classes and recurses to sub-directories
#
# Makefiles that include this makefile should have rules called
#   all    - make everything
#   clean  - remove everything that is built
# <p>
# $Id: Makefile,v 1.3 2006-11-17 20:04:31 burrill Exp $
###############################################################################

ifeq ($(SCALETARGETTYPE),trips)
  AR = $(TRIPS_TOOLCHAIN_DIR)/sbin/ar
  BUILDFLAGS ?= -save-temps
  MCC = $(TRIPS_TOOLCHAIN_DIR)/sbin/tcc $(BUILDFLAGS)
else
  AR = ar
  MCC = $(CC)
endif

ODEST := $(SCALELIB)/libF77

.PHONY: libF77 clean clobber

all:
	@mkdir -p $(ODEST)
	@$(MAKE) -j 4 libF77

libF77: $(ODEST)/libF77.a

$(ODEST)/Version.o: Version.c
	@echo "Creating " $@
	@$(MCC) $(CFLAGS) -DSCALETARGET_$(SCALEHOSTTYPE) -DSCALEHOST_$(SCALEHOSTTYPE) -I.. -c $< -o $@

$(ODEST)/%.o: %.c
	@echo "Creating " $@
	@$(MCC)  $(CFLAGS) -DSkip_f2c_Undefs $(CFLAGS) -DSCALETARGET_$(SCALEHOSTTYPE) -DSCALEHOST_$(SCALEHOSTTYPE) -I.. -c $< -o $@

# If your system lacks onexit() and you are not using an
# ANSI C compiler, then you should add -DNO_ONEXIT to CFLAGS,

MISC =	$(ODEST)/F77_aloc.o \
	$(ODEST)/Version.o \
	$(ODEST)/main.o \
	$(ODEST)/s_rnge.o \
	$(ODEST)/abort_.o \
	$(ODEST)/getarg_.o \
	$(ODEST)/iargc_.o \
	$(ODEST)/getenv_.o \
	$(ODEST)/signal_.o \
	$(ODEST)/s_stop.o \
	$(ODEST)/s_paus.o \
	$(ODEST)/system_.o \
	$(ODEST)/cabs.o\
	$(ODEST)/derf_.o \
	$(ODEST)/derfc_.o \
	$(ODEST)/erf_.o \
	$(ODEST)/erfc_.o \
	$(ODEST)/sig_die.o \
	$(ODEST)/exit.o

POW =	$(ODEST)/pow_ci.o \
	$(ODEST)/pow_dd.o \
	$(ODEST)/pow_di.o \
	$(ODEST)/pow_hh.o \
	$(ODEST)/pow_ii.o \
	$(ODEST)/pow_ri.o \
	$(ODEST)/pow_zi.o \
	$(ODEST)/pow_zz.o

CX =	$(ODEST)/c_abs.o \
	$(ODEST)/c_cos.o \
	$(ODEST)/c_div.o \
	$(ODEST)/c_exp.o \
	$(ODEST)/c_log.o \
	$(ODEST)/c_sin.o \
	$(ODEST)/c_sqrt.o

DCX =	$(ODEST)/z_abs.o \
	$(ODEST)/z_cos.o \
	$(ODEST)/z_div.o \
	$(ODEST)/z_exp.o \
	$(ODEST)/z_log.o \
	$(ODEST)/z_sin.o \
	$(ODEST)/z_sqrt.o

REAL =	$(ODEST)/r_abs.o \
	$(ODEST)/r_acos.o \
	$(ODEST)/r_asin.o \
	$(ODEST)/r_atan.o \
	$(ODEST)/r_atn2.o \
	$(ODEST)/r_cnjg.o \
	$(ODEST)/r_cos.o\
	$(ODEST)/r_cosh.o \
	$(ODEST)/r_dim.o \
	$(ODEST)/r_exp.o \
	$(ODEST)/r_imag.o \
	$(ODEST)/r_int.o\
	$(ODEST)/r_lg10.o \
	$(ODEST)/r_log.o \
	$(ODEST)/r_mod.o \
	$(ODEST)/r_nint.o \
	$(ODEST)/r_sign.o\
	$(ODEST)/r_sin.o \
	$(ODEST)/r_sinh.o \
	$(ODEST)/r_sqrt.o \
	$(ODEST)/r_tan.o \
	$(ODEST)/r_tanh.o

DBL =	$(ODEST)/d_abs.o \
	$(ODEST)/d_acos.o \
	$(ODEST)/d_asin.o \
	$(ODEST)/d_atan.o \
	$(ODEST)/d_atn2.o\
	$(ODEST)/d_cnjg.o \
	$(ODEST)/d_cos.o \
	$(ODEST)/d_cosh.o \
	$(ODEST)/d_dim.o \
	$(ODEST)/d_exp.o\
	$(ODEST)/d_imag.o \
	$(ODEST)/d_int.o \
	$(ODEST)/d_lg10.o \
	$(ODEST)/d_log.o \
	$(ODEST)/d_mod.o\
	$(ODEST)/d_nint.o \
	$(ODEST)/d_prod.o \
	$(ODEST)/d_sign.o \
	$(ODEST)/d_sin.o \
	$(ODEST)/d_sinh.o\
	$(ODEST)/d_sqrt.o \
	$(ODEST)/d_tan.o \
	$(ODEST)/d_tanh.o

INT =	$(ODEST)/i_abs.o \
	$(ODEST)/i_dim.o \
	$(ODEST)/i_dnnt.o \
	$(ODEST)/i_indx.o \
	$(ODEST)/i_len.o \
	$(ODEST)/i_mod.o \
	$(ODEST)/i_nint.o \
	$(ODEST)/i_sign.o

HALF =	$(ODEST)/h_abs.o \
	$(ODEST)/h_dim.o \
	$(ODEST)/h_dnnt.o \
	$(ODEST)/h_indx.o \
	$(ODEST)/h_len.o \
	$(ODEST)/h_mod.o \
	$(ODEST)/h_nint.o \
	$(ODEST)/h_sign.o

CMP =	$(ODEST)/l_ge.o \
	$(ODEST)/l_gt.o \
	$(ODEST)/l_le.o \
	$(ODEST)/l_lt.o \
	$(ODEST)/hl_ge.o \
	$(ODEST)/hl_gt.o \
	$(ODEST)/hl_le.o \
	$(ODEST)/hl_lt.o

EFL =	$(ODEST)/ef1asc_.o \
	$(ODEST)/ef1cmc_.o

CHAR =	$(ODEST)/s_cat.o \
	$(ODEST)/s_cmp.o \
	$(ODEST)/s_copy.o

F90BIT = $(ODEST)/lbitbits.o \
	$(ODEST)/lbitshft.o

OBJ = $(MISC) $(POW) $(CX) $(DCX) $(REAL) $(DBL) $(INT) $(HALF) $(CMP) $(EFL) $(CHAR) $(F90BIT)

$(ODEST):
	-mkdir -p $(ODEST)

$(ODEST)/libF77.a : $(OBJ)
	@echo "Creating " $@
	@ar cr $(ODEST)/libF77.a $(OBJ)
	@-ranlib $(ODEST)/libF77.a

clean:
	-rm -rf $(ODEST)

clobber:	clean
	-rm -f $(ODEST)/libF77.a

$(ODEST)/libF77.ann:
