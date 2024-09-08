
package com.mycompany.generateinfofiles;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

public class GenerateInfoFiles {

  // Estructuras para almacenar los vendedores, productos, y las ventas por vendedor y las ventas por producto
    private static Map<String, Vendedor> vendedores = new HashMap<>();
    private static Map<Integer, Producto> productos = new HashMap<>();
    private static Map<String, HashMap<Integer, Integer>> ventasPorVendedor = new HashMap<>();
    private static Map<Integer, Integer> ventasPorProducto = new HashMap<>();
    private static NumberFormat formatoNumero = NumberFormat.getInstance(Locale.GERMAN); // Usar el formato de número con separador de miles

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Carga la información de vendedores y productos desde las bases de datos
        cargarVendedores();
        cargarProductos();

        String documentoVendedor;
        int productoId, cantidad;
        String respuesta = null;

          // Condicional para registrar las ventas
        while (true) {
            System.out.println("Ingrese el número de documento del vendedor (o 'salir' para terminar): ");
            documentoVendedor = scanner.nextLine();

            if (documentoVendedor.equalsIgnoreCase("salir")) {
                break;
            }

            // Confirma si el vendedor existe en la base de datos
            if (!vendedores.containsKey(documentoVendedor)) {
                System.out.println("Vendedor no encontrado. Por favor, verifique el número de documento.");
                continue;
            }

            HashMap<Integer, Integer> productosVendidos = ventasPorVendedor.getOrDefault(documentoVendedor, new HashMap<>());

            // Registra los productos vendidos
            do {
                System.out.println("Ingrese el ID del producto vendido: ");
                productoId = Integer.parseInt(scanner.nextLine());

                // Confirma si el producto existe en la base de datos
                if (!productos.containsKey(productoId)) {
                    System.out.println("Producto no encontrado. Por favor, verifique el ID del producto.");
                    continue;
                }

                System.out.println("Ingrese la cantidad vendida: ");
                cantidad = Integer.parseInt(scanner.nextLine());

                // Actualiza las ventas por vendedor
                productosVendidos.put(productoId, productosVendidos.getOrDefault(productoId, 0) + cantidad);

                // Actualiza las ventas por producto
                ventasPorProducto.put(productoId, ventasPorProducto.getOrDefault(productoId, 0) + cantidad);

                System.out.println("¿Desea agregar otro producto? (si/no): ");
                respuesta = scanner.nextLine();
            } while (respuesta.equalsIgnoreCase("si"));

            // Guardar las ventas del vendedor
            ventasPorVendedor.put(documentoVendedor, productosVendidos);
        }

        // Guardar el reporte en la base de datos
        guardarReporte();

