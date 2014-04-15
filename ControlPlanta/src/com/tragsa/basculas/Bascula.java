package com.tragsa.basculas;

import java.io.IOException;

import com.tragsatec.residuos.indicador.OrionPlus;

public class Bascula {

	private OrionPlus op;
	private String nombreDispositivo = "";

	Bascula(String dispositivo) throws Exception, IOException {
		this.nombreDispositivo = dispositivo;

		try {
			op = new OrionPlus(dispositivo);
		} catch (Exception ex) {
			throw new Exception(
					"Clase Bascula. Funcion Bascula(). Error al concetar con dispositivo bascula, nombre dispositivo: "
							+ nombreDispositivo);
		}
		
	}

	public String getNombreDispositivo() {
		return nombreDispositivo;
	}


	public synchronized float esperarPeso(String matricula, 
			String logMatricula, int idCamara) throws Exception {

//		Float peso = (float) 0;
//		try {
//			if (ControlPlanta.isDebug)
//				op.setDebug(true);
//			else
//				op.setDebug(false);
//			
//			op.conectar();
//			peso = op.generarMedidaEstableKgMax();
//			
//			op.desconectar();
//			
//		}  catch (Exception ex) {
//			ControlPlanta.log.crearLog("*******************************Error al llamar op.generarMedidaEstableKgMax(). Mensaje: "
//							+ ex.getMessage(), logMatricula, matricula,	idCamara);
//			throw ex;
//		}
//		
//		/*
//		 * op.setCabecera(1, op.estiloTicket, " CAAM "); op.setCabecera(2,
//		 * op.estiloTitulo, "Tragsatec"); op.setCabecera(3, op.estiloNormal,
//		 * "Matricula: MU9097Y"); op.setCabecera(4, op.estiloNormal,
//		 * "Origen: Alcantarilla"); op.setCabecera(5, op.estiloPieSubtitulo,
//		 * "ENTRADA MATERIAL"); op.setCabecera(6, op.estiloPieNormal,
//		 * "Pruebas");
//		 * 
//		 * op.generaTicket();
//		 */
//		return peso;
		
		return 1700;
	}

		
	public synchronized float esperarPesoArticulado(String matricula, 
			String logMatricula, int idCamara, int sleep) throws Exception {

//		Float peso = (float) 0;
//		Float peso2 = (float) 0;
//		try {
//			// Llamar a Orion
//			if (ControlPlanta.isDebug)
//				op.setDebug(true);
//			else
//				op.setDebug(false);
//			
//			op.conectar();
//			ControlPlanta.log.crearLog("Esperando primer* peso vehiculo articulado."
//							, logMatricula, matricula, idCamara);
//			peso = op.generarMedidaEstableKgMax();
//			
//			ControlPlanta.log.crearLog("Esperando peso inestable vehiculo articulado."
//					, logMatricula, matricula, idCamara);
//			
//			while (op.medicionEstable())
//			{
//				//mientras sea estable estará en bucle
//				Thread.sleep(500);
//			}
//						
//			ControlPlanta.log.crearLog("Esperando segundo** peso vehiculo articulado."
//					, logMatricula, matricula, idCamara);
//			
//			peso2 = op.generarMedidaEstableKgMax();
//			peso = peso + peso2;
//			
//			op.desconectar();
//			
//		}  catch (Exception ex) {
//			ControlPlanta.log.crearLog("*******************************Error al llamar op.generarMedidaEstableKgMax(). Mensaje: "
//							+ ex.getMessage(), logMatricula, matricula,	idCamara);
//			throw ex;
//		}
//		
//		/*
//		 * op.setCabecera(1, op.estiloTicket, " CAAM "); op.setCabecera(2,
//		 * op.estiloTitulo, "Tragsatec"); op.setCabecera(3, op.estiloNormal,
//		 * "Matricula: MU9097Y"); op.setCabecera(4, op.estiloNormal,
//		 * "Origen: Alcantarilla"); op.setCabecera(5, op.estiloPieSubtitulo,
//		 * "ENTRADA MATERIAL"); op.setCabecera(6, op.estiloPieNormal,
//		 * "Pruebas");
//		 * 
//		 * op.generaTicket();
//		 */
//
//		return peso;
		
		return 700;
	}
	
		public synchronized boolean esperarPesoCero(String matricula, 
				String logMatricula, int idCamara) throws Exception {

			Boolean resul= false;
			// Llamar a Orion
			if (ControlPlanta.isDebug)
				op.setDebug(true);
			else
				op.setDebug(false);

			op.setDebug(false);

			try {
				op.conectar();
				resul=op.esMedidaEstableKgCero();

				op.desconectar();
				
			}  catch (Exception ex) {
				ControlPlanta.log.crearLog(
						"Error al llamar op.esMedidaEstableKgCero(). Mensaje: "
								+ ex.getMessage(), logMatricula, matricula,
						idCamara);
				throw ex;
			}
			return resul;
		}
		
	}
			


