package koolfileindexer.logica;

import koolfileindexer.modelo.Archivo;
import koolfileindexer.modelo.Categoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la clase Indexador sin usar mocks.
 * Se enfoca en probar métodos que no requieren conexión a BD.
 */
class IndexadorTest {

    @TempDir
    Path tempDir;

    private Indexador indexador;

    @BeforeEach
    void setUp() throws Exception {
        // En lugar de intentar cambiar el campo final, modificamos el contenido de
        // AtomicReference
        Field instance = Indexador.class.getDeclaredField("INSTANCIA");
        instance.setAccessible(true);
        AtomicReference<?> ref = (AtomicReference<?>) instance.get(null);
        Method compareAndSetMethod = AtomicReference.class.getMethod("compareAndSet", Object.class, Object.class);
        // Obtener valor actual
        Object currentValue = ref.get();
        // Establecer a null solo si tiene un valor actual
        if (currentValue != null) {
            compareAndSetMethod.invoke(ref, currentValue, null);
        }

        // Crear un nuevo indexador sin archivo de exclusiones
        indexador = Indexador.getInstance(null);
    }

    @Test
    void getInstance_esSingleton() {
        Indexador i1 = Indexador.getInstance(null);
        Indexador i2 = Indexador.getInstance("cualquier.txt");
        assertSame(i1, i2, "getInstance debe devolver siempre la misma instancia");
    }

    @Test
    void excluirArchivo_filtraCorrectamente() throws Exception {
        // Acceder al método privado excluirArchivo via reflexión
        Method m = Indexador.class.getDeclaredMethod("excluirArchivo", Path.class);
        m.setAccessible(true);

        // Archivos que deben excluirse
        assertTrue((Boolean) m.invoke(indexador, Paths.get(".oculto")), "debe excluir nombres que arrancan con punto");
        assertTrue((Boolean) m.invoke(indexador, Paths.get("C:\\foo\\bar.EXE")), "debe excluir extensión exe");
        assertTrue((Boolean) m.invoke(indexador, Paths.get("C:\\Windows\\system32\\file.txt")),
                "debe excluir rutas que contienen '/windows/'");
        assertTrue((Boolean) m.invoke(indexador, Paths.get("C:\\Program Files\\app.txt")),
                "debe excluir rutas que contienen '/program files/'");
        assertTrue((Boolean) m.invoke(indexador, Paths.get("C:\\Archivos de programa\\app.txt")),
                "debe excluir rutas que contienen '/archivos de programa/'");
        assertTrue((Boolean) m.invoke(indexador, Paths.get("C:\\temp\\thumbs.db")),
                "debe excluir archivos thumbs.db");

        // Archivos que NO deben excluirse
        assertFalse((Boolean) m.invoke(indexador, Paths.get("C:\\temp\\archivo.txt")),
                "no debe excluir archivos .txt normales");
        assertFalse((Boolean) m.invoke(indexador, Paths.get("C:\\Documentos\\Proyecto.docx")),
                "no debe excluir archivos .docx");
        assertFalse((Boolean) m.invoke(indexador, Paths.get("C:\\Fotos\\imagen.jpg")),
                "no debe excluir archivos .jpg");
    }

    @Test
    void crearArchivoDesdePath_generaArchivoConPropiedadesCorrectas() throws Exception {
        // Crear archivo temporal para la prueba
        Path tempFile = Files.createFile(tempDir.resolve("test.txt"));
        Files.writeString(tempFile, "Contenido de prueba");

        // Obtener atributos del archivo
        BasicFileAttributes attrs = Files.readAttributes(tempFile, BasicFileAttributes.class);

        // Acceder al método privado crearArchivoDesdePath
        Method m = Indexador.class.getDeclaredMethod("crearArchivoDesdePath", Path.class, BasicFileAttributes.class);
        m.setAccessible(true);

        // Invocar método y verificar resultado
        Archivo archivo = (Archivo) m.invoke(indexador, tempFile, attrs);

        // Verificar propiedades del archivo creado
        assertEquals("test.txt", archivo.getNombre(), "El nombre debe coincidir");
        assertEquals("txt", archivo.getExtension(), "La extensión debe coincidir");
        assertEquals(tempFile.toAbsolutePath().toString(), archivo.getRutaCompleta(), "La ruta debe coincidir");
        assertEquals(attrs.size(), archivo.getTamanoBytes(), "El tamaño debe coincidir");
        assertEquals(Categoria.DOCUMENTO, archivo.getCategoria(), "Categoría para .txt debe ser DOCUMENTO");
    }

