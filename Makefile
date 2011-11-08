SHELL=/bin/bash
CC=javac
CR=java -Xmx1G
CFALGS=-g
HN=`hostname`
TOPLEVEL=./org/IBM
DEVICEFILES=$(TOPLEVEL)/device
DECLUSTERINGFILES=$(TOPLEVEL)/declustering
HEURISTICFILE=$(TOPLEVEL)/heuristics
STATEFILE=$(TOPLEVEL)/stateGraph
ILPFILES=$(TOPLEVEL)/ILP
SRC=$(TOPLEVEL)/*.java $(DEVICEFILES)/StreamIT/*.java $(DECLUSTERINGFILES)/*.java $(ILPFILES)/*.java
SRC+=$(HEURISTICFILE)/*.java $(STATEFILE)/*.java

#This is a working example
COMPILE_FILES=../benchmarks/sexample.dot.gxl

#New tde
tde=../benchmarks/new/tde.dot.gxl
serpent=../benchmarks/new/serpent.dot.gxl
bsort=../benchmarks/new/bitonicsort.dot.gxl
mp3decoder=../benchmarks/new/mp3decoder.dot.gxl
beamformer=../benchmarks/new/beamformer.dot.gxl
audiobeam=../benchmarks/new/audiobeam.dot.gxl

#This takes too long to compile
# COMPILE_FILES=../work-after-partition.gxl

#2 core examples
benchmark12=../benchmarks/2core/audiobeam.dot.gxl #works with both uppaal and declustering
benchmark22=../benchmarks/2core/fft.dot.gxl #works with both uppaal and declustering
benchmark32=../benchmarks/2core/bitonicsort.dot.gxl #works with declustering and uppaal
benchmark42=../benchmarks/2core/vocoder.dot.gxl #Works with both uppaal and declustering
benchmark52=../benchmarks/2core/serpent_full.dot.gxl #works with declustering and uppaal
benchmark62=../benchmarks/2core/tde.dot.gxl #Works with declustering and uppaal
benchmark72=../benchmarks/2core/des.dot.gxl # works with uppaal, work with declustering
benchmark82=../benchmarks/2core/mpeg2decoder.dot.gxl # uppaal and declustering both work

#4 core examples
benchmark14=../benchmarks/4core/audiobeam.dot.gxl #works with both uppaal and declustering
benchmark24=../benchmarks/4core/fft.dot.gxl #works with both uppaal and declustering
benchmark34=../benchmarks/4core/bitonicsort.dot.gxl #works with declustering and uppaal
benchmark44=../benchmarks/4core/vocoder.dot.gxl #Works with both uppaal and declustering
benchmark54=../benchmarks/4core/serpent_full.dot.gxl #works with declustering and uppaal
benchmark64=../benchmarks/4core/tde.dot.gxl #Works with declustering and uppaal
benchmark74=../benchmarks/4core/des.dot.gxl # works with uppaal, work with declustering
benchmark84=../benchmarks/4core/mpeg2decoder.dot.gxl # uppaal and declustering both work

# CPLEX solution files
solution12=/tmp/audiobeam.sol

all: compile

compile: main
	$(CC) -cp .:$(CLASSPATH) $(CFALGS) $(SRC)

arch:
	$(CR) -cp .:$(CLASSPATH) org/IBM/createPArch amal029@localhost amal029@infinity -clf /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini -nlf /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini
	gxl2dot pArch.gxl > pArch$(HN).dot

arch2:
	$(CR) -cp .:$(CLASSPATH) org/IBM/createPArch amal029@localhost -clf /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini

arch8:
	$(CR) -cp .:$(CLASSPATH) org/IBM/createPArch amal029@localhost amal029@infinity amal029@infinity2 amal029@infinity3 -clf /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini -nlf /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini

clean:
	rm -rf org/IBM/*class $(DEVICEFILES)/StreamIT/*class $(DEVICEFILES)/StreamIT/*java~ \
	*dot *gxl *~ org/IBM/*~ ~/.cpuinfo ~/.distance* output/ stream2mc $(DECLUSTERINGFILES)/*class $(DECLUSTERINGFILES)/*java~ \
	$(ILPFILES)/*class $(ILPFILES)/*java~ $(HEURISTICFILE)/*.class $(HEURISTICFILE)/*~ $(STATEFILE)/*.class $(STATEFILE)/*~
testini:
	$(CR) -cp .:$(CLASSPATH) org/IBM/iniParser /home/amal029/Dropbox/IBM_Work/Data/socket_tcp_ip/infinity.ini

model: stage4

# The compiler gets invoked in stages. So, in the below two examples,
# you can stop after some stage. Normally, you will always require the
# first stage, because it does important things like graph flattening,
# inserting visit conditions for traversal and most importantly,
# converting generic gxl into actors and precedence relations. You can
# also add your own stage after any stage, the compiler stages are
# dynamically loaded as plugins.

# One stage pass
stage1: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel -Dstagefile=org.IBM.compilerStage1 $(COMPILE_FILES)

# Two stage compiler pass, stage-2 needs stage-1

# Note, an output named directory is created with .gxl files after every
# stage. First stage uses the arguments given on the command line. Every
# stage after that uses arguments produced by the previous stage (files
# that are produced in the output directory)

stage2: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel -DstageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2 \
	$(COMPILE_FILES)

# Third stage of the compiler needs stage-2 and stage-1
stage3: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel -DstageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2,org.IBM.compilerStage3 \
	$(COMPILE_FILES)

stage4: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel \
	-DstageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2,org.IBM.compilerStage3,org.IBM.compilerStage4 \
	$(benchmark1)

main:
	rm -f stream2mc
	gcc -std=gnu99 -g -o stream2mc main.c

declustering: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel \
	-DstageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2,org.IBM.compilerStage3,org.IBM.declustering.declusterStage1 \
	-DdivFactor=1 $(mp3decoder)

ilp: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel \
	-DstageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2,org.IBM.compilerStage3,org.IBM.ILP.ILPStage1 \
	-DdivFactor=1 $(benchmark32)

ilp2: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel \
	-DstageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2,org.IBM.compilerStage3,org.IBM.ILP.ILPStage2 \
	-DdivFactor=1 $(mp3decoder)

cplex: compile
	$(CR) -cp $(CLASSPATH) org.IBM.ILP.cplexSolParser $(solution12)

ilp3: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel \
	-DstageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2,org.IBM.compilerStage3,org.IBM.ILP.ILPStage3 \
	-DdivFactor=1 $(mp3decoder)
heuristics: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel \
	-DstageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2,org.IBM.compilerStage3,\
	org.IBM.heuristics.stage1 -DdivFactor=1 $(COMPILE_FILES)
