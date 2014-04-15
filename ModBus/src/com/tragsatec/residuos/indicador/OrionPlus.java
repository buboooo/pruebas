package com.tragsatec.residuos.indicador;

import java.io.IOException;

import com.tragsatec.residuos.Util;

public class OrionPlus extends Indicador  {
	
	public static int dirPermisos = 4000;
	public static short permisoEscritura = (short) 0x8200;
	public static short ordenImprimir = (short) 0x4017;
	public static int dirFunTeclado = 9;
	
	public static int estiloPieNormal = 1;
	public static int estiloPieSubtitulo = 2;
	public static int estiloNormal = 3;
	public static int estiloSubtitulo = 4;
	public static int estiloTitulo = 5;
	public static int estiloTicket = 6;
	
    // Variables de direcciones modbus
	private String dirStatus;
	private String dirPuat;
	private String dirNeto;
	private String dirTara;
	
	    	
    // Variables utilizadas para generar medida
	private int tiempoPesadaValida = 5; // tiempo en segundos para devolver peso estable
                                        // si se utiliza pesada 0-max o pesada max
	private float margenPesadaCero ; //= 10;
	private int topeEsperaPesada = 60; // 60 segundos maximos espera para generar medida 
	
	public OrionPlus(String idOrion) throws IOException{
		super(); // Es necesario
				        
		// Propiedades heredadas
		setIp(getProp().getProperty(idOrion + "_IP"));
		setPuerto(getProp().getProperty(idOrion + "_PORT"));
		
		// Propiedades no heredadas
		dirStatus = getProp().getProperty(idOrion + "_DIR_STATUS");
		dirPuat = getProp().getProperty(idOrion + "_DIR_PUAT");
		dirNeto = getProp().getProperty(idOrion + "_DIR_PESO_NETO");
		dirTara = getProp().getProperty(idOrion + "_DIR_PESO_TARA");
		tiempoPesadaValida = Integer.parseInt(getProp().getProperty(idOrion + "_SEG_PESO_MAX"));
		topeEsperaPesada = Integer.parseInt(getProp().getProperty(idOrion + "_TIEMPO_TIMEOUT"));
		margenPesadaCero = Integer.parseInt(getProp().getProperty(idOrion + "_MARGEN_PESADA_CERO"));
		
	  }
	
	
  public boolean medicionEstable() {
	boolean salida = false;
	
	
	int aux = leerMemoriaWord(Integer.parseInt(dirStatus)) >> 8; // Obtenemos el byte alto
	// Le restamos 32: Segun documentación byte <ST> es 0x20+b7b6b5b4b3b2b1b0 (estado=b7b6b5b4b3b2b1b0)
	aux = aux - 32; 
	Short palabra = (short) aux;
	if (isDebug()) System.out.println("medicionEstable(): " + palabra + "-" + Util.verBinario(palabra) + "-" + Util.verHexadecimal(palabra));
	if (Util.getBit(palabra,5)) salida = true;
	return salida;
	
  }
  
  
  public Float brutoKg() {
	  return netoKg() + taraKg();
  }
  
