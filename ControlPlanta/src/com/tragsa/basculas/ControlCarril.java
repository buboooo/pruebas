package com.tragsa.basculas;

import java.io.IOException;
import java.util.Hashtable;

import com.serotonin.modbus4j.exception.ModbusInitException;

public class ControlCarril {

	public ControlCarril() {
		
	}
	
	/**
	 * Una matrícula entra a ocupar el carril con báscula. Hasta que no termine todo el proceso, 
	 * no podrá entrar otra matrícula. Levanta barrera, espera peso y vuelve a levantar barrera.
	 * 
	 * @param matricula
	 * @param idCamara
	 * @param logMatricula
	 * @param tipoVehiculo si es vehiculo de una pesada o es articulado de dos pesadas como máximo
	 * @throws Exception 
	 */
	
	public synchronized float procesandoCarrilBarreraBascula(int idCarril, String matricula, 
			int idCamara,boolean matriculaNoRegistrada, String logMatricula) throws Exception   {
		
		int retrasoMilis = 0;
		float peso = 0;
	
		Hashtable<String, String> atributosCamara = new Hashtable<String, String>();
		atributosCamara = ControlPlanta.tipoCamara.get(idCamara);

		// obtenemos los nombres de los dispositivos asociados a la camara
		String barreraDis = atributosCamara.get("barrera");
		String nombreDispositivo = atributosCamara.get("bascula");
		
		/**************************************************************
		 * 1 levantaBarrera
		 */	
		levantaBarrera(barreraDis, matricula, idCamara, logMatricula, retrasoMilis);		
		ControlPlanta.log.crearLog(
				"    ******** Levanta barrera de entrada: "+ barreraDis, logMatricula, matricula, idCamara);
	
		/**************************************************************
		 * 2 espera peso de la báscula
		 */
		Bascula bascula = ControlPlanta.hbasculas.get(nombreDispositivo);	
		
		try {
			
			peso = bascula.esperarPeso(matricula,logMatricula, idCamara);
			//peso = bascula.esperarPesoArticulado(matricula,logMatricula, idCamara,1000);
		} catch (Exception ex) {
			throw ex;
		}
			
		ControlPlanta.log.crearLog("    ****** Peso detectado para almacenar: "+ 
				peso, logMatricula, matricula, idCamara);
		
		return peso;
	}
	
	public synchronized float procesandoCarrilBarreraBasculaArticulado(int idCarril, String matricula, 
			int idCamara, boolean matriculaNoRegistrada, String logMatricula) throws Exception   {
		
		int retrasoMilis = 0;
		float peso = 0;
	
		Hashtable<String, String> atributosCamara = new Hashtable<String, String>();
		atributosCamara = ControlPlanta.tipoCamara.get(idCamara);

		// obtenemos los nombres de los dispositivos asociados a la camara
		String barreraDis = atributosCamara.get("barrera");
		String nombreDispositivo = atributosCamara.get("bascula");
		
		/**************************************************************
		 * 1 levantaBarrera  ENTRADA
		 */
		//levanta la barreda entrada al carril
		levantaBarrera(barreraDis, matricula, idCamara, logMatricula, retrasoMilis);	
		ControlPlanta.log.crearLog(
				"    ******** Levanta barrera de entrada: "+ barreraDis, logMatricula, matricula, idCamara);
	
				
		//Buscamos el id de la otra camara de este carril
		int otraCamaraCarril = Integer.parseInt(atributosCamara.get("camaraContraria"));
		Hashtable<String, String> atributosCamaras = ControlPlanta.tipoCamara.get(otraCamaraCarril);
		barreraDis = atributosCamaras.get("barrera");
		nombreDispositivo = atributosCamara.get("bascula");

		/**************************************************************
		 * 3 levantaBarrera de SALIDA 
		 * 
		 */
		retrasoMilis = 0;  //tiempo de espera antes de abrir la barrera
		if ( (otraCamaraCarril != 0) && !(barreraDis.equals("")) )
			levantaBarrera(barreraDis, matricula, otraCamaraCarril, logMatricula, retrasoMilis);

		ControlPlanta.log.crearLog(
					"    ******** Levanta barrera de salida: "+ barreraDis, logMatricula, matricula, idCamara);
		
		
		/**************************************************************
		 * 2 espera peso de la báscula
		 */
		Bascula bascula = ControlPlanta.hbasculas.get(nombreDispositivo);	
		
		try {
			peso = bascula.esperarPesoArticulado(matricula,logMatricula, idCamara,1000);
		} catch (Exception ex) {
			throw ex;
		}
			
		ControlPlanta.log.crearLog("    ****** Peso detectado para almacenar: "+ 
				peso, logMatricula, matricula, idCamara);
		
		return peso;
	}
	
