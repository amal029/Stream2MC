SHELL=/bin/bash
CC=javac
CR=java
CFALGS=-Xlint -g
HN=`hostname`

all: clean compile

compile:
	$(CC) -cp $(CLASSPATH) $(CFALGS) ./org/IBM/*.java

run:
	$(CR) -cp $(CLASSPATH) org/IBM/createPArch amal029@localhost amal029@infinity -clf /home/amal029/clf1 /home/amal029/clf2 -nlf /home/amal029/nlf1 /home/amal029/nlf2
	gxl2dot pArch.gxl -o pArch$(HN).dot

clean:
	rm -rf org/IBM/*class *dot *gxl *~ org/IBM/*~ ~/.cpuinfo ~/.distance* output/

test_ini:
	$(CR) -cp $(CLASSPATH) org/IBM/iniParser core.ini 

# The compiler gets invoked in stages. So, in the below two examples,
# you can stop after some stage. Normally, you will always require the
# first stage, because it does important things like graph flattening,
# inserting visit conditions for traversal and most importantly,
# converting generic gxl into actors and precedence relations. You can
# also add your own stage after any stage, the compiler stages are
# dynamically loaded as plugins.

# One stage pass
stage1: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel -Dstagefile=org.IBM.compilerStage1 ../sample_stream-graph2.gxl

# Two stage compiler pass, stage-2 needs stage-1

# Note, an output named directory is created with .gxl files after every
# stage. First stage uses the arguments given on the command line. Every
# stage after that uses arguments produced by the previous stage (files
# that are produced in the output directory)

stage2: compile
	$(CR) -cp $(CLASSPATH) org/IBM/createMcModel -DstageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2 ../sample_stream-graph2.gxl
