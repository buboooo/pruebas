package com.tragsa.basculas;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Hashtable;


public class Datos {
	private static Connection c = null;
	private String tablaMatriculas = "";
	private String tablaImagenes = "";

	public Datos()
			throws SQLException, IOException, ClassNotFoundException {
	
		String jdbc = ControlPlanta.configuracion.get("jdbc");
		String user = ControlPlanta.configuracion.get("user");
		String password = ControlPlanta.configuracion.get("password");
		String classdrv = ControlPlanta.configuracion.get("classDriver");
		
		Class.forName(classdrv);
		c = DriverManager.getConnection(jdbc, user, password);

		tablaMatriculas = ControlPlanta.configuracion.get("tablaMatriculas");
		tablaImagenes = ControlPlanta.configuracion.get("tablaImagenes");
	}

	public void desconecta() {
		
		try {
			c.close();
		} catch (SQLException e) {
			System.out.println("Intento desconexión fallido");	
		}
	}

	public Hashtable<Integer, Hashtable<String, String>> cargaConfigCamaras(
			int idplanta, Hashtable<String, Bascula> hbasculas)
			throws IOException, Exception {
		
		Hashtable<Integer, Hashtable<String, String>> tipoCamara = new Hashtable<Integer, Hashtable<String, String>>();
	
		String sql = "Select * from camaras cam,carriles c where cam.idcarril = c.id and c.idplanta = "
				+ idplanta + " order by idcarril";
		PreparedStatement s = c.prepareStatement(sql);
		ResultSet camaras = s.executeQuery();

		while (camaras.next()) {
			
			Hashtable<String, String> atributosCamara = new Hashtable<String, String>();
			
			String tipo = camaras.getString("tipo");
			if (tipo.equals("entrada") || tipo.equals("salida") )
					atributosCamara.put("tipo", camaras.getString("tipo"));
			else
			{
				System.out.println("El campo tipo de la tabla 'camaras' debe tener uno de los valores siguientes: "
						+ "entrada o salida");
				this.desconecta();
				System.exit(-1);
			}

			int idCarril = camaras.getInt("idcarril");
			atributosCamara.put("idcarril", String.valueOf(idCarril) );

			if (camaras.getString("barrera") != null)
				atributosCamara.put("barrera", camaras.getString("barrera"));
			else
				atributosCamara.put("barrera", "");

			if (camaras.getString("bascula") != null)
				atributosCamara.put("bascula", camaras.getString("bascula"));
			else
				atributosCamara.put("bascula", "");

			String nombreDispositivo = atributosCamara.get("bascula");
			if (nombreDispositivo != "") {
				// si el dispositivo báscula no está creado se crea y se mete en
				// la lista de básculas que existen en una planta
				if (((Bascula) hbasculas.get(nombreDispositivo)) == null) {
					Bascula bascula = new Bascula(nombreDispositivo);
					hbasculas.put(nombreDispositivo, bascula);
				}
			}

			//camara contraria
			//Select * from camaras cam,carriles c where cam.idcarril = c.id and  c.idplanta = 1 and cam.id != 2 and idcarril = 1 order by idcarril
			sql = "Select cam.id from camaras cam,carriles c where cam.idcarril = c.id and  "
					+ "c.idplanta = " + idplanta + " and cam.id != " + camaras.getInt("id") + " and idcarril = "+ idCarril +" order by idcarril";
			s = c.prepareStatement(sql);
			ResultSet rs = s.executeQuery();
			while (rs.next())
				atributosCamara.put("camaraContraria", String.valueOf(rs.getInt(1))  );
			
			rs.close();
			
			tipoCamara.put(camaras.getInt("id"), atributosCamara);
			
			}//fin while camaras.next()
		
		
		return tipoCamara;
	}
	/**
	 * el objetivo final de esta función es contruir una consulta similar a esta
	 * 		select * from alpr where camera_code = '2' or camera_code = '1' limit 1
	 * @param idCarril
	 * @return
	 * @throws SQLException
	 * @author jmunoz21.ext
	 */
	public ResultSet ConsultaPorCarril(int idCarril) throws SQLException {
		//el objetivo final de esta función es contruir una consulta similar a esta
		//select * from alpr where camera_code = '2' or camera_code = '1' limit 1
		
		String strSubQuery = creaSubquery(idCarril);

		//strSubQuery = "Select * from "+ tablaMatriculas + " " +  strSubQuery + " limit 1";
		strSubQuery = ControlPlanta.configuracion.get("consultaSql") + " and " +  strSubQuery + " limit 1";
		PreparedStatement s = c.prepareStatement(strSubQuery);
		ResultSet registrosEsteCarril = s.executeQuery();
		
		return registrosEsteCarril;
	}
	
