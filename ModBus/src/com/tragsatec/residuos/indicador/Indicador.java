package com.tragsatec.residuos.indicador;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.ip.IpParameters;
import com.tragsatec.residuos.Util;

public abstract class Indicador {
	    
  private boolean debug = false;
  
  private String ip;
  private String puerto;
  private Properties prop;
  private ModbusMaster master;	

  
  public Indicador() throws IOException {
				
		prop = new Properties();
		InputStream input = null;	 
					
    	input = this.getClass().getClassLoader().getResourceAsStream("indicador.properties"); //new FileInputStream("indicador.properties");
	    prop.load(input);	        			

	  }
  
  
  protected boolean escribirMemoriaWord(int direccion, short valor) {

	  boolean salida = false;
	  
      try {
    	  
          master.setValue(127, RegisterRange.HOLDING_REGISTER, direccion, DataType.TWO_BYTE_INT_UNSIGNED, valor);
          salida = true;
      } catch (Exception e) { 
    	e.printStackTrace();  
      }

      return salida;
	  
  }
  
  public void conectar() throws com.serotonin.modbus4j.exception.ModbusInitException {
      ModbusFactory factory = new ModbusFactory();
      IpParameters params = new IpParameters();
      params.setHost(getIp());
      params.setPort(Integer.parseInt(getPuerto()));
      params.setEncapsulated(false);
      master = factory.createTcpMaster(params, true);
      // master.setRetries(4);
      master.setTimeout(1000);
      master.setRetries(0);	
      
      if (isDebug())System.out.println("conectar(). Conectando a  " + getIp() + " por puerto " + getPuerto() );
      master.init();
    
  }

  public void desconectar() {
      
      try {
    	  if (isDebug())System.out.println("desconectar(). Desconectando de  " + getIp() + " por puerto " + getPuerto() );  
        master.destroy();
      } catch (Exception e) { 
    	e.printStackTrace();  
      }        
  }
  
  public Integer leerMemoriaWord(int direccion) {

	  Integer salida = 0;
	  
       try {
         //ver OJOJOOJOJOJOJ

    	   
    	   salida = Integer.parseInt(master.getValue(127, 
 		            RegisterRange.INPUT_REGISTER, 
 					direccion,
         			DataType.TWO_BYTE_INT_UNSIGNED).toString());
    	   
      	   // Evito casos de mesada en reposo negativos (máx -1000 kilogramos)
    	   if (salida.intValue() > 64536) salida = 0; 
    	   
      } catch (Exception e) { 
    	e.printStackTrace();  
      }

      return salida;
	  
  }
  
  protected Integer leerHoldingRegister(int direccion) {

	  Integer salida = 0;
	  
       try {
    	   
    	   salida = Integer.parseInt(master.getValue(127, 
    			    RegisterRange.HOLDING_REGISTER, 
 					direccion,
         			DataType.TWO_BYTE_INT_UNSIGNED).toString());
    	   
    	   
    	   
      } catch (Exception e) { 
    	  System.out.println(e.getMessage());
    	e.printStackTrace();  
      }

      return salida;
	  
  }
  
    /**
     * Indica si está en modo debug
     * @return
     */
	public boolean isDebug() {
		return debug;
	}
	
	/**
	 * Sirve para  indicar modo debug
	 * @param debug
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
  

	public String getIp() {
		return ip;
	}




	public void setIp(String ip) {
		this.ip = ip;
	}


	public String getPuerto() {
		return puerto;
	}


	public void setPuerto(String puerto) {
		this.puerto = puerto;
	} 
	
	
	public Properties getProp() {
		return prop;
	}


	public void setProp(Properties prop) {
		this.prop = prop;
	}
	
	/**
	 * Calcula peso bruto asociado a camion que se desea pesar (Kg)
	 * Es bloqueante....
	 * 	 * @return
	 */
	public abstract Float generarMedidaEstableKgCeroMaxCero();


	/**
	 * Indica que la medición es estable
	 * @return
	 */
	public abstract boolean medicionEstable();
	    
	/**
	 * Devuelve datos pesada bruto en Kg
	 * @return
	 */
	public abstract Float brutoKg();
	  
	/**
	 * Devuelve datos pesada neto en Kg
	 * @return
	 */
	public abstract Float netoKg();
	  
	/**
	 * Devuelve datos tara en Kg
	 * @return
	 */
	public abstract Float taraKg();



	  
}
