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
# $Id: Makefile,v 1.3 2006-09-10 20:15:53 burrill Exp $
###############################################################################

ifeq ($(SCALETARGETTYPE),trips)
  AR = $(TRIPS_TOOLCHAIN_DIR)/sbin/ar
  BUILDFLAGS ?= -save-temps
  MCC = $(TRIPS_TOOLCHAIN_DIR)/sbin/tcc $(BUILDFLAGS)
else
  AR = ar
  MCC = $(subst -Xc,,$(CC))
endif

ODEST := $(SCALELIB)/libscale

.PHONY: libscale clean clobber

all:
	@mkdir -p $(ODEST)
	@$(MAKE) -j 4 libscale
	@$(MAKE) $(ODEST)/libscale.ann

libscale: $(ODEST)/libscale.a

$(ODEST)/%.o: %.c
	@echo "Creating " $@
	@$(MCC) $(CFLAGS) -DSCALETARGET_$(SCALETARGETTYPE) -DSCALEHOST_$(SCALEHOSTTYPE) -I.. -c $< -o $@

OBJ =	$(ODEST)/pflib.o \
	$(ODEST)/fio_backspace.o \
	$(ODEST)/fio_close.o \
	$(ODEST)/fio_dofio.o \
	$(ODEST)/fio_dolio.o \
	$(ODEST)/fio_douio.o \
	$(ODEST)/fio_endfile.o \
	$(ODEST)/fio_erdfe.o \
	$(ODEST)/fio_erdue.o \
	$(ODEST)/fio_ersfe.o \
	$(ODEST)/fio_ersfi.o \
	$(ODEST)/fio_ersle.o \
	$(ODEST)/fio_ersli.o \
	$(ODEST)/fio_ersue.o \
	$(ODEST)/fio_ewdfe.o \
	$(ODEST)/fio_ewdue.o \
	$(ODEST)/fio_ewsfe.o \
	$(ODEST)/fio_ewsfi.o \
	$(ODEST)/fio_ewsle.o \
	$(ODEST)/fio_ewsli.o \
	$(ODEST)/fio_ewsue.o \
	$(ODEST)/fio_inquire.o \
	$(ODEST)/fio_open.o \
	$(ODEST)/fio_rewind.o \
	$(ODEST)/fio_srdfe.o \
	$(ODEST)/fio_srdue.o \
	$(ODEST)/fio_srsfe.o \
	$(ODEST)/fio_srsfi.o \
	$(ODEST)/fio_srsle.o \
	$(ODEST)/fio_srsli.o \
	$(ODEST)/fio_srsue.o \
	$(ODEST)/fio_swdfe.o \
	$(ODEST)/fio_swdue.o \
	$(ODEST)/fio_swsfe.o \
	$(ODEST)/fio_swsfi.o \
	$(ODEST)/fio_swsle.o \
	$(ODEST)/fio_swsli.o \
	$(ODEST)/fio_swsue.o \
	$(ODEST)/scale_CDABS.o \
	$(ODEST)/scale_abs.o \
	$(ODEST)/scale_absc.o \
	$(ODEST)/scale_cabs.o \
	$(ODEST)/scale_dabs.o \
	$(ODEST)/scale_absl.o \
	$(ODEST)/scale_absll.o \
	$(ODEST)/scale_absz.o \
	$(ODEST)/scale_addcc.o \
	$(ODEST)/scale_addzz.o \
	$(ODEST)/scale_aimagc.o \
	$(ODEST)/scale_aimagz.o \
	$(ODEST)/scale_aint.o \
	$(ODEST)/scale_alloca.o \
	$(ODEST)/scale_anint.o \
	$(ODEST)/scale_atan2f.o \
	$(ODEST)/scale_conjgc.o \
	$(ODEST)/scale_conjgz.o \
	$(ODEST)/scale_cosc.o \
	$(ODEST)/scale_ccos.o \
	$(ODEST)/scale_coshf.o \
	$(ODEST)/scale_cosz.o \
	$(ODEST)/scale_createcomplex.o \
	$(ODEST)/scale_createdoublecomplex.o \
	$(ODEST)/scale_ddim.o \
	$(ODEST)/scale_divcc.o \
	$(ODEST)/scale_divzz.o \
	$(ODEST)/scale_dprod.o \
	$(ODEST)/scale_dsign.o \
	$(ODEST)/scale_expc.o \
	$(ODEST)/scale_cexp.o \
	$(ODEST)/scale_expect.o \
	$(ODEST)/scale_expz.o \
	$(ODEST)/scale_ffs.o \
	$(ODEST)/scale_fprintf.o \
	$(ODEST)/scale_fputc.o \
	$(ODEST)/scale_fputs.o \
	$(ODEST)/scale_fwrite.o \
	$(ODEST)/scale_idim.o \
	$(ODEST)/scale_imaxabs.o \
	$(ODEST)/scale_index.o \
	$(ODEST)/scale_isign.o \
	$(ODEST)/scale_labs.o \
	$(ODEST)/scale_llabs.o \
	$(ODEST)/scale_log10f.o \
	$(ODEST)/scale_logc.o \
	$(ODEST)/scale_clog.o \
	$(ODEST)/scale_logz.o \
	$(ODEST)/scale_memcpy.o \
	$(ODEST)/scale_memset.o \
	$(ODEST)/scale_moddd.o \
	$(ODEST)/scale_modff.o \
	$(ODEST)/scale_modii.o \
	$(ODEST)/scale_modll.o \
	$(ODEST)/scale_moveWords.o \
	$(ODEST)/scale_multcc.o \
	$(ODEST)/scale_multzz.o \
	$(ODEST)/scale_negatec.o \
	$(ODEST)/scale_negatez.o \
	$(ODEST)/scale_nint.o \
	$(ODEST)/scale_pause.o \
	$(ODEST)/scale_pow_zi.o \
	$(ODEST)/scale_pow_zz.o \
	$(ODEST)/scale_powcc.o \
	$(ODEST)/scale_powci.o \
	$(ODEST)/scale_powdd.o \
	$(ODEST)/scale_powdi.o \
	$(ODEST)/scale_powdl.o \
	$(ODEST)/scale_powff.o \
	$(ODEST)/scale_powfi.o \
	$(ODEST)/scale_powii.o \
	$(ODEST)/scale_powil.o \
	$(ODEST)/scale_powli.o \
	$(ODEST)/scale_powll.o \
	$(ODEST)/scale_powzi.o \
	$(ODEST)/scale_powzz.o \
	$(ODEST)/scale_prefetch.o \
	$(ODEST)/scale_printf.o \
	$(ODEST)/scale_putchar.o \
	$(ODEST)/scale_puts.o \
	$(ODEST)/scale_return_address.o \
	$(ODEST)/scale_rindex.o \
	$(ODEST)/scale_sassign.o \
	$(ODEST)/scale_sassignc.o \
	$(ODEST)/scale_sassigncp.o \
	$(ODEST)/scale_sassignp.o \
	$(ODEST)/scale_scmp.o \
	$(ODEST)/scale_setjmp.o \
	$(ODEST)/scale_sinc.o \
	$(ODEST)/scale_csin.o \
	$(ODEST)/scale_sindex.o \
	$(ODEST)/scale_sinhf.o \
	$(ODEST)/scale_sinz.o \
	$(ODEST)/scale_sqrtc.o \
	$(ODEST)/scale_csqrt.o \
	$(ODEST)/scale_sqrtz.o \
	$(ODEST)/scale_stop.o \
	$(ODEST)/scale_strcat.o \
	$(ODEST)/scale_strchr.o \
	$(ODEST)/scale_strcmp.o \
	$(ODEST)/scale_strcpy.o \
	$(ODEST)/scale_strcspn.o \
	$(ODEST)/scale_strlen.o \
	$(ODEST)/scale_strncat.o \
	$(ODEST)/scale_strncmp.o \
	$(ODEST)/scale_strncpy.o \
	$(ODEST)/scale_strpbrk.o \
	$(ODEST)/scale_strrchr.o \
	$(ODEST)/scale_strspn.o \
	$(ODEST)/scale_strstr.o \
	$(ODEST)/scale_subcc.o \
	$(ODEST)/scale_subzz.o \
	$(ODEST)/scale_tanhf.o \
	$(ODEST)/scale_trap.o \
	$(ODEST)/scale_z_div.o

$(ODEST)/libscale.a:	$(OBJ)
	@echo "Creating " $@
	@$(AR) crs $(ODEST)/libscale.a $(OBJ)

clean:
	-rm -rf $(ODEST)

clobber: clean

$(ODEST)/libscale.ann: $(patsubst %.o,%.ann,$(wildcard $(ODEST)/*.o))
	@echo "Creating " $@
	@rm -f $(ODEST)/libscale.ann
	@cat $(ODEST)/*.ann >$(ODEST)/libscale.ann

$(ODEST)/%.ann: %.c
	@echo "Creating " $@
	@awk '/PURE / {print "scale.clef.PureFunctionAnnotation scale.clef.decl.RoutineDecl " $$3 " " $$4 ";"}' $^ >$@