  public Float netoKg() {
	  float salida;
	  Integer palabraPuat = leerMemoriaWord(Integer.parseInt(dirPuat));
	  Integer palabraNeto = leerMemoriaWord(Integer.parseInt(dirNeto));
	  
	  int decimales = Util.getMedioByte(palabraPuat, 1);
	  int unidades  = Util.getMedioByte(palabraPuat, 0);
	  
	  if (isDebug()) System.out.println("netoKg(). Puat: " + palabraPuat + "-" + Util.verBinario(palabraPuat) + "-" + Util.verHexadecimal(palabraPuat));
	  if (isDebug()) System.out.println("netoKg(). Neto: " + palabraNeto + "-" + Util.verBinario(palabraNeto) + "-" + Util.verHexadecimal(palabraNeto));
	  
	  if (isDebug()) System.out.println("netoKg(). Decimales: " +  decimales);
	  if (isDebug()) System.out.println("netoKg(). Unidades: " + unidades);
	  
	  salida = indiceConversion(unidades) * palabraNeto.floatValue()/(float)(Math.pow(10.0,decimales));
	  
	  return salida;
	
  }

  
  public Float taraKg() {
	  float salida;
	  Integer palabraPuat = this.leerMemoriaWord(Integer.parseInt(dirPuat));
	  Integer palabraTara = this.leerMemoriaWord(Integer.parseInt(dirTara));
	  
	  int decimales = Util.getMedioByte(palabraPuat, 1);
	  int unidades  = Util.getMedioByte(palabraPuat, 0);
	  
	  if (isDebug()) System.out.println("taraKg(). Puat: " + palabraPuat + "-" + Util.verBinario(palabraPuat) + "-" + Util.verHexadecimal(palabraPuat));
	  if (isDebug()) System.out.println("taraKg(). Tara: " + palabraTara + "-" + Util.verBinario(palabraTara) + "-" + Util.verHexadecimal(palabraTara));
	  
	  if (isDebug()) System.out.println("taraKg(). Decimales: " +  decimales);
	  if (isDebug()) System.out.println("taraKg(). Unidades: " + unidades);

	  
	  salida = indiceConversion(unidades) * palabraTara.floatValue()/(float)(Math.pow(10.0,decimales));
	  
	  return salida;
	
  }
 
  
    
  
  private float indiceConversion(int unidad) {
	float salida = -1;
	if (unidad == 0) salida = 1/1000; // TONELADAS A KILOS 
	else if (unidad == 1) salida = 1; // KILOS A KILOS
	else if (unidad == 3) salida = 1000; // GRAMOS A KILOS
	else if (unidad == 4) salida = (float)0.45359237; // LIBRAS A KILOS
	
	return salida;
  }



	
	/**
	 * Calcula peso bruto asociado a camion que se desea pesar (Kg)
	 * Cuando el peso pasa de cero estable a max estable y, otra vez a ser cero estable devuelve peso maximo
	 * Es bloqueante....
	 * 	 * @return
	 */
	public Float generarMedidaEstableKgCeroMaxCero() {
	    	
		boolean medidaGeneradaInicio = false;
		boolean medidaGeneradaFin = false;	
		float medidaGenerada = 0;
		
		float bruto = -1;
		int medidas = 0;
		
		
		// Debo obtener la mayor medida estable entre dos medidas estables proximas a cero
		while (!medidaGeneradaFin) {
			medidas ++;
			bruto = brutoKg();		
			
			// Inicio cuando medida cero estable
			if (!medidaGeneradaInicio && 
			     medicionEstable() && 
			     brutoKg() < margenPesadaCero) {
				medidaGeneradaInicio = true;
				
				// Cada segundo compruebo pesada y guardo max  si pesada es estable	
			} else if (medidaGeneradaInicio && 
				medicionEstable() &&
				bruto == brutoKg() ) {
				
				if (bruto > medidaGenerada) {
					medidaGenerada = bruto;
				}
				
				// Finalizo cuando vuelvo a medida cero estable
				if (medidaGenerada > bruto && 
					bruto < margenPesadaCero) medidaGeneradaFin = true;
								
			}
	
			if (isDebug()) System.out.println("generarMedidaEstableKgCeroMaxCero(). bruto leido: " + bruto +"; bruto ESTABLE ALMACENADO: " + medidaGenerada);
			
			// Controlo timeout por tiempo excedido
			if (medidas > topeEsperaPesada) {
				medidaGeneradaFin = true;
			}
			try {
				Thread.sleep(1000); // un segundo de espera
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				medidaGeneradaFin = true;
				e.printStackTrace();
			}
			
		}
		
		return medidaGenerada;
		
	}

