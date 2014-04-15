package com.tragsatec.residuos.automata;

import java.io.IOException;


public class Omron extends Automata {
	
	private String dirTelmSubida;
	private String dirTelmBajada;
	private String dirEstadoBarrera;
	private String dirEstadoDeteccion;
		
	public Omron(String idAutomata) throws IOException {
		super();

		// set the properties value
		setIp(getProp().getProperty(idAutomata + "_IP"));
		setPuerto(getProp().getProperty(idAutomata + "_PORT"));
		
		dirTelmSubida = getProp().getProperty(idAutomata + "_DIR_TELM_SUBIR");
		dirTelmBajada = getProp().getProperty(idAutomata + "_DIR_TELM_BAJAR");
		dirEstadoBarrera = getProp().getProperty(idAutomata + "_DIR_EST_SUBIDA");
		dirEstadoDeteccion  = getProp().getProperty(idAutomata + "_DIR_EST_DETECCION");
	  }
	
	
  public boolean subirBarrera() {
	return  escribirMemoriaWord(Integer.parseInt(dirTelmSubida), (short)1);
  }

  public boolean bajarBarrera() {
	return  escribirMemoriaWord(Integer.parseInt(dirTelmBajada), (short)1);
  }
  
  public String estadoBarrera() {
	  return  leerMemoriaWord(Integer.parseInt(dirEstadoBarrera)).toString();	
  }

  public boolean deteccionVehiculo() {
	  Short s = (Short)leerMemoriaWord(Integer.parseInt(dirEstadoDeteccion));	  
	  return s!=null && s.shortValue() > 0;
  }
  
}
