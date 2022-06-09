package generatejavadoc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    private static final Pattern PATTERN = Pattern.compile("\\r?\\n");

    private static void fillFilesRecursively(
            Path directory, final List<File> resultFiles)
            throws IOException {
        Files.walkFileTree(
                directory,
                new java.nio.file.SimpleFileVisitor<Path>() {
                    @Override
                    public java.nio.file.FileVisitResult visitFile(
                            Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                        final String filePath = file.toString().toLowerCase();
                        if (filePath.endsWith("readme.md")) {
                            resultFiles.add(file.toFile());
                        }
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                });
    }

    public static void main(String[] args) throws IOException {
        List<File> resultFiles = new ArrayList<>();
        fillFilesRecursively(Paths.get("."), resultFiles);
        int ind = 0;
        for (File file : resultFiles) {
            if (ind % 20 == 0) {
                System.out.println(file.getAbsolutePath());
            }
            ind++;
            final byte[] readmeMd = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
            String readmeMdText = new String(readmeMd, UTF_8);
            try (DirectoryStream<Path> dirStream =
                    Files.newDirectoryStream(
                            Paths.get(file.getAbsolutePath().replace("readme.md", "")), "*.java")) {
                for (Path entry : dirStream) {
                    File javaFile = entry.toFile();
                    Path path = Paths.get(javaFile.getAbsolutePath());
                    final byte[] solutionJava = Files.readAllBytes(path);
                    String solutionJavaText = new String(solutionJava, UTF_8);
                    int[] index = {0};
                    String[] fromStr = {
                        "(\"{{",
                        "= \"{{",
                        "\n- [",
                        "    board = [[",
                        "    grid = [[",
                        " = [[",
                        "**,",
                        "**]",
                        "(**",
                        "**)"
                    };
                    String[] toStr = {
                        "(\"{ {",
                        "= \"{ {",
                        "\n- \\[",
                        "    board = [ [",
                        "    grid = [ [",
                        " = \\[\\[",
                        "** ,",
                        "** ]",
                        "( **",
                        "** )"
                    };
                    String readmeMdJavadoc =
                            "/**\n"
                                    + StringUtils.replaceEach(
                                                    PATTERN.splitAsStream(readmeMdText)
                                                            .map(
                                                                    line -> {
                                                                        String firstLine =
                                                                                line.replace(
                                                                                                "\\.",
                                                                                                " -")
                                                                                        + "\\.";
                                                                        String str =
                                                                                index[0]++ == 0
                                                                                        ? firstLine
                                                                                        : line;
                                                                        return line.isEmpty()
                                                                                ? " *"
                                                                                : " * " + str;
                                                                    })
                                                            .collect(Collectors.joining("\n")),
                                                    fromStr,
                                                    toStr)
                                            .replace("`**", "` **")
                                            .replace(",**", ", **")
                                            .replace("/*", "{@literal /}*")
                                            .replace("*/", "*{@literal /}")
                                    + "\n**/";
                    String publicClass =
                            solutionJavaText.contains("@SuppressWarnings")
                                    ? "@SuppressWarnings"
                                    : "public class ";
                    Files.write(path, getBytes(solutionJavaText, readmeMdJavadoc, publicClass));
                }
            }
        }
    }

    private static byte[] getBytes(
            String solutionJavaText, String readmeMdJavadoc, String publicClass) {
        return solutionJavaText
                .replace("\n" + publicClass, "\n" + readmeMdJavadoc + "\n" + publicClass)
                .getBytes(UTF_8);
    }
}