	/**
	 * Calcula peso bruto asociado a camion que se desea pesar (Kg)
	 * Cuando el peso máximo se estabiliza devuelve peso maximo
	 * Es bloqueante....
	 * 	 * @return
	 */
	public Float generarMedidaEstableKgMax() {
	    	

		boolean medidaGeneradaFin = false;	
		float medidaGenerada = 0;		
		
		float bruto = -1;
		int medidas = 0;
		int vecesMismoPeso = 0;
		
		
		// Debo obtener la mayor medida estable entre dos medidas estables proximas a cero
		while (!medidaGeneradaFin) {
			
			medidas ++;
			bruto = brutoKg();			
			
			// Comienzo a comprobar el peso
            if (medicionEstable() &&
				bruto == brutoKg() &&
				bruto >= margenPesadaCero) {
				
				if (bruto > medidaGenerada) {
					medidaGenerada = bruto;
					vecesMismoPeso = 0;
				} else if (bruto == medidaGenerada) {
					vecesMismoPeso++;
				} else vecesMismoPeso = 0;
								
				// Finalizo si se ha indicado estabilidad en peso máximo
				// y han pasadu un número de veces con el mismo peso
				if (vecesMismoPeso >= tiempoPesadaValida) medidaGeneradaFin = true;
				
			}
	
			if (isDebug()) System.out.println("generarMedidaEstableKgMax(). bruto leido: " + bruto +"; bruto ESTABLE ALMACENADO: " + medidaGenerada);
			
			// Controlo timeout por tiempo excedido
			if (medidas > topeEsperaPesada) {
				medidaGeneradaFin = true;
			}
			try {
				Thread.sleep(1000); // un segundo de espera
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				medidaGeneradaFin = true;
				e.printStackTrace();
			}			
		}
		
		return medidaGenerada;
		
	}
	
	
	/**
	 * Calcula peso bruto asociado a camion que se desea pesar (Kg)
	 * Cuando el peso máximo se estabiliza devuelve peso maximo
	 * Es bloqueante....
	 * 	 * @return
	 */
	public boolean esMedidaEstableKgCero() {
	    	

		boolean medidaGeneradaFin = false;	
		float medidaGenerada = 0;		
		
		float bruto = -1;
		int medidas = 0;
		int vecesMismoPeso = 0;
		
		boolean salida = false;
		
		// Debo obtener la mayor medida estable entre dos medidas estables proximas a cero
		while (!medidaGeneradaFin) {
			
			medidas ++;
			bruto = brutoKg();			
			
			// Comienzo a comprobar el peso
            if (medicionEstable() &&
				bruto == brutoKg() &&
				bruto < margenPesadaCero) {
				
            	vecesMismoPeso++;
								
				// Finalizo si se ha indicado estabilidad en peso máximo
				// y han pasado un número de veces con el mismo peso
				if (vecesMismoPeso >= tiempoPesadaValida) { 
					medidaGeneradaFin = true;
					salida = true;
				}
				
			} else vecesMismoPeso = 0;
	
			if (isDebug()) System.out.println("esMedidaEstableKgCero(). bruto leido: " + bruto +"; bruto ESTABLE ALMACENADO: " + medidaGenerada);
			
			try {
				Thread.sleep(1000); // un segundo de espera
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				medidaGeneradaFin = true;
				e.printStackTrace();
			}			
		}
		
		return salida;
		
	}
	

	/**
	 * Sirve para indicar las cadeceras en el ticket del indicador orion plus
	 * @param num Número de cabecera (1-6)
	 * @param estilo (Tamaño de cabecera)
	 * @param texto
	 */
	public void setCabecera(int num, int estilo, String texto ) {
		StringBuffer cadena = new StringBuffer();
		char[] ca;
    	Short[] aux20 = new Short[20];
    	int indice20 = 0;
		
    	if (isDebug()) System.out.println("setCabecera.");
    	
		// Preparo cadena de 38 caracteres
		for (int i=0; i <38; i++) {
			if (i<texto.length()) cadena.append(texto.substring(i, i+1));
			else cadena.append(" ");
		}

		ca = cadena.toString().toCharArray();
		
       	// Generamos shorts equivalentes a cadena.
       	aux20[indice20] = (short) (ca[0] + (estilo-1)*256);
       	for (int k=1; k <= ca.length-2; k=k+2) {
       		indice20++;
       		aux20[indice20] = (short)	(ca[k]*256 + ca[k+1]);       	
       	}
       	indice20++;
       	aux20[indice20] = (short) (ca[ca.length-1]*256);
       	
       	this.escribirMemoriaWord(dirPermisos, this.permisoEscritura);
        for (int i = 0; i < 20; i++) {
        	this.escribirMemoriaWord(i+(578+(num-1)*20), aux20[i].shortValue());        	
        }
	}
	
	public int getParamGeneral(int numreg) {		
		int res = this.leerHoldingRegister(numreg);
		return res;
	}

	/**
	 * Da orden de impresión de ticket en indicador orion plus.
	 */
	public void generaTicket() {

		if (isDebug()) System.out.println("generaTicket.");
       	this.escribirMemoriaWord(dirFunTeclado, this.ordenImprimir);
        
	}	
}