	/**
	 * Crea una condición where para consulta de matriculas pendientes de tratar
	 * @param idCarril
	 * @return
	 * @throws SQLException
	 */
	private String creaSubquery(int idCarril) throws SQLException
	{
		PreparedStatement s = c.prepareStatement("select * from camaras "
				+ "where idcarril = " + idCarril);
		//select * from camaras where idcarril = 1
		ResultSet camarasPorCarril = s.executeQuery();
		int idCamara = 0;
			
		String strSubQuery = " (";
		//creamo subquery de cámaras de ese carril
		while (camarasPorCarril.next())	{
			idCamara = camarasPorCarril.getInt(1);
			if (strSubQuery != " (")
				strSubQuery = strSubQuery + " or " + "camera_code = '" + idCamara + "'";
			else
				strSubQuery = strSubQuery + "camera_code = '" + idCamara +"'";
		}
		strSubQuery = strSubQuery + ") ";
		
		return strSubQuery;
	}
	
	public ResultSet carriles()	throws SQLException {
		PreparedStatement s = c.prepareStatement("Select * from carriles where idplanta = " + ControlPlanta.configuracion.get("cod_planta"));
		ResultSet carriles = s.executeQuery();

		return carriles;
	}
	
	

	/**
	 * Elimina el registro creado de la captura de cámaras.
	 * 
	 * @author Juan Carlos Muñoz
	 * @param idTabla
	 *            id del registro a eliminar
	 * @version: 14-3-2014
	 */
	public void eliminaEntradasCarril(Integer idCarril) throws SQLException {
		String strSubquery = creaSubquery(idCarril);
		
		PreparedStatement s = c.prepareStatement("delete from "
				+ tablaMatriculas + " where " + strSubquery);
		s.executeUpdate();

		// borra en cascada a través de base de datos
		// s = c.prepareStatement("delete from alpr_image where alpr_id = " +
		// idTabla );
		// s.executeUpdate();

	}

	/**
	 * Comprueba si la matricula del vehículo está dada de alta en la tabla
	 * "vehiculo"
	 * 
	 * @author Juan Carlos Muñoz
	 * @param matricula
	 *            del vehiculo
	 * @version: 14-3-2014
	 */
	public Integer CompruebaVehiculo(String matricula) throws SQLException {
		int idVehiculo = 0;
		String sqlVehiculo = "Select * from vehiculo where matricula = ";

		PreparedStatement s = c.prepareStatement(sqlVehiculo + "'" + matricula
				+ "'");
		ResultSet r = s.executeQuery();
		while (r.next()) {
			idVehiculo = r.getInt("id");
			break;
		}

		return idVehiculo;
	}
	
	
	/**
	 * Mira el tipo de vehiculo que es, "articulado" o null que sería normal
	 * 
	 * @author Juan Carlos Muñoz
	 * @param matricula
	 *            del vehiculo
	 * @version: 14-3-2014
	 */
	public String TipoVehiculo(String matricula) throws SQLException {
		String tipo = "";
		String sqlVehiculo = "Select tipo from vehiculo where matricula = ";

		PreparedStatement s = c.prepareStatement(sqlVehiculo + "'" + matricula
				+ "'");
		ResultSet r = s.executeQuery();
		while (r.next()) {
			tipo = r.getString("tipo");
			break;
		}

		return tipo;
	}