	/**
	 * Cuando el vehículo ya ha pesado, entra en esta función para esperar su salida.
	 * Cuando cumpla que no existe peso y que la barrera está cerrada será cuando el carril
	 * 		está desocupado.
	 * @param idCamara
	 * @param matricula
	 * @param logMatricula
	 * @throws Exception
	 */
	public synchronized void esperaSalidaCarrilArticulado(int idCamara, String matricula, 
			String logMatricula) throws Exception {
		
		Hashtable<String, String> atributosCamara = new Hashtable<String, String>();
		atributosCamara = ControlPlanta.tipoCamara.get(idCamara);

		// obtenemos los nombres de los dispositivos asociados a la camara
		String barreraDis = atributosCamara.get("barrera");
		String nombreDispositivo = atributosCamara.get("bascula");
		
		/**************************************************************
		 * 4 Se espera a tener un peso estable cercano a cero y barrera de salida bajada
		 * 
		 */
		Bascula bascula = ControlPlanta.hbasculas.get(nombreDispositivo);
//		float pesoSalida = 0;
//		boolean sigue = true;
//		while (sigue) {
//			pesoSalida = bascula.esperarPeso(matricula,logMatricula, idCamara);
//			if ( (estadoBarrera(barreraDis).equals("bajada")) && !(pesoSalida > ControlPlanta.pesoMinimoHayAlgo) ) {
//				sigue = false;
//			}
//		}
		
		bascula.esperarPesoCero(matricula, logMatricula, idCamara);
	}
	
	/**
	 * Cuando el vehículo ya ha pesado, entra en esta función para esperar su salida.
	 * Cuando cumpla que no existe peso y que la barrera está cerrada será cuando el carril
	 * 		está desocupado.
	 * @param idCamara
	 * @param matricula
	 * @param logMatricula
	 * @throws Exception
	 */
	public synchronized void esperaSalidaCarril(int idCamara, String matricula, 
			String logMatricula) throws Exception {
		int retrasoMilis = 0;
		
		/**************************************************************
		 *  Calculamos el id de la otra cámara y su barrera (nombre dispositivo)
		 */
		int otraCamaraCarril = 0;
		String barreraDis = "";
		String nombreDispositivo="";
		
		Hashtable<String, String> atributosCamara = new Hashtable<String, String>();
		atributosCamara = ControlPlanta.tipoCamara.get(idCamara);
		
		//Buscamos el id de la otra camara de este carril
		otraCamaraCarril = Integer.parseInt(atributosCamara.get("camaraContraria"));
		Hashtable<String, String> atributosCamaras = ControlPlanta.tipoCamara.get(otraCamaraCarril);
		barreraDis = atributosCamaras.get("barrera");
		nombreDispositivo = atributosCamara.get("bascula");

		/**************************************************************
		 * 3 levantaBarrera de salida tras un tiempo de espera
		 * 
		 * Cuando se tenga peso estable se ordena levantar la barrera de salida
		 */
		retrasoMilis = 2000;  //tiempo de espera antes de abrir la barrera
		if ( (otraCamaraCarril != 0) && !(barreraDis.equals("")) )
			levantaBarrera(barreraDis, matricula, otraCamaraCarril, logMatricula, retrasoMilis);

		ControlPlanta.log.crearLog(
					"    ******** Levanta barrera de salida: "+ barreraDis, logMatricula, matricula, idCamara);
		
		/**************************************************************
		 * 4 Se espera a tener un peso estable cercano a cero y barrera de salida bajada
		 * 
		 */
		Bascula bascula = ControlPlanta.hbasculas.get(nombreDispositivo);
		
		bascula.esperarPesoCero(matricula, logMatricula, idCamara);
		
	}
	
