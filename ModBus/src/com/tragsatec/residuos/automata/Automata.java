package com.tragsatec.residuos.automata;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.ip.IpParameters;

public abstract class Automata {
	
	private String ip;
	private String puerto;
	private Properties prop;
    private ModbusMaster master;
	
	
    public Automata() throws IOException {
		
		setProp(new Properties());
		InputStream input = null;
	 
		input = this.getClass().getClassLoader().getResourceAsStream("automata.properties"); //new FileInputStream("automata.properties");
        getProp().load(input);
							 
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
      master.init();
    
  }

  public void desconectar() {
      
      try {
        master.destroy();
      } catch (Exception e) { 
    	e.printStackTrace();  
      }        
  }
  
  protected Short leerMemoriaWord(int direccion) {

	  Short salida = null;
	  
       try {
       
          salida = Short.valueOf(master.getValue(127, 
          		            RegisterRange.HOLDING_REGISTER, 
          					direccion,
                  			DataType.TWO_BYTE_INT_UNSIGNED).toString());
          
      } catch (Exception e) { 
    	e.printStackTrace();  
      }

      return salida;
	  
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
  
}
