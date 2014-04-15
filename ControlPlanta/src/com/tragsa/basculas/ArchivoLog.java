package com.tragsa.basculas;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Clase para crear registros en un fichero log.txt externo, para poder ejecutar
 * la aplicación en modo debug y recibir información
 * @author Juan Carlos Muñoz
 * @param Operacion es un texto que se va a registrar en el fichero y mostrar en system.out 
 * @version: 14-3-2014 
 */
public class ArchivoLog  {
    FileWriter archivo; //nuestro archivo log
    
    private final static Logger logs = Logger.getLogger(ControlPlanta.class .getName());

    public ArchivoLog() {
    	try {
    		LogManager.getLogManager().readConfiguration(
				new FileInputStream("./log.properties"));
    	}catch (Exception e)
    	{
    		System.out.println("Error al abrir archivo log.properties");
    		System.exit(-1);
    	}
    }
    /**
     * 
     * @param Operacion
     * @param logMatricula si se establece, solo se mostraran los log asociados a dicha matricula
     * @param Matricula es el campo de la matricula actual.
     * @param idCamara es la camara que se está tratando en ese momento.
     * @throws IOException
     */
    public void crearLog(String Operacion, String logMatricula, String matricula, int idCamara) {
    	boolean registra = false;
   	
    	//tiene capacidad de registrar un log para una sola matrícula
    	// ver como pasar los parámetros a la función main() debug matricula 774433CRW
    	if (!ControlPlanta.isDebug)
    		return;
    	
       if (logMatricula != "")
       {
    	   if (logMatricula.equals(matricula))
    		   registra = true;
       }
       else
    	   registra = true;
    		   
       if (registra==false)
    	   return;
       
       if (idCamara != -1)
    	   Operacion = Operacion + "  del id cámara: " + idCamara + ". En matricula: " + matricula;
       
       logs.info(Operacion);
       
       
       
       /*
        * si se quiere crear un archivo distinto al de log.properties
        */
       
//       //Pregunta el archivo existe, caso contrario crea uno con el nombre log.txt
//       if (new File("log.txt").exists()==false){
//    	   archivo=new FileWriter(new File("log.txt"),false);
//    	   }
//       
//            archivo = new FileWriter(new File("log.txt"), true);
//            Calendar fechaActual = Calendar.getInstance(); //Para poder utilizar el paquete calendar
//            
//            String logstr = "\r\n" + Operacion;
//            if (idCamara != -1)
//            	logstr = logstr +  "  del id cámara: " + idCamara;
//            
//            System.out.println(logstr);
//            
//            //Empieza a escribir en el archivo
//            archivo.write((String.valueOf(fechaActual.get(Calendar.DAY_OF_MONTH))
//                 +"/"+String.valueOf(fechaActual.get(Calendar.MONTH)+1)
//                 +"/"+String.valueOf(fechaActual.get(Calendar.YEAR))
//                 +";"+String.valueOf(fechaActual.get(Calendar.HOUR_OF_DAY))
//                 +":"+String.valueOf(fechaActual.get(Calendar.MINUTE))
//                 +":"+String.valueOf(fechaActual.get(Calendar.SECOND)))+";"+logstr+"\r\n");
//            archivo.close(); //Se cierra el archivo
            
           
    }//Fin del metodo crearLog
   

}//Fin de la clase