	/**
	 * Cuando solo hay barrera tras la cámara y no hay peso.
	 * @param idCarril
	 * @param matricula
	 * @param idCamara
	 * @param logMatricula
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ModbusInitException 
	 */
	public synchronized void procesandoSoloBarrera(int idCarril, String matricula, int idCamara, 
			String logMatricula) throws IOException, InterruptedException, ModbusInitException {
		int retrasoMilis = 0;
		
		Hashtable<String, String> atributosCamara = new Hashtable<String, String>();
		atributosCamara = ControlPlanta.tipoCamara.get(idCamara);

		// obtenemos los nombres de los dispositivos asociados a la camara
		String barreraDis = atributosCamara.get("barrera");
		//String nombreDispositivo = atributosCamara.get("bascula");
		
		/**************************************************************
		 * 1 levantaBarrera
		 */
		//levanta la barreda entrada al carril
		levantaBarrera(barreraDis, matricula, idCamara, logMatricula, retrasoMilis);	
		
	}
	
	/**
	 * Comprueba que el estado de la barrera esté bajada
	 * @param barrera
	 * @return
	 * @throws IOException 
	 * @throws ModbusInitException 
	 */
	private String estadoBarrera(String barrera) throws IOException, ModbusInitException {
		String estado = "bajada";
		
		//Descomentar para producción
		
//		 Omron au = new Omron(barrera);
//		
//		 try {
//			au.conectar();
//			if ( au.estadoBarrera().equals("valorBajada") )
//				estado = "bajada";
//			ControlPlanta.log.crearLog("Estado: " + au.estadoBarrera(),"","",-1);
//
//			au.desconectar();
//			
//		} catch (ModbusInitException e) {
//			ControlPlanta.log.crearLog("Error bascula: "+ barrera +". Descripción: " + au.estadoBarrera(),"","",-1);
//			throw e;
//		}
//		 
		
		return estado;
	}
	
	/**
	 * Levanta la barrera llamando a las librerias Omron
	 * 
	 * @author Juan Carlos Muñoz
	 * @param barrera  es el nombre del dispositivo de barrera almacenado en el
	 *            archivo Automata.properties, ejemplo "OMRON_01"
	 * @param logMatricula guardará solo en el log información de esta matricula, si está 
	 * 				en blanco lo hará de todas las matriculas.           
	 * @version: 31-3-2014
	 * @throws IOException 
	 * @throws ModbusInitException 
	 * @throws InterruptedException
	 */
	private synchronized void levantaBarrera(String barrera,
			String matricula, int idCamara, String logMatricula, int retrasoMilis) 
					throws IOException, ModbusInitException {
		//Descomentar para producción
		//simula levantar barrera
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//		 Omron au = new Omron(barrera);
//		
//		 try {
//			au.conectar();
//			
//			ControlPlanta.log.crearLog("Subiendo barrera: "+ barrera,"","",-1);
//			 au.subirBarrera();
//			
//			 //au.bajarBarrera();
//			 ControlPlanta.log.crearLog("Estado: " + au.estadoBarrera(),"","",-1);
//			 
//			 au.desconectar();
//		} catch (ModbusInitException e) {
//			throw e;
//		}
		
		 
		
	} // fin levantaBarrera()
	
	
}
