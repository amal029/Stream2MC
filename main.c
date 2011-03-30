#include<stdio.h>
#include<stdlib.h>
#include<unistd.h>
#include<string.h>


/**
   @author Avinash Malik
   @date March 29/2011 

   This function wraps the whole Java virtual machine invocation and
   corresponding optional arguments
 */

void
help(void){
  fprintf(stderr,"Usage: stream2mc -x <max-java-memory> [will be used as mega-bytes] -f <gxl-file-name> [each file is given as a -f argument]\n");
  exit(EXIT_FAILURE);
}

const char *
getCommand(const char *gxlFile,const char* x){
  char command[2556];
  snprintf(command,2555,"java %s -cp %s org/IBM/createMcModel -DcompilerStageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2,org.IBM.compilerStage3,org.IBM.compilerStage4 %s\n",x,getenv("CLASSPATH"),gxlFile);
  return strdup(command);
}

int
main(int argc, char *argv[]){
  int opt=-1;
  char *x = "-Xmx1G";
  char *mx,*gxlFile;
  fprintf(stderr,"-------------------------Stream graph to Uppaal Model checking compiler-------------------------\n");
  fprintf(stderr,"-------------------------Author: Avinash Malik <avimalik@ie.ibm.com>----------------------------\n");
  if(argc < 2)
    help();
  while((opt = getopt(argc,argv,"h:x:f:"))!=-1){
    switch(opt){
    case 'h':
      help();
      break;
    case 'x':
      mx = strdup(optarg);
      x = calloc(256,sizeof(char));
      snprintf(x,255,"-Xmx%sM",mx);
      break;
    case 'f':
      gxlFile = strtok(optarg," ");
      //Call the Java function
      if(gxlFile == NULL)
	help();
      //Make the command string
      const char *command = getCommand(gxlFile,x);
      if(system(command)==-1)
	exit(EXIT_FAILURE);
      else printf ("%s\n",gxlFile);
      while((gxlFile = strtok(NULL," "))!=NULL){
	const char *command = getCommand(gxlFile,x);
	if(system(command)==-1)
	  exit(EXIT_FAILURE);
	else printf ("%s\n",gxlFile);
      }
      break;
    default:
      help();
    }
  }
  fprintf(stderr,"---------All generated Uppaal (.xml) files are placed in the output named directory-------\n");
}
