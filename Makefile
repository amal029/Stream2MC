CC=javac
CLASSPATH=.:../gxl-0.92/gxl.jar
CFALGS=-Xlint
SHELL=/bin/bash
all: clean createPArch

createPArch:
	javac -cp $(CLASSPATH) $(CFALGS) ./org/IBM/createPArch.java

run:
	java -cp .:../gxl-0.92/gxl.jar org/IBM/createPArch amal029@localhost
	gxl2dot pArch.gxl -o pArch.dot

clean:
	rm -rf org/IBM/*class *dot *gxl *~
