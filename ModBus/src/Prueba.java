import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.ip.IpParameters;
import com.tragsatec.residuos.Util;
import com.tragsatec.residuos.automata.*;
import com.tragsatec.residuos.indicador.*;

public class Prueba {
	
	public static short permisoEscritura = (short) 0x8200;
	
	// <punto_dec><unidades><alt_baj><tipo_tara>
	private static String palabra = "21F0";
	
	public static void main(String[] args) throws Exception {
		main4(args);
	}
	
	public static void main4(String[] args) throws Exception {
		
		OrionPlus op = new OrionPlus("ORIONPLUS01B1");
    	op.setDebug(true);
    	op.conectar();
    	//op.setCabecera(1, op.estiloTicket, " CAAM ");
   	
    	int res1=0;
    	int dire=0;
    	for (int j=0; j < 42; j++) {
    		dire = 2000+ j;
	    	res1 = op.getParamGeneral(2000+ j);
	    	System.out.println(dire + "-->" + Util.verHexadecimal(res1) + "(" + res1 +")");
    	}
    	

    	
//    	res1 = op.leerMemoriaWord(579);
//    	System.out.println("segundo valor: " + res1);
    	op.desconectar();
	}
	
	
	public static void main1(String[] args) throws Exception {
		
		
		Omron au = new Omron("OMRON_01");
		
		try {
		au.conectar();
		
		System.out.println("Subiendo barrera");
		au.subirBarrera();
		
		//au.bajarBarrera();
		System.out.println("Estado: " + au.estadoBarrera());
		} catch (com.serotonin.modbus4j.exception.ModbusInitException ee) {
			ee.printStackTrace();
		}
		
		au.desconectar();
	}
	
		
	public static void main2(String[] args) throws Exception {

    	OrionPlus op = new OrionPlus("ORIONPLUS01B1");
    	op.setDebug(true);
    	op.conectar();
    	//System.out.println(op.generarMedidaEstableKgCeroMaxCero());
    	
    	System.out.println(op.generarMedidaEstableKgMax());
    	
    	//System.out.println("cero -->" + op.esMedidaEstableKgCero());
    			
//    
//    	op.setCabecera(1, op.estiloTicket, " CAAM ");
//    	op.setCabecera(2, op.estiloTitulo, "Tragsatec");
//    	op.setCabecera(3, op.estiloNormal, "Matricula: MU9097Y");
//    	op.setCabecera(4, op.estiloNormal, "Origen: Alcantarilla");
//    	op.setCabecera(5, op.estiloPieSubtitulo, "ENTRADA MATERIAL");
//    	op.setCabecera(6, op.estiloPieNormal, "Pruebas");
//    	
    	op.generaTicket();
    
    	op.desconectar();
		
	}
	
    public static void main3(String[] args) throws Exception {
    	
    	short aux = 0;
    	Short[] aux20 = new Short[20];
    	int indice20 = 0;
    	String cadena = "ABCD MUNDO...........................!"; // 38 caracteres
    	
        ModbusFactory factory = new ModbusFactory();
        IpParameters params = new IpParameters();
        params.setHost("192.168.250.2");
        params.setPort(502);
        params.setEncapsulated(false);
        
        ModbusMaster master = factory.createTcpMaster(params, true);
        // master.setRetries(4);
        master.setTimeout(1000);
        master.setRetries(0);

        long start = System.currentTimeMillis();
        try {
        	
           master.init();

       	char[] ca = cadena.toCharArray();

       	// Generamos shorts equivalentes a cadena.
       	aux20[indice20] = (short) ca[0];
       	for (int k=1; k <= ca.length-2; k=k+2) {
       		indice20++;
       		aux20[indice20] = (short)	(ca[k]*256 + ca[k+1]);       	
       	}
       	indice20++;
       	aux20[indice20] = (short) (ca[ca.length-1]*256);
       	
       	
String s = "";
char c1;
char c2;
        for (int k = 0; k < 1; k++) {
            
            for (int i = 0; i < 20; i++) {
           
               aux = (Short) master.getValue(127, 
            		   RegisterRange.HOLDING_REGISTER, 
               		i+578,
                       DataType.TWO_BYTE_INT_SIGNED); 

               s = Util.verHexadecimal(aux);
               c2 = (char) (Util.getMedioByte(aux, 0) + Util.getMedioByte(aux, 1)*16);
               c1 = (char) (Util.getMedioByte(aux, 2) + Util.getMedioByte(aux, 3)*16);
               if (i>0) System.out.print(c1);
               else System.out.println("Tipo letra: " + ((int)c1 + 1));
               
               if (i<19) System.out.print(c2);
               else System.out.println("");
               
               //System.out.print(i*2 + "--> dec: " + aux);
               //System.out.print("--> HEX: " + s);
               
               //System.out.println(";--> BIN: " + Util.verBinario(aux));
               
           	//for (int j = 0; j < 500; j++) {System.out.print(".");}
           	//System.out.println(".");               
            
            
               
           	//master.setValue(127, RegisterRange.HOLDING_REGISTER, i+578, DataType.TWO_BYTE_INT_SIGNED, aux20[i].shortValue());
           	
           	//for (int j = 0; j < 500; j++) {System.out.print(".");}
           	//System.out.println(".");               
           	
            }
            
            for (int j = 0; j < 0; j++) {
            aux = (Short) master.getValue(127, 
         		   RegisterRange.HOLDING_REGISTER, 
            		1044+j,
                    DataType.TWO_BYTE_INT_SIGNED);
            System.out.println(Util.verHexadecimal(aux));
            }
            
            
            
            
            
        }
         /*
            Short sh = 1234;
            master.setValue(127, RegisterRange.HOLDING_REGISTER, 1003, DataType.TWO_BYTE_INT_UNSIGNED, sh.shortValue());
            System.out.println("write: " + master.getValue(127, 
            		RegisterRange.HOLDING_REGISTER, 
            					1003,
                    			DataType.TWO_BYTE_INT_UNSIGNED));
                    			
           */ 

        //master.setValue(127, RegisterRange.HOLDING_REGISTER, 4000, DataType.TWO_BYTE_INT_SIGNED, permisoEscritura);
        
        }
        finally {
            master.destroy();
        }
        System.out.println(" ");
        System.out.println("Took: " + (System.currentTimeMillis() - start) + "ms");

        
    }

    // public static void main(String[] args) throws Exception {
    // ModbusFactory factory = new ModbusFactory();
    // IpParameters params = new IpParameters();
    // params.setHost("localhost");
    // params.setPort(12345);
    // ModbusMaster master = factory.createTcpMaster(params, true, false);
    // // master.setRetries(4);
    // master.setRetries(0);
    // try {
    // master.init();
    // master.getValue(1, RegisterRange.HOLDING_REGISTER, 0, DataType.TWO_BYTE_INT_UNSIGNED);
    // }
    // finally {
    // master.destroy();
    // }
    // }
}