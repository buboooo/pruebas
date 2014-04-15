package com.tragsatec.residuos;

public class Util {

	/**
	 * Obtiene porciones de medio byte de un short (2 bytes)
	 * @param aux
	 * @param num
	 * @return
	 */
	public static int getMedioByte(int aux, int num) {
		
		int salida = -1;
		
		if (num == 0) salida = aux & 15;
		else if (num == 1) salida = (aux>>4)&15;
		else if (num == 2) salida = (aux>>8)&15;
		else if (num == 3) salida = (aux>>12)&15;
				
		return salida;
	}
	
	
	/**
	 * Indica si el bint indicado del valor short (2 bytes) 
	 * es 1=true o 0=false
	 * @param aux
	 * @param num
	 * @return
	 */
	public static boolean getBit(int aux, int num) {		
		int salida = -1;		
		if (num <= 15) salida = (aux>>num)&1;						
		return salida==1;
	}
	
	/**
	 * Visualiza en formato hexadecimal un short (2 bytes)
	 * @param aux
	 * @return
	 */
	public static String verHexadecimal(int aux) {		
		//return Integer.toHexString((aux>>12)&15) + 
		//	   Integer.toHexString((aux>>8)&15)+
		//	   Integer.toHexString((aux>>4)&15) + 
		//	   Integer.toHexString((aux)&15);
		StringBuffer b = new StringBuffer();
		
		for (int j=0; j< 4 - Integer.toHexString(aux).length(); j++) b.append("0");
		
		return b.toString() + Integer.toHexString(aux).toUpperCase();
	}

	/**
	 * Visualiza en formato binario un short (2 bytes)
	 * @param aux
	 * @return
	 */
	public static String verBinario(int aux) {
		StringBuffer b = new StringBuffer();
		
		for (int j=0; j< 16 - Integer.toBinaryString(aux).length(); j++) b.append("0");
		return b.toString() + Integer.toBinaryString(aux);
	}	
}
