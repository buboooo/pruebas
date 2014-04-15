package pruebas;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.*; //SAXBuilder


public class CargaDatos {

	private static boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
			getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

			private static Hashtable<String, Hashtable> grupos = new Hashtable<String, Hashtable>();
			
			private final static Logger log = Logger.getLogger(CargaDatos.class .getName()); 

			public static void main(String[] args) throws Exception {

				if (args.length>0){
					if (args[0].equals("/?"))
					{

						System.out.println("Se puede ejecutar con  'java -jar basculas.jar debug matricula C74432CRW'  --> solo muestra mensajes de esa matricula "
								+ "* o 'java -jar basculas.jar debug' --> muestra todos los mensajes"
								+ " * o 'java -jar basculas.jar' ---> sin depuración.");
						System.exit(0);
					}

					if (args[0].equals("debug"))
						isDebug = true;
				}

				 LogManager.getLogManager().readConfiguration(
					        new FileInputStream("./log.properties"));
				
				log.info("Empieza salida log");
				
				cargaConfiguracion();

				quienFalta();
				
				log.info("Sigue funcionando");
				//--->				
				//lista a los miembros de un grupo menos el que lo visita
				List result = nombresGrupo("grupo 1","ba");
				JSONObject js = listadoIndividuos(result);
				
				log.info("Después primer json");
				//--->
				//Cuando se selecciona un individuo, dependiendo del grupo y el login
				Hashtable<String,Nombre> listaNom = grupos.get("grupo 1");
				Nombre n = listaNom.get("ba");
				String nom = n.getNombre();
				votar(n,"aAlguien");
				guardaConfiguracionLineal("grupo 1",n);

				log.info("Después segundo json");
				//--->
				//Para ver resultado de votaciones
				List lvotados = obtenerListaVotados();
				JSONObject jsVotaciones = listadoVotaciones(lvotados);
				
				log.info("Después de lista");
				//--->
				//Para ver quien queda por votar de un grupo
				List lfaltan = quienFalta();
				JSONObject jsFaltan = listadoFaltan(lfaltan);
				log.info("Final Aplicación");
			}
			
			private static List<Nombre> quienFalta() throws FileNotFoundException, JDOMException, IOException
			{
				List<Nombre> faltan = new ArrayList<Nombre>();
				
				//Lee del fichero todos los que han votado
				List<Nombre> yaVotados = obtenerListaVotados();

				Enumeration egrupos = grupos.keys();
				String claveGrupos = "";
				while (egrupos.hasMoreElements())
				{
					claveGrupos = (String)egrupos.nextElement();
					
					Hashtable<String,Nombre> listaNom = grupos.get(claveGrupos);
					Enumeration e = listaNom.keys();

					int n = listaNom.size()+1;
					String clave="";
					while( e.hasMoreElements() ){
						clave = (String)e.nextElement();
						
						if ( estaEnLista(listaNom.get(clave).getNombre(),yaVotados) == -1)
						{
							faltan.add(listaNom.get(clave));
						}					
					}
					
				}

				return faltan;
			}
			

			private static int estaEnLista(String alias, List<Nombre> lista)
			{
				int resul = -1;
				for(int n=0;n<lista.size();n++)
				{
					if ( ((Nombre) lista.get(n)).getNombre().equals(alias) )
					{
						resul = n;
						break;
					}
				}
				return resul;
			}

			/**
			 * Lee el archivo donde van haciendo las votaciones y crea una lista de clases
			 * 	Nombre, en donde aparece el alias, el grupo y a quién vota
			 * @return Lista de clases Nombre
			 * @throws FileNotFoundException
			 * @throws JDOMException
			 * @throws IOException
			 */
			private static List<Nombre> obtenerListaVotados() throws FileNotFoundException, JDOMException, IOException
			{
				List<Nombre> result = new ArrayList<>();

				String grupo = "";
				String quien = "";
				String aquien = "";
				int primera=0;
				int segunda=0;


				FileReader archivo;
				archivo = new FileReader(new File("resultados.txt"));

				BufferedReader br = new BufferedReader(archivo);

				String linea;
				while((linea = br.readLine()) != null)
				{
					System.out.println(linea);
					primera = linea.indexOf("'", 0);
					segunda = linea.indexOf("'",primera+1);

					grupo = linea.substring(primera, segunda);

					primera = linea.indexOf("'", segunda+1);
					segunda = linea.indexOf("'",primera+1);

					quien = linea.substring(primera+1, segunda);

					primera = linea.indexOf("'", segunda+1);
					segunda = linea.indexOf("'",primera+1);

					aquien = linea.substring(primera, segunda);

					Nombre nom = new Nombre();
					nom.setGrupo(grupo);
					nom.setNombre(quien);
					nom.setVota(aquien);

					int donde = estaEnLista(nom.getNombre(), result);
					if ( donde != -1 )
						result.get(donde).setVota(aquien);
					else
						result.add(nom);

				}

				archivo.close();

				return result;

			}

			/**
			 * Carga la configuración de la aplicación
			 * crea una lista hashtable "grupos" donde grupos.get("grupo 1") es
			 * 	una lista Hashtable<String,Nombre> listaNom cuyo primer parametro es el 
			 * 	alias o individuo
			 *  La clase Nombre tiene Nombre.getGrupo, getNombre, getVota (individua a quien votas)
			 * 
			 * 
			 * @author Juan Carlos Muñoz
			 * 
			 * @version: 4-3-2014 
			 * @throws IOException 
			 * @throws JDOMException 
			 * @throws FileNotFoundException 
			 */
			private static void cargaConfiguracion() throws Exception
			{
				int n=0;

				try {
					// Creamos el builder basado en SAX  
					SAXBuilder builder = new SAXBuilder();  
					// Construimos el arbol DOM a partir del fichero xml  
					Document doc = builder.build(new FileInputStream("grupos.xml")); 

					Element raiz = doc.getRootElement();  
					// Recorremos los hijos de la etiqueta raíz  
					List<Element> hijosRaiz = raiz.getChildren();

					for(Element hijo: hijosRaiz){  
						n++;

						Hashtable<String,Nombre> listaNombres = new Hashtable<String,Nombre>();

						String nombre = hijo.getName();  
						String id = hijo.getAttributeValue("id");
						String valor = hijo.getValue();  

						if (isDebug)
							System.out.println("\nEtiqueta: "+nombre+". Texto: "+valor);  

						if (nombre == "grupo")
						{
							List<Element> nombresDelGrupo = hijo.getChildren();
							int i = 0;
							for(Element nombres: nombresDelGrupo){  

								Nombre nom = new Nombre();

								nom.setGrupo("grupo "+ id);
								nom.setNombre(nombres.getAttribute("alias").getValue());
								nom.setVota(nombres.getAttribute("vota").getValue());

								listaNombres.put(nom.getNombre(), nom );
								i++;
							}
						}
						else
						{
							//grupos.put(id, valor);
						}

						grupos.put("grupo " + n, listaNombres);
					}  //del for

				}
				catch (Exception ex) {
					System.out.println(ex.getMessage());
					throw ex;
				}

			} //fin funcion cargaConfiguracion

			/**
			 * Devuelve una lista de string que son los nombres de individuos que
			 * pertenecen a un grupo. Se usa por ejemplo: nombresGrupo("grupo 1","ba")
			 * @param ngrupo 	puede ser "grupo 1", "grupo 2", ...
			 * @param sinAlias   Saca a todos los individuos del grupo menos a este
			 * @return
			 */
			private static List<String> nombresGrupo(String ngrupo,String sinAlias)
			{
				List<String> result = new ArrayList<>();
				Hashtable<String,Nombre> nombres = grupos.get(ngrupo);

				Enumeration e = nombres.keys();
				String clave;
				while( e.hasMoreElements() ){
					clave = (String)e.nextElement();
					System.out.println( "Clave : " + nombres.get(clave).getNombre() );
					result.add( nombres.get(clave).getNombre() );
				}

				return result;

			}

			private static void votar(Nombre quien, String aQuien)
			{
				quien.setVota(aQuien);
			}

			/**
			 * guarda cada selección de un individuo en un txt de forma linea con su 
			 *	última votación es la verdadera. Un individuo puede votar varias veces, pero
			 *	la última será la válida.
			 * @param grupo
			 * @param nom
			 * @throws IOException
			 */
			private static synchronized void guardaConfiguracionLineal(String grupo, Nombre nom) throws IOException
			{
				FileWriter archivo;
				if (new File("resultados.txt").exists()==false){
					archivo=new FileWriter(new File("resultados.txt"),false);

				}
				archivo = new FileWriter(new File("resultados.txt"), true);

				//				archivo.write("<?xml version='1.0' encoding='UTF-8'?>");
				//				archivo.write("\r\n");
				//				archivo.write("<resultados>");
				//				archivo.write("\r\n");

				archivo.write("<nombre grupo='"+ grupo + "' alias='" + nom.getNombre() + "' vota='" + nom.getVota() +"'/>");
				archivo.write("\r\n");

				//				archivo.write("</resultados>");
				//				archivo.write("\r\n");

				archivo.close();

			}

			//No usar
			private static void guardaConfiguracion() throws Exception
			{
				FileWriter archivo;
				if (new File("resultados.txt").exists()==false){archivo=new FileWriter(new File("log.txt"),false);}
				archivo = new FileWriter(new File("resultados.txt"), true);

				archivo.write("<?xml version='1.0' encoding='UTF-8'?>");
				archivo.write("\r\n");
				archivo.write("<grupos>");
				archivo.write("\r\n");

				//recorre todo el objeto grupos y actualiza el xml
				Enumeration e = grupos.keys();
				String clave;
				int n = grupos.size()+1;
				while( e.hasMoreElements() ){
					clave = (String)e.nextElement();
					n--;
					archivo.write("<grupo id='" + n + "' >");
					archivo.write("\r\n");

					//System.out.println( "Clave : " + nombres.get(clave).getNombre() );
					Hashtable<String,Nombre> nombres = grupos.get(clave);

					Enumeration enombres = nombres.keys();
					String clave2;
					while( enombres.hasMoreElements() ){
						clave2 = (String)enombres.nextElement();

						Nombre nom = nombres.get(clave2);  //son los atributos de un nombre del grupo

						archivo.write("<nombre alias='" + nom.getNombre() + "' vota='" + nom.getVota() +"'/>");
						archivo.write("\r\n");
						//<nombre alias="ja" vota=""/>

					} //fin Enumeration nombres

					archivo.write("</grupo>");
					archivo.write("\r\n");
				}

				archivo.write("</grupos>");
				archivo.write("\r\n");
				archivo.close();
			}

			
			
			private static JSONObject listadoIndividuos(List listToJson) {
				
				JSONArray lista = new JSONArray();
				JSONObject resultado = new JSONObject();
				//Writer out = response.getWriter();
				
				for(Object object : listToJson) {
					String element = (String) object;
					
					JSONObject json = new JSONObject();	
					
					json.put("cod", element);
					json.put("nombre",element);

					lista.add(json);
				}

			    resultado.put("success", true);
			    resultado.put("listado",lista);

			    return resultado;
			    
			}
			
			private static JSONObject listadoVotaciones(List listToJson) {
				
				JSONArray lista = new JSONArray();
				JSONObject resultado = new JSONObject();
				//Writer out = response.getWriter();
				
				for(Object object : listToJson) {
					Nombre element = (Nombre) object;
					
					JSONObject json = new JSONObject();	
					
					json.put("grupo", element.getGrupo());
					json.put("quien",element.getNombre());
					json.put("aquien",element.getVota());

					lista.add(json);
				}

			    resultado.put("success", true);
			    resultado.put("listado",lista);

			    return resultado;
			    
			}
			
			private static JSONObject listadoFaltan(List listToJson)
			{
				JSONArray lista = new JSONArray();
				JSONObject resultado = new JSONObject();
				//Writer out = response.getWriter();
				
				for(Object object : listToJson) {
					Nombre element = (Nombre) object;
					
					JSONObject json = new JSONObject();	
					
					json.put("grupo", element.getGrupo());
					json.put("quien",element.getNombre());
					json.put("aquien",element.getVota());

					lista.add(json);
				}

			    resultado.put("success", true);
			    resultado.put("listado",lista);

			    return resultado;
			}
			
			
			
			
			
			private static class Nombre
			{
				private String grupo="";
				private String nombre="";
				private String vota="";

				public String getNombre() {
					return nombre;
				}
				public void setNombre(String nombre) {
					this.nombre = nombre;
				}
				public String getVota() {
					return vota;
				}
				public void setVota(String vota) {
					this.vota = vota;
				}

				public String getGrupo() {
					return grupo;
				}

				public void setGrupo(String grupo) {
					this.grupo = grupo;
				}
			}
			
			

}


