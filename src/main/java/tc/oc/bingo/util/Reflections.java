package tc.oc.bingo.util;

import lombok.SneakyThrows;
import tc.oc.bingo.Bingo;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class Reflections {

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T> List<Class<T>> findClasses(
            String basePackage, Class<T> type, Class<? extends Annotation> matcher) {
        String basePath = basePackage.replace(".", File.separator);
        try (JarFile jar = new JarFile(getJarFile())) {
            return StreamUtils.toStream(jar.entries())
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith(basePath) && name.endsWith(".class"))
                    .map(path -> path.replace('/', '.').replace(".class", ""))
                    .map(Class::forName)
                    .filter(cls -> type.isAssignableFrom(cls) && cls.isAnnotationPresent(matcher))
                    .map(cls -> (Class<T>) cls)
                    .collect(Collectors.toList());
        }
    }

    public static File getJarFile() throws IllegalStateException {
        String uri = Bingo.class.getResource("Bingo.class").toString();
        if (uri.startsWith("file:"))
            throw new IllegalStateException("This class has been loaded from a directory and not from a jar file.");
        if (!uri.startsWith("jar:file:")) {
            int idx = uri.indexOf(':');
            String protocol = idx == -1 ? "(unknown)" : uri.substring(0, idx);
            throw new IllegalStateException("This class has been loaded remotely via the " + protocol +
                    " protocol. Only loading from a jar on the local file system is supported.");
        }

        int idx = uri.indexOf('!');
        //As far as I know, the if statement below can't ever trigger, so it's more of a sanity check thing.
        if (idx == -1)
            throw new IllegalStateException("You appear to have loaded this class from a local jar file, but I can't make sense of the URL!");

        try {
            String fileName = URLDecoder.decode(uri.substring("jar:file:".length(), idx), Charset.defaultCharset().name());
            return new File(fileName);
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("default charset doesn't exist. Your VM is borked.");
        }
    }

}
