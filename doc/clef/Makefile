include $(SCALEHOME)/$(SCALERELEASE)/scale/Makefile.defs

VPATH := if:nodes:build:nodes/pictures

TEXT	:= *.tex if/*.tex # nodes/*.tex
IF_HIER	:= $(subst edot,eps,$(wildcard if/*.edot))
#HIER	:= $(subst edot,eps,$(wildcard nodes/*.edot))
HIER	:= 
#PICTURES:= $(subst edot,eps,$(wildcard nodes/pictures/*.edot))
PICTURES:= 

#--- Specify targets which are simple to type.
book:	clef.ps			# Generate the entire book
clef:	clef.ps			# Generate the entire book

#--- I'm too lazy to type ghostview.
show:
	ghostview -fg navyblue -bg gray80 clef.ps &

#--- Main building rules
clef.ps: clef.dvi
	@-dvips clef -o clef.ps
	@-echo Paper is complete.

clef.dvi: $(TEXT) $(IF_HIER) $(HIER) $(PICTURES)
	latex clef.tex		# Latex the book once
	makeindex clef		# Make the index for the book
	bibtex clef		# Generate the bibliography
	latex clef.tex		# Run Latex again to correct references
	latex clef.tex		# Run one more time to fix bibtex refs
	@-echo Dvi has been generated.

#--- Rules for building hierarchy pictures.

# The conversion from .ps to .eps used to be performed by "convert":
#        convert -density 300x300 hierarchy.ps hierarchy.eps
# However, "convert" produces portable bit maps, which are too big.
# Therefore, we now use a dirty trick which inserts sufficient
# information for the .ps to be a .eps.
%.eps: %.ps setup.h
	cp epsf_header.txt $@
	cat $< >> $@

# The 1.0 version of DOT seg faults on the Alpha - must use Mips version
%.ps: %.dot %.edot setup.h
	$(DOT) -Tps $< -o $@

%.dot: %.edot setup.h
	$(CPP) $< > $@

#--- Rules for building transparencies (depracated)
slides: slides.tex
	latex slides.tex
	dvips slides
	ghostview -fg black -bg gray60 -swap slides.ps &
	-echo Slides are complete.

#--- Rules for cleaning up the directory structure
clean: 
	-rm *.aux *.bbl *.blg *.dvi *.idx *.ilg *.ind *.log *.toc
	cd if; rm if_hier*.eps if_hier_decl.dot

veryclean: clean
	-rm *.ps
