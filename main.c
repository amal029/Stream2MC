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
#define VERSION "0.1"

void
version(const char *v){
  fprintf(stderr,"version %s\n", v);
}

void
help(void){
  fprintf(stderr,
	  "Usage: stream2mc -d <div-factor> \n -x <max-java-memory> [will be used as mega-bytes] \n -f <gxl-file-name> [each file is given as a -f argument]\n"
	  );
  fprintf(stderr," -h for help\n");
  fprintf(stderr," -v for version\n");
  exit(EXIT_FAILURE);
}

const char *
getCommand(const char *gxlFile,const char* x,const long divFactor){
  char command[2556];
  snprintf(command,2555,"java %s -cp %s org/IBM/createMcModel -DcompilerStageFiles=org.IBM.compilerStage1,org.IBM.compilerStage2,org.IBM.compilerStage3,org.IBM.compilerStage4 -DdivFactor=%ld %s\n",x,getenv("CLASSPATH"),divFactor,gxlFile);
  return strdup(command);
}

unsigned long
getdivFactor(const char *arg){
  return ((unsigned long)atol(arg));
}

int
main(int argc, char *argv[]){
  int opt=-1;
  char *x = "-Xmx1G";
  char *mx,*gxlFile;
  unsigned char optt = 0;
  unsigned long divFactor = 1;
  while((opt = getopt(argc,argv,"d:vhx:f:"))!=-1){
    optt=1;
    switch(opt){
    case 'v':
      version(VERSION);
      break;
    case 'h':
      help();
      break;
    case 'd':
      divFactor = getdivFactor(optarg);
      break;
    case 'x':
      mx = strdup(optarg);
      x = calloc(256,sizeof(char));
      snprintf(x,255,"-Xmx%sM",mx);
      break;
    case 'f':
      fprintf(stderr,"-------------------------Stream graph to Uppaal Model checking compiler-------------------------\n");
      fprintf(stderr,"-------------------------Author: Avinash Malik <avimalik@ie.ibm.com>----------------------------\n");
      gxlFile = strtok(optarg," ");
      //Call the Java function
      if(gxlFile == NULL)
	help();
      //Make the command string
      const char *command = getCommand(gxlFile,x,divFactor);
      if(system(command)==-1)
	exit(EXIT_FAILURE);
      else printf ("%s\n",gxlFile);
      while((gxlFile = strtok(NULL," "))!=NULL){
	const char *command = getCommand(gxlFile,x,divFactor);
	if(system(command)==-1)
	  exit(EXIT_FAILURE);
	else printf ("%s\n",gxlFile);
      }
      fprintf(stderr,"---------All generated Uppaal (.xml) files are placed in the output named directory-------\n");
      break;
    default:
      help();
    }
  }
  if(!optt)
    help();
}