    @Test
    void cargarExclusiones_cargaPatronesDesdeArchivo() throws Exception {
        // Crear un archivo temporal de exclusiones
        Path exclusionesFile = Files.createFile(tempDir.resolve("exclusiones_test.txt"));
        List<String> exclusiones = Arrays.asList(
                "# Comentario que debe ignorarse",
                "patron1",
                "patron2",
                "", // Línea en blanco que debe ignorarse
                "patron3");
        Files.write(exclusionesFile, exclusiones);

        // Crear un nuevo indexador con este archivo de exclusiones
        Field instance = Indexador.class.getDeclaredField("INSTANCIA");
        instance.setAccessible(true);
        AtomicReference<?> ref = (AtomicReference<?>) instance.get(null);
        Method compareAndSetMethod = AtomicReference.class.getMethod("compareAndSet", Object.class, Object.class);
        // Obtener valor actual
        Object currentValue = ref.get();
        // Establecer a null solo si tiene un valor actual
        if (currentValue != null) {
            compareAndSetMethod.invoke(ref, currentValue, null);
        }

        Indexador idx = Indexador.getInstance(exclusionesFile.toString());

        // Acceder al campo de rutas excluidas (rutasExcluidas en lugar de
        // patronesExcluidos)
        Field rutasExcluidasField = Indexador.class.getDeclaredField("rutasExcluidas");
        rutasExcluidasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Path> rutasExcluidas = (Set<Path>) rutasExcluidasField.get(idx);

        // Verificar que las rutas se cargaron correctamente
        assertEquals(3, rutasExcluidas.size(), "Debe cargar 3 patrones excluyendo comentarios y líneas vacías");

        // Convertir los Path a String para facilitar la verificación
        List<String> rutasComoStrings = rutasExcluidas.stream()
                .map(Path::toString)
                .map(String::toLowerCase)
                .toList();

        // Verificar que contiene los patrones (como parte de las rutas)
        boolean contienePatron1 = rutasComoStrings.stream().anyMatch(s -> s.endsWith("patron1"));
        boolean contienePatron2 = rutasComoStrings.stream().anyMatch(s -> s.endsWith("patron2"));
        boolean contienePatron3 = rutasComoStrings.stream().anyMatch(s -> s.endsWith("patron3"));

        assertTrue(contienePatron1, "Debe contener el primer patrón");
        assertTrue(contienePatron2, "Debe contener el segundo patrón");
        assertTrue(contienePatron3, "Debe contener el tercer patrón");
    }

    @Test
    void detectaArchivosConExtensionesImagen() throws Exception {
        // Crear archivos temporales para la prueba
        Path jpgFile = Files.createFile(tempDir.resolve("imagen.jpg"));
        Path pngFile = Files.createFile(tempDir.resolve("icono.png"));
        Path gifFile = Files.createFile(tempDir.resolve("animacion.gif"));

        // Obtener atributos
        BasicFileAttributes attrsJpg = Files.readAttributes(jpgFile, BasicFileAttributes.class);
        BasicFileAttributes attrsPng = Files.readAttributes(pngFile, BasicFileAttributes.class);
        BasicFileAttributes attrsGif = Files.readAttributes(gifFile, BasicFileAttributes.class);

        // Acceder al método privado
        Method m = Indexador.class.getDeclaredMethod("crearArchivoDesdePath", Path.class, BasicFileAttributes.class);
        m.setAccessible(true);

        // Verificar categoría
        Archivo archivoJpg = (Archivo) m.invoke(indexador, jpgFile, attrsJpg);
        Archivo archivoPng = (Archivo) m.invoke(indexador, pngFile, attrsPng);
        Archivo archivoGif = (Archivo) m.invoke(indexador, gifFile, attrsGif);

        assertEquals(Categoria.IMAGEN, archivoJpg.getCategoria(), "JPG debe ser categoría IMAGEN");
        assertEquals(Categoria.IMAGEN, archivoPng.getCategoria(), "PNG debe ser categoría IMAGEN");
        assertEquals(Categoria.IMAGEN, archivoGif.getCategoria(), "GIF debe ser categoría IMAGEN");
    }

