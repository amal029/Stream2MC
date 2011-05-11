SHELL=/bin/bash
CC=javac
CR=java
CFALGS=-Xlint -g
HN=`hostname`
TOPLEVEL=./org/IBM
DEVICEFILES=$(TOPLEVEL)/device
DECLUSTERINGFILES=$(TOPLEVEL)/declustering
SRC=$(TOPLEVEL)/*.java $(DEVICEFILES)/StreamIT/*.java $(DECLUSTERINGFILES)/*.java

#This is a working example
#COMPILE_FILES=../sample_stream-graph2.gxl

#This takes too long to compile
# COMPILE_FILES=../work-after-partition.gxl

benchmark1=../benchmarks/audiobeam.dot.gxl #works with both uppaal and declustering
benchmark2=../benchmarks/fft.dot.gxl #works with both uppaal and declustering
benchmark3=../benchmarks/bitonicsort.dot.gxl #works with declustering and uppaal
benchmark4=../benchmarks/vocoder.dot.gxl #Works with both uppaal and declustering
benchmark5=../benchmarks/serpent_full.dot.gxl #works with declustering and uppaal
benchmark6=../benchmarks/tde.dot.gxl #Works with declustering and uppaal
benchmark7=../benchmarks/des.dot.gxl # works with uppaal, work with declustering
benchmark8=../benchmarks/mpeg2decoder.dot.gxl # uppaal and declustering both work

all: compile

compile: main
	$(CC) -cp $(CLASSPATH) $(CFALGS) $(SRC)

arch:
	$(CR) -cp $(CLASSPATH) org/IBM/createPArch amal029@localhost amal029@infinity -clf /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini -nlf /home/amal029/Dropbox/IBM_Work/Data/socket_tcp_ip/infinity.ini /home/amal029/Dropbox/IBM_Work/Data/socket_tcp_ip/infinity.ini
	gxl2dot pArch.gxl -o pArch$(HN).dot

arch2:
	$(CR) -cp $(CLASSPATH) org/IBM/createPArch amal029@localhost -clf /home/amal029/Dropbox/IBM_Work/Data/pthreads_mutexes/B2.ini

clean:
	rm -rf org/IBM/*class $(DEVICEFILES)/StreamIT/*class $(DEVICEFILES)/StreamIT/*java~ \
	*dot *gxl *~ org/IBM/*~ ~/.cpuinfo ~/.distance* output/ stream2mc $(DECLUSTERINGFILES)/*class $(DECLUSTERINGFILES)/*java~

testini:
	$(CR) -cp $(CLASSPATH) org/IBM/iniParser /home/amal029/Dropbox/IBM_Work/Data/socket_tcp_ip/infinity.ini

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
	-DdivFactor=1 $(benchmark1)
