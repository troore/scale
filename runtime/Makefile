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
# $Id: Makefile,v 1.3 2007-10-29 17:38:47 burrill Exp $
###############################################################################

.PHONY: libScale clean clobber

ifeq ($(SCALETARGETTYPE),trips)
  AR        := $(TRIPS_TOOLCHAIN_DIR)/sbin/ar
  MCC       := $(TRIPS_TOOLCHAIN_DIR)/sbin/tcc -march $(TRIPS_HW_CONFIG) -save-temps
  LIBSCALE  := $(SCALELIB)/libScale.a
  LIBSCALEANN:= $(SCALELIB)/libScale.ann
else
  LIBSCALE     := $(SCALELIB)/libScale_$(SCALETARGETTYPE).a
  LIBSCALEANN  := $(SCALELIB)/libScale.ann
  MCC          := $(CC)
  ifeq ($(SCALEHOSTTYPE),alpha)
	MCC  = $(CC)  -pthread
  endif
  ifeq ($(SCALEHOSTTYPE),sparc)
	MCC  = $(CC)
  endif
  ifeq ($(SCALEHOSTTYPE),i686)
	MCC  = $(CC)
  endif
  ifeq ($(SCALEHOSTTYPE),ppc)
	MCC  = $(CC)
  endif
  ifeq ($(CC),gcc)
	WARN = -Wall
	PIC =
  else
	WARN = +w
	PIC = -KPIC
  endif
endif

all:
	@mkdir -p $(SCALELIB)
	@(cd libscale; $(MAKE) all -j 4;)
	@(cd libI77; $(MAKE) all -j 4;)
	@(cd libF77; $(MAKE) all -j 4;)
	@$(MAKE) -j 4 $(LIBSCALE)

$(SCALELIB)/libscale/libscale.a:
	@(cd libscale; $(MAKE) all -j 4;)

$(SCALELIB)/libI77/libI77.a:
	@(cd libI77; $(MAKE) all -j 4;)

$(SCALELIB)/libF77/libF77.a:
	@(cd libF77; $(MAKE) all -j 4;)

$(LIBSCALE): $(SCALELIB)/libI77/libI77.a $(SCALELIB)/libF77/libF77.a $(SCALELIB)/libscale/libscale.a $(LIBSCALEANN)
	@echo "Creating" $@
	@-rm -rf $(SCALELIB)/libobjs $(LIBSCALE)
	@-mkdir -p $(SCALELIB)/libobjs
	@(cd $(SCALELIB)/libobjs; $(AR) -x $(SCALELIB)/libscale/libscale.a;)
	@$(AR) csr $(LIBSCALE) $(SCALELIB)/libobjs/*.o
	@-rm -f $(SCALELIB)/libobjs/*
	@(cd $(SCALELIB)/libobjs; $(AR) -x $(SCALELIB)/libI77/libI77.a;)
	@$(AR) csr $(LIBSCALE) $(SCALELIB)/libobjs/*.o
	@-rm -f $(SCALELIB)/libobjs/*
	@(cd $(SCALELIB)/libobjs; $(AR) -x $(SCALELIB)/libF77/libF77.a;)
	@$(AR) csr $(LIBSCALE) $(SCALELIB)/libobjs/*.o
	@-rm -rf $(SCALELIB)/libobjs

clobber:
	-rm -rf $(SCALELIB)

clean:
	-rm -f $(LIBSCALE)
	-rm -f $(SCALELIB)/libI77/*.o $(SCALELIB)/libF77/*.o $(SCALELIB)/libscale/*.o

$(LIBSCALEANN):
	@echo "Creating " $@
	@mkdir -p $(SCALELIB)
	@(cd libscale; $(MAKE) $(SCALELIB)/libscale/libscale.ann -j 4;)
	@(cd libI77; $(MAKE) $(SCALELIB)/libI77/libI77.ann -j 4;)
	@(cd libF77; $(MAKE) $(SCALELIB)/libF77/libF77.ann -j 4;)
	@cat $(SCALELIB)/libscale/libscale.ann >$@

trips: $(LIBSCALE) $(LIBSCALEANN)
	cp $(LIBSCALE) $(TRIPS_TOOLCHAIN_DIR)/lib/$(TRIPS_HW_CONFIG)
	cp $(LIBSCALEANN) $(TRIPS_TOOLCHAIN_DIR)/lib/$(TRIPS_HW_CONFIG)