	/**
	 * Crea un registro en la tabla "entradas" por cada captura de camara
	 * localizada en las entradas de planta
	 * 
	 * @author Juan Carlos Muñoz
	 * @param idVehiculo
	 *            id del vehículo que ya está previamente dado de alta
	 * @param fecha_entrada
	 *            hora de entrada del vehículo
	 * @param cod_planta
	 *            código de planta donde se está registrando la actividad
	 * @version: 14-3-2014
	 */
	public Integer creaEntrada(int idVehiculo, Date fecha_entrada,
			int cod_planta) throws SQLException {
		int id = 0;

		String sqlstr;
		if (idVehiculo == 0)
			sqlstr = "insert into entradas (fecha_entrada,cod_planta) values ('"
					+ fecha_entrada + "'," + cod_planta + ") returning id";
		else
			sqlstr = "insert into entradas (vehiculo,fecha_entrada,cod_planta) values ("
					+ idVehiculo
					+ ",'"
					+ fecha_entrada
					+ "',"
					+ cod_planta
					+ ") returning id";
		
		PreparedStatement s = c.prepareStatement(sqlstr);
		ResultSet r = s.executeQuery();
		while (r.next()) {
			id = r.getInt("id");
			break;
		}

		return id;
	}
	
	public void borraEntrada(int id_entrada) throws SQLException {
		String sqlstr = "Delete from entradas where id = " + id_entrada;
		PreparedStatement s = c.prepareStatement(sqlstr);
		s.executeUpdate();
	}

	/**
	 * Crea un registro en la tabla ticket para registrar el peso en entrada o
	 * bien de salida
	 * 
	 * @author Juan Carlos Muñoz
	 * @param idVehiculo
	 *            id del vehículo que ya está previamente dado de alta
	 * @param fecha_entrada
	 *            hora de entrada del vehículo
	 * @param cod_planta
	 *            código de planta donde se está registrando la actividad
	 * @version: 14-3-2014
	 */
	public void creaTicket(int id_entrada, float peso, String tipoCamara)
			throws SQLException {
		boolean existeIdEntrada = false;

		String sqlstr = "";

		// Comprueba si existe ya una entrada en ticket previamente creada
		// si es así, hará un update de la tabla ticket.
		PreparedStatement s1 = c
				.prepareStatement("select id_entrada from  ticket where "
						+ " id_entrada = '" + id_entrada + "'");
		ResultSet r1 = s1.executeQuery();
		while (r1.next()) {
			existeIdEntrada = true;
			break;
		}

		switch (tipoCamara) {
		case "entrada":
				sqlstr = "insert into ticket (id_entrada,peso_entrada) values ("
						+ id_entrada + "," + peso + ")";

			break;
		case "salida":
			if (existeIdEntrada)
				sqlstr = "update ticket set peso_salida = " + peso
						+ " where id_entrada= " + id_entrada;
			else
				sqlstr = "insert into ticket (id_entrada,peso_salida) values ("
						+ id_entrada + "," + peso + ")";
			break;
		case "interiorEntradaVertedero":
			if (existeIdEntrada)
				sqlstr = "update ticket set peso_ent_vert = " + peso
						+ " where id_entrada= " + id_entrada;
			else
				sqlstr = "insert into ticket (id_entrada,peso_ent_vert) values ("
						+ id_entrada + "," + peso + ")";
			break;

		case "interiorSalidaVertedero":
			if (existeIdEntrada)
				sqlstr = "update ticket set peso_salida_vert = " + peso
						+ " where id_entrada= " + id_entrada;
			else
				sqlstr = "insert into ticket (id_entrada,peso_salida_vert) values ("
						+ id_entrada + "," + peso + ")";
			break;

		}

		PreparedStatement s = c.prepareStatement(sqlstr);
		s.executeUpdate();
		ControlPlanta.log.crearLog("            Ejecutado: "+ sqlstr, "", "", -1);

	}

	/**
	 * Crea un registro en tabla de "Incidencias" cuando entra un vehículo cuya
	 * matrícula no está registrada en la tabla "vehiculo" o por otros casos, un
	 * error en la báscula (no conecta).
	 * 
	 * @author Juan Carlos Muñoz
	 * @param id_entrada
	 *            id de la tabla "entradas"
	 * @param tipo
	 *            es un integer según un tipo de incidencia
	 * @param descripcion
	 *            de la incidencia
	 * @version: 14-3-2014
	 * @throws SQLException 
	 */
	public void creaIncidencia(Integer id_entrada, Integer tipoIncidencia,
			String descripcion, String matricula) throws SQLException {
		try {
			PreparedStatement s = c
					.prepareStatement("insert into incidencias (id_entrada,tipo,descripcion) "
					+ "values ("+ id_entrada + "," + tipoIncidencia + ",'" + descripcion + "')");
			s.executeUpdate();

			int idVehiculo = 0;
			
			/* matricula no resgistrada. La creamos en
			 vehiculos para posteriormente editar
			 el registro mediante formulario web */
			if (tipoIncidencia == 0) 	
			{
				s = c.prepareStatement("insert into vehiculo (matricula) values ('"
						+ matricula + "') returning id");
				ResultSet vehiculo = s.executeQuery();

				while (vehiculo.next()) {
					idVehiculo = vehiculo.getInt(1);
					break;
				}
			}

			if (idVehiculo != 0) {
				s = c.prepareStatement("update entradas set vehiculo = "
						+ idVehiculo + " where id = " + id_entrada);
				s.executeUpdate();
			}

		} catch (SQLException ex) {
			ControlPlanta.log.crearLog("Funcion creaIncidencia(). Error al ejecutar update: " 
							+ ex.getMessage(), "", "", -1);
			throw ex;
		}

	}

