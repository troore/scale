###############################################################################
#
# Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
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
# $Id: Makefile,v 1.4 2007-04-23 15:53:26 burrill Exp $
###############################################################################

ifeq ($(SCALETARGETTYPE),trips)
  AR = $(TRIPS_TOOLCHAIN_DIR)/sbin/ar
  BUILDFLAGS ?= -save-temps
  MCC = $(TRIPS_TOOLCHAIN_DIR)/sbin/tcc $(BUILDFLAGS)
else
  AR = ar
  MCC = $(CC)
endif

ODEST := $(SCALELIB)/libI77

.PHONY: libI77 clean clobber

all:
	@mkdir -p $(ODEST)
	@$(MAKE) -j 4 libI77

libI77: $(ODEST)/libI77.a

$(ODEST)/Version.o: Version.c
	@echo "Creating " $@
	@$(MCC) $(CFLAGS) -DAllow_TYQUAD -DSCALETARGET_$(SCALEHOSTTYPE) -DSCALEHOST_$(SCALEHOSTTYPE) -I.. -c $< -o $@

$(ODEST)/%.o: %.c
	@echo "Creating " $@
	@$(MCC) $(CFLAGS) -DSkip_f2c_Undefs -DAllow_TYQUAD $(CFLAGS) -DSCALETARGET_$(SCALEHOSTTYPE) -DSCALEHOST_$(SCALEHOSTTYPE) -I.. -c $< -o $@

OBJ =	$(ODEST)/Version.o \
	$(ODEST)/backspace.o \
	$(ODEST)/close.o \
	$(ODEST)/dfe.o \
	$(ODEST)/dolio.o \
	$(ODEST)/due.o \
	$(ODEST)/endfile.o \
	$(ODEST)/err.o \
	$(ODEST)/fmt.o \
	$(ODEST)/fmtlib.o \
	$(ODEST)/ftell_.o \
	$(ODEST)/iio.o \
	$(ODEST)/ilnw.o \
	$(ODEST)/inquire.o \
	$(ODEST)/lread.o \
	$(ODEST)/lwrite.o \
	$(ODEST)/open.o \
	$(ODEST)/rdfmt.o \
	$(ODEST)/rewind.o \
	$(ODEST)/rsfe.o \
	$(ODEST)/rsli.o \
	$(ODEST)/rsne.o \
	$(ODEST)/sfe.o \
	$(ODEST)/sue.o \
	$(ODEST)/typesize.o \
	$(ODEST)/uio.o \
	$(ODEST)/util.o \
	$(ODEST)/wref.o \
	$(ODEST)/wrtfmt.o \
	$(ODEST)/wsfe.o \
	$(ODEST)/wsle.o \
	$(ODEST)/wsne.o \
	$(ODEST)/xwsne.o

$(ODEST)/libI77.a:	$(OBJ)
	@echo "Creating " $@
	@ar cr $(ODEST)/libI77.a $(OBJ)
	@-ranlib $(ODEST)/libI77.a

clean:
	-rm -rf $(ODEST)

clobber:	clean
	-rm -f $(ODEST)/libI77.a

backspace.o:	fio.h
close.o:	fio.h
dfe.o:		fio.h		fmt.h
due.o:		fio.h
endfile.o:	fio.h rawio.h
err.o:		fio.h rawio.h
fmt.o:		fio.h		fmt.h
ftell_.o:	fio.h
iio.o:		fio.h		fmt.h
ilnw.o:		fio.h			lio.h
inquire.o:	fio.h
lread.o:	fio.h		fmt.h	lio.h	fp.h
lwrite.o:	fio.h		fmt.h	lio.h
open.o:		fio.h rawio.h
rdfmt.o:	fio.h		fmt.h		fp.h
rewind.o:	fio.h
rsfe.o:		fio.h		fmt.h
rsli.o:		fio.h			lio.h
rsne.o:		fio.h			lio.h
sfe.o:		fio.h
sue.o:		fio.h
uio.o:		fio.h
util.o:		fio.h
wref.o:		fio.h		fmt.h		fp.h
wrtfmt.o:	fio.h		fmt.h
wsfe.o:		fio.h		fmt.h
wsle.o:		fio.h		fmt.h	lio.h
wsne.o:		fio.h			lio.h
xwsne.o:	fio.h		fmt.h	lio.h	

$(ODEST)/libI77.ann:
