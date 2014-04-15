package pruebas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;


public class Check {

	public static void main(String[] args) throws Exception {

		probarConexion();
	}

	public static void escribir()
	{
		try {
			System.out.println("Escribe algo: ");
			Thread.sleep(2000);
			//algo = LeerEntrada.eerEntrada();
			Thread t2 = new Thread(new LeerEntrada());
			t2.setDaemon(false);
			t2.start();
			} catch (Exception ex) {
			
		}
	}
	
	public static boolean probarConexion() { //Statement stmt

		String socket = "5432";
		InetAddress ina;
		String serverHostname = "192.168.0.55"; //la IP del ordenador
		try {
			ina = InetAddress.getByName(serverHostname);
			if(ina.isReachable(5000)){ //5000=tiempo durante el que esperamos por la respuesta
				System.out.println("OK");
				//ya sabemos que el ordenador está encendido
				//comprobamos mediante un socket si responde el puerto de la Javadb
				Socket echoSocket = null;
				try {
					echoSocket = new Socket(serverHostname, Integer.parseInt(socket)); //1527 puerto que usa javadb
					echoSocket.close();
					
					System.out.println("Existe socket abierto en "+ serverHostname + " con puerto "+socket);
				} catch (Exception e) {
					System.out.println("Error al conectarse al socket: " + serverHostname);
					return false;
				}
				return true;
			}else{
				System.out.println("No responde al ping");
				return false;
			}
		} catch (IOException e) {
			System.out.println("Error al conectarse a: " + serverHostname);
			return false;
		}

	}

	
	static class LeerEntrada implements Runnable{

		public LeerEntrada(){
			
		}
		@Override
		public void run() {
			String salida = "";
			boolean menu = false;
			// TODO Auto-generated method stub
			
			while (1<2) {
				
			try{
				
				// Definimos un flujo de caracteres de entrada: leerEntrada
				BufferedReader leerEntrada = new BufferedReader(new InputStreamReader(System.in));
				// Leemos la entrada, finaliza al pulsar la tecla Entrar
				salida = leerEntrada.readLine();
				
				if ( (salida.equals("")) && menu)
					System.out.println("--> Estas dentro de menú. Usa el siguiente comando");
				
				if ( (salida.equals("menu"))  || (menu) )
				{
					menu = true;
					System.out.println("--> carril n (deja desocupado carril n) ");
					
					if (salida.startsWith("carril"))
					{
						int idCarril = Integer.parseInt(salida.substring(7,8));
						System.out.println("Carril: " +idCarril + " ha quedado libre.");
						//operacionCarril.put(idCarril, "");
					}
				}
				
				
				} catch( Exception e) {
					System.err.println("Error: " + e.getMessage());					
						}
			} //fin while	
			
		}
		}
	
	
}