        scanner.close();
    }

    // Clase para representar a un vendedor
    static class Vendedor {
        String tipoDocumento;
        String numeroDocumento;
        String nombre;
        String apellido;

        Vendedor(String tipoDocumento, String numeroDocumento, String nombre, String apellido) {
            this.tipoDocumento = tipoDocumento;
            this.numeroDocumento = numeroDocumento;
            this.nombre = nombre;
            this.apellido = apellido;
        }
    }

    // Clase para representar a un producto
    static class Producto {
        int id;
        String nombre;
        double valor;

        Producto(int id, String nombre, double valor) {
            this.id = id;
            this.nombre = nombre;
            this.valor = valor;
        }
    }

    // Método para llamar a los vendedores desde la base de datos
    public static void cargarVendedores() {
        try (BufferedReader reader = new BufferedReader(new FileReader("vendedores.txt"))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] datos = linea.split(",");
                if (datos.length == 4) {
                    String tipoDocumento = datos[0];
                    String numeroDocumento = datos[1];
                    String nombre = datos[2];
                    String apellido = datos[3];
                    vendedores.put(numeroDocumento, new Vendedor(tipoDocumento, numeroDocumento, nombre, apellido));
                } else {
                    System.out.println("Formato incorrecto en la línea: " + linea);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al cargar los vendedores: " + e.getMessage());
        }
    }

    // Método para llamar a los productos desde la base de datos
    public static void cargarProductos() {
        try (BufferedReader reader = new BufferedReader(new FileReader("productos.txt"))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] datos = linea.split(",");
                if (datos.length == 3) {
                    int idProducto = Integer.parseInt(datos[0]);
                    String nombreProducto = datos[1];
                    double valorProducto = Double.parseDouble(datos[2]);
                    productos.put(idProducto, new Producto(idProducto, nombreProducto, valorProducto));
                } else {
                    System.out.println("Formato incorrecto en la línea: " + linea);
                }
            }
        } catch (IOException e) {
            System.out.println("Error al cargar los productos: " + e.getMessage());
        }
    }

    // Método para guardar el reporte de ventas en la base de datos
    public static void guardarReporte() {
        try (FileWriter writer = new FileWriter("reporte_ventas.txt")) {
            writer.write("--- Reporte de Ventas por Vendedor ---\n");
            // Ordenar los vendedores por valor total vendido
            List<Map.Entry<String, HashMap<Integer, Integer>>> listaVendedores = new ArrayList<>(ventasPorVendedor.entrySet());
            listaVendedores.sort((e1, e2) -> {
                double valorTotal1 = calcularValorTotalVendedor(e1.getKey());
                double valorTotal2 = calcularValorTotalVendedor(e2.getKey());
                return Double.compare(valorTotal2, valorTotal1); // Ordenar de mayor a menor
            });

            for (Map.Entry<String, HashMap<Integer, Integer>> entrada : listaVendedores) {
                Vendedor vendedor = vendedores.get(entrada.getKey());
                if (vendedor != null) {
                    double valorTotalVendido = calcularValorTotalVendedor(entrada.getKey());
                    writer.write("Vendedor: " + vendedor.nombre + " " + vendedor.apellido + " (Tipo Documento: " + vendedor.tipoDocumento + ", Número Documento: " + vendedor.numeroDocumento + ")\n");
                    
                    HashMap<Integer, Integer> productosVendidos = entrada.getValue();
                    for (Map.Entry<Integer, Integer> productoVenta : productosVendidos.entrySet()) {
                        Producto producto = productos.get(productoVenta.getKey());
                        if (producto != null) {
                            double valorProductoTotal = producto.valor * productoVenta.getValue();
                            writer.write("  ID Producto: " + producto.id + " - Nombre: " + producto.nombre + " - Cantidad vendida: " + formatoNumero.format(productoVenta.getValue()) + " - Valor Total: $" + formatoNumero.format(valorProductoTotal) + "\n");
                        }
                    }
                    writer.write("  Valor Total Vendido: $" + formatoNumero.format(valorTotalVendido) + "\n");
                }
            }

            writer.write("\n--- Reporte de Ventas por Producto ---\n");
            
            // Metodo para ordenar los productos por el valor total vendido
            List<Map.Entry<Integer, Integer>> listaProductos = new ArrayList<>(ventasPorProducto.entrySet());
            listaProductos.sort((e1, e2) -> {
                double valorTotal1 = calcularValorTotalProducto(e1.getKey());
                double valorTotal2 = calcularValorTotalProducto(e2.getKey());
                return Double.compare(valorTotal2, valorTotal1); // Ordenar de mayor a menor
            });

            for (Map.Entry<Integer, Integer> entrada : listaProductos) {
                Producto producto = productos.get(entrada.getKey());
                if (producto != null) {
                    double valorTotal = calcularValorTotalProducto(entrada.getKey());
                    writer.write("ID Producto: " + producto.id + " - Nombre: " + producto.nombre + " - Cantidad total vendida: " + formatoNumero.format(entrada.getValue()) + " - Valor Total: $" + formatoNumero.format(valorTotal) + "\n");
                }
            }

            System.out.println("El reporte de ventas ha sido guardado en 'reporte_ventas.txt'.");
        } catch (IOException e) {
            System.out.println("Error al guardar el archivo: " + e.getMessage());
        }
    }

    // Método para calcular el valor total vendido por un vendedor
    public static double calcularValorTotalVendedor(String numeroDocumento) {
        HashMap<Integer, Integer> productosVendidos = ventasPorVendedor.get(numeroDocumento);
        double valorTotal = 0.0;
        if (productosVendidos != null) {
            for (Map.Entry<Integer, Integer> productoVenta : productosVendidos.entrySet()) {
                Producto producto = productos.get(productoVenta.getKey());
                if (producto != null) {
                    valorTotal += producto.valor * productoVenta.getValue();
                }
            }
        }
        return valorTotal;
    }

    // Método para calcular el valor total vendido de un producto
    public static double calcularValorTotalProducto(int idProducto) {
        Integer cantidadVendida = ventasPorProducto.get(idProducto);
        Producto producto = productos.get(idProducto);
        if (producto != null && cantidadVendida != null) {
            return producto.valor * cantidadVendida;
        }
        return 0.0;
    }
}
