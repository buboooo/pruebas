/*
 * Nota: Cambios para producción:
 *  -. Ver el código de incidencia generado cuando la matricula no está en la tabla vehiculo proporcionada de la CARM
 *  -. 		o incidencia cuando falla la pesada 
 */

/*
 * Se puede ejecutar con:
 *   "java -jar basculas.jar debug matricula C74432CRW"  --> solo muestra mensajes de esa matricula
 * o "java -jar basculas.jar debug" --> muestra todos los mensajes
 * o "java -jar basculas.jar" ---> sin depuración.
 */

package com.tragsa.basculas;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.*;

import com.serotonin.modbus4j.exception.ModbusInitException;

//Ver archivo de configuración.xml necesario para cargar la configuración de la aplicación
public class ControlPlanta {
	static boolean isDebug = java.lang.management.ManagementFactory
			.getRuntimeMXBean().getInputArguments().toString()
			.indexOf("-agentlib:jdwp") > 0;
	
	public static ArchivoLog log = new ArchivoLog();
	
	//tipoCamara: para cada id de cámara tenemos un hashtable con los atributos de la misma
	public static Hashtable<Integer, Hashtable<String, String>> tipoCamara = new Hashtable<Integer, Hashtable<String, String>>();
	public static Hashtable<Integer,String> operacionCarril = new Hashtable<Integer,String>();
	public static Hashtable<String, String> configuracion = new Hashtable<String, String>();
	public static Hashtable<String, Bascula> hbasculas = new Hashtable<String, Bascula>();
	
	private static String ficheroConfiguracion = "config.xml";
	private static int tipoIncidencia = 0; 
	/*OJO, este valor se ha puesto para	
		 desarrollo, pero tendrá
		 que verse el número de código que corresponda con la incidencia de un
		 vehículo
		 que no esté en la tabla "vehiculo" cuando entra a la planta.
		
		  0 si matricula no registrada al entrar
		  1 cuando error en la báscula/barrera error modbus
		  2 cuando error no clasificado
	*/
	private static String logMatricula = ""; // para depurar viendo por donde pasa una mátricula
	private static Datos bbdd = null;
	public static int pesoMinimoHayAlgo;  //peso mínimo para entender que la pesada es válida. Se carga por configuración xml

	public static void main(String[] args) throws IOException,
			InterruptedException {
		if (args.length > 0) {
			if (args[0].equals("/?")) {
				System.out
						.println("***********************************************\r\n"
								+ "Se puede ejecutar de las siguientes formas:  \r\n\r\n"
								+ "1.- 'java -jar basculas.jar debug matricula C74432CRW'  --> solo muestra info para esa matricula \r\n\r\n"
								+ "2.- 'java -jar basculas.jar debug' --> guarda todos los mensajes en un archivo log.txt \r\n\r\n"
								+ "3.- 'java -jar basculas.jar' ---> sin mensajes de depuración.");
				System.exit(0);
			}

			if (args[0].equals("debug"))
				isDebug = true;
		}
		if (args.length > 1) {
			if (args[1].equals("matricula"))
				logMatricula = args[2];
		}

		try {
			cargaConfiguracion();  //Lee archivo XML

			ejecutar();
		} catch (Exception e) {
			// System.out.println("Error en la ejecución: " + e.getMessage() +
			// "\n" + e.getStackTrace().toString());
			log.crearLog("Funcion main(). Error en la ejecución: " + e.getMessage() + "\n" + e.getStackTrace().toString(),
					logMatricula, "", -1);

			System.exit(-1);
		}
	}

	/**
	 * Un hilo para cada camara y según el tipo si lleva barrera, peso o si es
	 * de entrada, se van realizando las acciones oportunas.
	 * 
	 * @author Juan Carlos Muñoz
	 * @param idTabla
	 *            es el id de la tabla de capturas de camara, para luego borrar
	 *            el registro de dicha tabla
	 * @param camara
	 *            id de la cámara leido del fichero de configuración
	 * @param matricula
	 * 			  del vehículo
	 * @param cCarril
	 * 			  clase que gestiona el carril, cada hilo tiene su instancia de clase ControlCarril
	 * @version: 4-3-2014
	 */
	static class hiloCamara implements Runnable {
		private Integer camara;
		private String tipoVehiculo = "";
		private String matricula = "";
		private int idcarril = 0;
		private int cod_planta;
		private int id_entrada = 0;
		private int idTabla;
		private int idVehiculo = 0;
		private Date fecha_entrada; // Current Date and Time in GMT timezone: 2012-01-10
							// 07:02:59
		private String tipoCamara = ""; // "entrada", "salida",
								// "interioEntradaVertedero",
								// "interioSalidaVertedero"