	/**
	 * Crea un registro en la tabla de "movimientos" por cada registro en la
	 * tabla de capturas de camara
	 * 
	 * @author Juan Carlos Muñoz
	 * @param camara
	 *            es el id de la camara que detecta el vehiculo
	 * @param fecha_entrada
	 *            es la fecha en la que se detecta el vehículo
	 * @param id_entrada
	 *            es el id de la tabla "entradas" que se ha creado cuando entró
	 *            el vehículo
	 * @version: 14-3-2014
	 */
	public int creaMovimiento(int camara, Date fecha_entrada, String matricula,
			int id_entrada, int alpr_id) throws SQLException {
		int idVehiculo = 0;

		try {
			
			//si id_entrada == 0 es que será un vehiculo de salida, cogemos su id_entrada
			if (id_entrada == 0) {
				PreparedStatement s1 = c
						.prepareStatement("select id from  vehiculo where "
								+ " matricula = '" + matricula + "'");
				ResultSet r1 = s1.executeQuery("select id from  vehiculo where "
						+ " matricula = '" + matricula + "'");
				while (r1.next()) {
					idVehiculo = r1.getInt("id");
					break;
				}

				if (idVehiculo != 0) {
					PreparedStatement s = c
							.prepareStatement("select e.id from entradas e, vehiculo v where "
									+ " e.vehiculo = v.id and e.fecha_salida is null and e.vehiculo ="
									+ idVehiculo);
					ResultSet r = s.executeQuery();
					while (r.next()) {
						id_entrada = r.getInt("id");
						break;
						}
					}
				} //fin if(id_entrada==0)

			/*mira si ya hay un movimiento creado, para recuperarse de un estado anterior
			de error, por ejemplo tras un error de pesada/bascula */
			int id_entrada2=0;
			PreparedStatement s =
					c.prepareStatement("select id_entrada from movimientos where camara = "+ camara + " and "
							+ " fecha = '" + fecha_entrada + "'");
			ResultSet rs = s.executeQuery();
			while (rs.next())
				id_entrada2 = rs.getInt(1);
			if (id_entrada2 != 0)
				return id_entrada2;

			if (id_entrada2==0 && id_entrada!=0)  //si no habia un movimiento ya creado
			{
				s = c
						.prepareStatement("insert into movimientos (camara,fecha,id_entrada,matricula,image)"
								+ " values ("
								+ camara
								+ ",'"
								+ fecha_entrada
								+ "',"
								+ id_entrada
								+ ","
								+ "'"
								+ matricula
								+ ""
								+ "', (select image from "
								+ tablaImagenes
								+ " where alpr_id=" + alpr_id + ") )");

				// insert into movimientos (camara,fecha,id_entrada) values
				// (1,(select taken_on from alpr where alpr_id= 1),2933)
				s.executeUpdate();
			}
		}catch (Exception ex)
		{
			ControlPlanta.log.crearLog("********************************************************", "" , matricula, camara);
			ControlPlanta.log.crearLog("*******Error al registrar movimiento: " + ex.getMessage(), "" , matricula, camara);
		}
		return id_entrada;
	}

	public void actualizaSalida(int id_entrada, Date fecha_salida)
			throws SQLException {
		PreparedStatement s = c
				.prepareStatement("update entradas set fecha_salida = '"
						+ fecha_salida + "'  where id = " + id_entrada);
		s.executeUpdate();

	}

}
