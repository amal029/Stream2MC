SHELL=/bin/bash
CC=javac
CFALGS=-Xlint
HN=`hostname`

all: clean compile

compile:
	javac -cp $(CLASSPATH) $(CFALGS) ./org/IBM/*.java

run:
	java -cp $(CLASSPATH) org/IBM/createPArch avinash@escc3 avinash@escc4 -clf /home/avinash/tutu /home/avinash/tutu -nlf /home/avinash/tutu /home/avinash/tutu
	gxl2dot pArch.gxl -o pArch$(HN).dot

clean:
	rm -rf org/IBM/*class *dot *gxl *~ org/IBM/*~