    @Test
    void detectaArchivosConExtensionesVideo() throws Exception {
        // Crear archivos temporales para la prueba
        Path mp4File = Files.createFile(tempDir.resolve("pelicula.mp4"));
        Path aviFile = Files.createFile(tempDir.resolve("grabacion.avi"));

        // Obtener atributos
        BasicFileAttributes attrsMp4 = Files.readAttributes(mp4File, BasicFileAttributes.class);
        BasicFileAttributes attrsAvi = Files.readAttributes(aviFile, BasicFileAttributes.class);

        // Acceder al método privado
        Method m = Indexador.class.getDeclaredMethod("crearArchivoDesdePath", Path.class, BasicFileAttributes.class);
        m.setAccessible(true);

        // Verificar categoría
        Archivo archivoMp4 = (Archivo) m.invoke(indexador, mp4File, attrsMp4);
        Archivo archivoAvi = (Archivo) m.invoke(indexador, aviFile, attrsAvi);

        assertEquals(Categoria.VIDEO, archivoMp4.getCategoria(), "MP4 debe ser categoría VIDEO");
        assertEquals(Categoria.VIDEO, archivoAvi.getCategoria(), "AVI debe ser categoría VIDEO");
    }

    @Test
    void detectaArchivosConExtensionesDocumento() throws Exception {
        // Crear archivos temporales para la prueba
        Path docFile = Files.createFile(tempDir.resolve("informe.doc"));
        Path pdfFile = Files.createFile(tempDir.resolve("contrato.pdf"));
        Path txtFile = Files.createFile(tempDir.resolve("notas.txt"));

        // Obtener atributos
        BasicFileAttributes attrsDoc = Files.readAttributes(docFile, BasicFileAttributes.class);
        BasicFileAttributes attrsPdf = Files.readAttributes(pdfFile, BasicFileAttributes.class);
        BasicFileAttributes attrsTxt = Files.readAttributes(txtFile, BasicFileAttributes.class);

        // Acceder al método privado
        Method m = Indexador.class.getDeclaredMethod("crearArchivoDesdePath", Path.class, BasicFileAttributes.class);
        m.setAccessible(true);

        // Verificar categoría
        Archivo archivoDoc = (Archivo) m.invoke(indexador, docFile, attrsDoc);
        Archivo archivoPdf = (Archivo) m.invoke(indexador, pdfFile, attrsPdf);
        Archivo archivoTxt = (Archivo) m.invoke(indexador, txtFile, attrsTxt);

        assertEquals(Categoria.DOCUMENTO, archivoDoc.getCategoria(), "DOC debe ser categoría DOCUMENTO");
        assertEquals(Categoria.DOCUMENTO, archivoPdf.getCategoria(), "PDF debe ser categoría DOCUMENTO");
        assertEquals(Categoria.DOCUMENTO, archivoTxt.getCategoria(), "TXT debe ser categoría DOCUMENTO");
    }

    @Test
    void detectaArchivosConExtensionesMusica() throws Exception {
        // Crear archivos temporales para la prueba
        Path mp3File = Files.createFile(tempDir.resolve("cancion.mp3"));
        Path wavFile = Files.createFile(tempDir.resolve("grabacion.wav"));

        // Obtener atributos
        BasicFileAttributes attrsMp3 = Files.readAttributes(mp3File, BasicFileAttributes.class);
        BasicFileAttributes attrsWav = Files.readAttributes(wavFile, BasicFileAttributes.class);

        // Acceder al método privado
        Method m = Indexador.class.getDeclaredMethod("crearArchivoDesdePath", Path.class, BasicFileAttributes.class);
        m.setAccessible(true);

        // Verificar categoría
        Archivo archivoMp3 = (Archivo) m.invoke(indexador, mp3File, attrsMp3);
        Archivo archivoWav = (Archivo) m.invoke(indexador, wavFile, attrsWav);

        assertEquals(Categoria.MUSICA, archivoMp3.getCategoria(), "MP3 debe ser categoría MUSICA");
        assertEquals(Categoria.MUSICA, archivoWav.getCategoria(), "WAV debe ser categoría MUSICA");
    }
}