		private ControlCarril cCarril;
		private boolean matriculaNoRegistrada=false;
				
		//FUNCION EJECUCIÓN DEL THREAD
		public hiloCamara(int idCarril, Integer idTabla, int camara, String matricula,
				Date fecha_entrada, ControlCarril cCarril) {
			this.idcarril = idCarril;
			this.camara = camara;
			this.matricula = matricula;
			this.idTabla = idTabla;
			this.fecha_entrada = fecha_entrada;
			this.cCarril = cCarril;
		}

		public void run() {
			Float peso = (float) 0;

			try {
				/* si camara es de entrada, crea un registro en "entradas" comprobando
				 si está en tabla CARM "vehiculo" */
				tipoCamara = tipoCamara(camara);
				if (tipoCamara.equals("entrada")) {
					cod_planta = Integer.parseInt(configuracion
							.get("cod_planta"));

					idVehiculo = bbdd.CompruebaVehiculo(matricula);
					id_entrada = bbdd.creaEntrada(idVehiculo,
							fecha_entrada, cod_planta);
					
					// crea una incidencia de matricula no registrada
					if (idVehiculo == 0) {			
						bbdd.creaIncidencia(id_entrada, 0,
								"Matricula no registrada: " + matricula, matricula);
						matriculaNoRegistrada = true;
						}
					} //fin if(tipoCamara...

				/* registro en la tabla "movimientos" por cada captura de la cámara,
				 si no hay id_entrada, lo buscará si ya existe previamente	*/
				int id_entrada2 = bbdd.creaMovimiento(camara, fecha_entrada,
						matricula, id_entrada, idTabla);
				
				/*
				 Si ocurre lo siguiente es que el programa intenta ejecutarse de nuevo y
				 ya había tratado esta matricula. La Entrada que acaba de crear se debe borrar
				 y tomar el id_entrada que ya existía.	*/
				if ( (id_entrada2 != id_entrada) && (id_entrada != 0))
				{
					bbdd.borraEntrada(id_entrada);
					id_entrada = id_entrada2;
				}
				else
					id_entrada = id_entrada2;
				
				/*
				 Si esto ocurre es que es una matricula de salida que no tiene registro de entrada,
				 se termina el proceso si hacer ninguna acción. */
				if (id_entrada == 0)
				{
					bbdd.eliminaEntradasCarril(idcarril);
					ControlPlanta.setOperacionCarril(idcarril, "");
					log.crearLog("La matricula sale sin existir registro de entrada", logMatricula, matricula,camara);
					return;
					}
				
				if (tipoCamara.equals("salida"))
					bbdd.actualizaSalida(id_entrada, fecha_entrada);

				/*
				 Según donde esté la camara se espera que ocurran unas acciones. */

				switch (accionesCamara(camara)) {
				case "BarrerayBascula":
					// si camara tiene barrera y bascula
					
					tipoVehiculo = bbdd.TipoVehiculo(matricula);
					if (tipoVehiculo == null) tipoVehiculo = "";
					log.crearLog("---Esperando pesada del camión", logMatricula, matricula,camara);
					
					if (!tipoVehiculo.equals("articulado"))
						peso = cCarril.procesandoCarrilBarreraBascula(idcarril, matricula,
							camara, matriculaNoRegistrada, logMatricula);
					else
						peso = cCarril.procesandoCarrilBarreraBasculaArticulado(idcarril, matricula,
								camara, matriculaNoRegistrada, logMatricula);
					
					if (peso > pesoMinimoHayAlgo)
					{
						bbdd.creaTicket(id_entrada, peso, tipoCamara);
						log.crearLog("            ->crea ticket", logMatricula, matricula,camara);
					}
					else
					{
						bbdd.creaIncidencia(id_entrada, tipoIncidencia + 2, "Error captura de peso en báscula", matricula);
						log.crearLog("->crea incidencia: " + tipoIncidencia + 2 + 
								". Error captura de peso en báscula", logMatricula, matricula,camara);
					}
					
					/* Se comenta el siguiente código ya que una vez creado el ticket,
					 * se espera a que manualmente el operador (web) imprima el ticket de entrada/salida.
					 */

//					log.crearLog("->Espera a salir. Matricula: "
//							+ matricula, logMatricula, matricula,camara);
//					if (!tipoVehiculo.equals("articulado"))
//						cCarril.esperaSalidaCarril(camara,matricula,logMatricula);
//					else 
//						cCarril.esperaSalidaCarrilArticulado(camara,matricula,logMatricula);
//					
//					log.crearLog("->Ha salido de ocupar carril: " + idcarril + ". Matricula: "
//							+ matricula, logMatricula, matricula,camara);
					
					break;
				case "SoloBarrera":
					// si camara tiene barrera sin báscula;
					
					log.crearLog("---Abriendo barrera camara sin báscula", logMatricula, matricula,camara);
					cCarril.procesandoSoloBarrera(idcarril, matricula, camara, logMatricula);
					log.crearLog("---Abierta barrera camara sin báscula", logMatricula, matricula,camara);

					break;

				case "SoloBascula":
					// si camara tiene solo báscula;

					break;
				case "SinBarreraNiBascula":
					// si camara no tiene ni barrera ni báscula
					break;
				default:
						log.crearLog("Funcion tipoCamara(). Camara con tipo no reconocido",
								logMatricula, "", -1);
					break;
				} //try catch
				
				log.crearLog("->Ha terminado de procesar la Matricula: "
						+ matricula, logMatricula, matricula,camara);
				
				bbdd.eliminaEntradasCarril(idcarril);
				ControlPlanta.setOperacionCarril(idcarril, "");

			} catch (ModbusInitException eb) {
				//bbdd.desconecta(); //cerraria la conexión para el resto de
				
				ControlPlanta.log.crearLog("Error con la báscula. Mensaje: " 
						+ eb.getMessage(), logMatricula, matricula,	camara);
				try {
					bbdd.creaIncidencia(id_entrada, tipoIncidencia + 1,	"Error en la barrera/báscula", matricula);
					/*
					 * 	Este error si es que ocurre, es grave.
					 * 
					 *  Para gestionar este tipo de incidencia se propone:
					 *  Dejar el registro en bbdd sin eliminar, el cual se puede usar para mostrar en la 
					 *	página web información, cuando sea este tipo de incidencia. 
					 *	Se propone hacer un botón web para eliminar los resgistros de este carril por 
					 *	parte del usuario y así liberar el carril "a mano" y rellenar la incidencia.
					*/
				} catch (SQLException e) {
					log.crearLog("Error. No ha podido registrar la incidencia tras el error anterior. "
							+ "Error tras creaIncidencia(): " + e.getMessage(), logMatricula, matricula,	camara);
			
				}
			}catch (Exception ei) {
				
				log.crearLog("**********************ERROR******************", logMatricula, matricula, camara);
				log.crearLog("Funcion run() del hiloCamara. Mensaje: "
								+ ei.getMessage(), logMatricula, matricula,	camara);
				try {
					//intenta registrar una incidencia, pero puede estar caida la BBDD
					bbdd.creaIncidencia(id_entrada, tipoIncidencia + 2,	"-Error sin clasificar-", matricula);
				} catch (SQLException e) {
					log.crearLog("Error. No ha podido registrar la incidencia tras el error anterior"
							, logMatricula, matricula,	camara);
			
				}
				
			} // fin (Exception..)
		} // fin run ()
	} // class hiloCamara implements Runnable

	/**
	 * Carga la configuración de la aplicación de un fichero XML
	 * 
	 * @author Juan Carlos Muñoz
	 * 
	 * @version: 4-3-2014
	 * @throws IOException
	 * @throws JDOMException
	 * @throws FileNotFoundException
	 */
	private static void cargaConfiguracion() throws Exception {
		try {
			// Creamos el builder basado en SAX
			SAXBuilder builder = new SAXBuilder();
			// Construimos el arbol DOM a partir del fichero xml
	
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl == null)
				cl = ClassLoader.getSystemClassLoader();
	
			Document doc = builder.build(cl
					.getResourceAsStream(ficheroConfiguracion));
	
			Element raiz = doc.getRootElement();
			// Recorremos los hijos de la etiqueta raíz
			List<Element> hijosRaiz = raiz.getChildren();
			for (Element hijo : hijosRaiz) {
				// Obtenemos el nombre y su contenido de tipo texto
				String nombre = hijo.getName();
				String valor = hijo.getValue();
	
				configuracion.put(nombre, valor);
			}
	
		} catch (Exception ex) {
			log.crearLog("Funcion cargaConfiguracion(). Error: " + ex.getMessage(),	"", "", -1);
			throw ex;
		}
		
		//esta variable se usa para saber que el peso de la báscula no está reconociendo una persona o 
		// 		tiene un valor por defecto proximo a cero
		pesoMinimoHayAlgo = Integer.parseInt(configuracion.get("pesoMinimoHayAlgo"));
	}

	
	private static void ejecutar() throws Exception {
		try {
			// Crea connect a base datos
			bbdd = new Datos();
		} catch (Exception ex) {
			log.crearLog("Funcion ejecutar(). No se puedo abrir la base de datos!",
					logMatricula, "", -1);
			System.exit(1);
		}

		log.crearLog("Conexión bbdd realizada...", logMatricula, "", -1);

		tipoCamara = bbdd.cargaConfigCamaras(
				Integer.parseInt(configuracion.get("cod_planta")), hbasculas);

		 consultaMatriculas();
	
	} // fin ejecutar()

	
	/**
	 * Lee registros de información de las capturas de cámara.
	 * 
	 * @author Juan Carlos Muñoz
	 * 
	 * @version: 4-3-2014
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void consultaMatriculas() throws SQLException, IOException,
			InterruptedException {

		/*
		 * Ejemplo de consulta camaras por carril:
		 * select * from alpr where camera_code = '2' or camera_code = '1' limit 1
		 */
		
		ResultSet r = null;
		ResultSet matriculas = null;
		List<Integer> carriles = new ArrayList<Integer>();
		List<ControlCarril> lcontrolCarril = new ArrayList<>();
		
		
		//Por cada carril inicializamos variables
		r = bbdd.carriles();
		while (r.next()) {
			int idCarril = r.getInt("id");
			carriles.add(idCarril);
			setOperacionCarril(idCarril, "");
			ControlCarril cCarril = new ControlCarril();
			lcontrolCarril.add(cCarril);
			}

		/*
		 * por cada carril
		 * 			si no hay operación en el carril
		 * 				extraemos una única matricula pendiente
		 * 				ponemos una operación en el carril
		 * 				lanzamos thread trataMatricula
		*/
		
		//La variable 'operacionCarril' indica si el carril está ocupado
		while (1<2) {

			for(int i = 0;i<carriles.size();i++){
				int idCarril = (int) carriles.get(i);
				
				if ( getOperacionCarril(idCarril).equals("") ) {
					//select * from alpr where camera_code = '2' or camera_code = '1' limit 1
					matriculas = bbdd.ConsultaPorCarril(idCarril);
					if (matriculas.next()) {
						setOperacionCarril(idCarril, matriculas.getString("plate"));
					
						trataMatricula(idCarril, matriculas.getInt("alpr_id"), 
								matriculas.getInt("camera_code"), matriculas.getString("plate"),
								matriculas.getTimestamp("taken_on"), lcontrolCarril.get(i));
					}
				}
				else
				{
					/*
					 * Comprobamos  si no hay matriculas pendientes en el carril
					 * y la variable de carril ocupado está con un valor de matricula
					 * entonces la ponemos en blanco.
					 * 
					 * Esto sirve ya que la matricula no se borra de pendientes (alpr) hasta
					 * que no es tratada totalmente. 
					 * 
					 * Controla el error producido por la báscula, dejando el carril ocupado y 
					 * no sabiendo cuando podrá estar libre. Mientras que alpr tenga una matricula
					 * en ese carril significa que está el carril ocupado.
					 * 
					 * Botón en la interfaz web que elimine este registro y deje el carril liberado
					 */
					
					matriculas = bbdd.ConsultaPorCarril(idCarril);
					if (!matriculas.next()) {
						setOperacionCarril(idCarril, "");					
						log.crearLog("////Carril "+ idCarril + 
								" ha sido liberado tras borrar registros pendientes", 
								logMatricula, "", -1);
					}
				}
			} //fin for(...i<carriles.size()...)
			
			Thread.sleep(1500);
		} //fin (1<2)
	}

	/**
	 * Lanza un hilo por la matricula pasada como argumento
	 * 
	 * @author Juan Carlos Muñoz
	 * 
	 * @param matricula
	 *            La matrícula a tratar
	 * @version: 4-3-2014
	 * @throws IOException
	 */
	private static void trataMatricula(int idCarril, int idTabla, int camaraVideo,
			String matricula, Date fecha_entrada, ControlCarril cCarril) throws IOException {

		log.crearLog("------------------------------EMPIEZA TRATAR MATRICULA ----------------------------",
				logMatricula, matricula, camaraVideo);
	
		log.crearLog("Funcion trata(). Tratando matricula: " + matricula,
					logMatricula, matricula, camaraVideo);

		try {
			Thread t2 = new Thread(new hiloCamara(idCarril, idTabla, camaraVideo,
					matricula, fecha_entrada, cCarril));
			t2.setDaemon(false);
			t2.start();
			log.crearLog("***Hilo lanzado", logMatricula, matricula, camaraVideo);
			
		} catch (Exception ei) { // 250 mills might not be enough!
			log.crearLog(
					"Funcion trata(). Error al crear el thread para esta camara. Error descripcion: "
							+ ei.getMessage(), logMatricula, matricula,
					camaraVideo);
			System.err.println(ei);
			throw ei;
		}
	}

	/**
	 * Mira si la camara es de entrada, salida
	 * 
	 * @author Juan Carlos Muñoz
	 * @param camaraVideo
	 *            el nombre de la cámara de video cuando ha cargado la
	 *            configuración la aplicación
	 * @version: 4-3-2014
	 * @throws IOException
	 */
	private static String tipoCamara(Integer camaraVideo) throws IOException {
		Hashtable<String, String> atributosCamara = new Hashtable<String, String>();
		atributosCamara = tipoCamara.get(camaraVideo);

		return atributosCamara.get("tipo");
	}

	/**
	 * Mira si la camara tiene barrera y/o bascula Devuelve el dato string
	 * tipificado descriptivo de las acciones venideras
	 * 
	 * @author Juan Carlos Muñoz
	 * @param camaraVideo
	 *            el nombre de la cámara de video cuando ha cargado la
	 *            configuración la aplicación
	 * @version: 4-3-2014
	 */
	private static String accionesCamara(Integer camaraVideo) {
		Hashtable<String, String> atributosCamara = new Hashtable<String, String>();
		atributosCamara = tipoCamara.get(camaraVideo);

		if ((atributosCamara.get("barrera").equals(""))
				&& (atributosCamara.get("bascula") != ""))
			return "SoloBascula";
		if (!(atributosCamara.get("barrera").equals(""))
				&& !(atributosCamara.get("bascula").equals("") ) )
			return "BarrerayBascula";
		if ( !(atributosCamara.get("barrera").equals("") )
				&& (atributosCamara.get("bascula").equals("") ))
			return "SoloBarrera";

		return "SinBarreraNiBascula";
	}

	/**
	 * Los hilos creados pueden acceder a esta variable y la protegemos con synchronized
	 * @param idCarril
	 * @param valor
	 */
	public static synchronized void setOperacionCarril(int idCarril, String valor) {
		operacionCarril.put(idCarril,valor);
	}
	
	public static synchronized String getOperacionCarril(int idCarril) {
		return operacionCarril.get(idCarril);
	}
	
}